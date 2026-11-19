package com.zlf.fufuzhibo.server.websocket;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 直播间 WebSocket 连接注册表：聊天与直播状态广播共用同一批连接
 *
 * <p>广播不再在调用线程里直接发送，而是写入每条连接各自的有界队列，
 * 由专用线程池异步发送：慢客户端只会撑满自己的队列并被断开，
 * 不会阻塞房间内其它观众，也不会卡住 Redis 订阅线程。
 */
@Slf4j
@Component
public class LiveRoomSessionRegistry {

    /**
     * 单条连接允许积压的最大消息数，超过视为慢客户端并断开
     */
    private static final int MAX_PENDING_MESSAGES = 256;

    /**
     * 异步发送线程数：慢客户端的阻塞只占用这批线程，不影响业务线程
     */
    private static final int SEND_THREADS = 8;

    private final JsonMapper jsonMapper;
    private final int maxConnections;
    private final long idleTimeoutMs;
    private final ExecutorService sendExecutor;
    private final AtomicBoolean capacityWarned = new AtomicBoolean(false);

    /**
     * roomId -> 该房间内的全部连接
     */
    private final Map<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    /**
     * sessionId -> 连接状态（所属房间、发送队列、最近活跃时间）
     */
    private final Map<String, SessionState> sessionStates = new ConcurrentHashMap<>();

    public LiveRoomSessionRegistry(
            JsonMapper jsonMapper,
            @Value("${fufu.chat.max-connections:5000}") int maxConnections,
            @Value("${fufu.chat.idle-timeout-ms:120000}") long idleTimeoutMs) {
        this.jsonMapper = jsonMapper;
        this.maxConnections = maxConnections;
        this.idleTimeoutMs = idleTimeoutMs;
        this.sendExecutor = Executors.newFixedThreadPool(SEND_THREADS, runnable -> {
            Thread thread = new Thread(runnable, "ws-sender");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 注册连接，超过单实例连接上限时返回 false，由调用方关闭连接
     */
    public boolean register(Long roomId, WebSocketSession session) {
        if (roomId == null || session == null) {
            return false;
        }
        if (sessionStates.size() >= maxConnections) {
            if (capacityWarned.compareAndSet(false, true)) {
                log.warn("WebSocket 连接数达到上限，拒绝新连接: max={}", maxConnections);
            }
            return false;
        }
        capacityWarned.set(false);
        roomSessions.computeIfAbsent(roomId, key -> ConcurrentHashMap.newKeySet()).add(session);
        sessionStates.put(session.getId(),
                new SessionState(roomId, session, System.currentTimeMillis()));
        return true;
    }

    public void unregister(Long roomId, WebSocketSession session) {
        if (session == null) {
            return;
        }
        SessionState state = sessionStates.remove(session.getId());
        if (state != null) {
            state.discard();
        }
        Long actualRoomId = state == null ? roomId : state.roomId;
        if (actualRoomId == null) {
            return;
        }
        Set<WebSocketSession> sessions = roomSessions.get(actualRoomId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            roomSessions.remove(actualRoomId, sessions);
        }
    }

    /**
     * 刷新连接活跃时间，收到客户端消息或 pong 帧时调用
     */
    public void touch(WebSocketSession session) {
        if (session == null) {
            return;
        }
        SessionState state = sessionStates.get(session.getId());
        if (state != null) {
            state.lastActive = System.currentTimeMillis();
        }
    }

    /**
     * 序列化一次后向房间内所有连接广播
     */
    public void broadcast(Long roomId, Map<String, Object> frame) {
        if (roomId == null) {
            return;
        }
        String payload;
        try {
            payload = jsonMapper.writeValueAsString(frame);
        } catch (Exception e) {
            log.warn("广播消息序列化失败: roomId={}, error={}", roomId, e.getMessage());
            return;
        }
        broadcastRaw(roomId, payload);
    }

    /**
     * 广播已序列化的消息，供 Redis 订阅端直接复用，避免二次序列化
     */
    public void broadcastRaw(Long roomId, String payload) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        TextMessage textMessage = new TextMessage(payload);
        for (WebSocketSession session : sessions) {
            SessionState state = sessionStates.get(session.getId());
            if (state != null) {
                state.offer(textMessage);
            }
        }
    }

    /**
     * 向单条连接发送消息，同样走异步队列，保证同一连接的发送顺序
     */
    public void send(WebSocketSession session, TextMessage textMessage) {
        if (session == null) {
            return;
        }
        SessionState state = sessionStates.get(session.getId());
        if (state != null) {
            state.offer(textMessage);
        }
    }

    /**
     * 服务端心跳：定时下发 ping 帧，浏览器会自动回 pong；
     * 超过空闲超时未活跃的连接直接断开，避免半开连接持续累积
     */
    @Scheduled(fixedDelayString = "${fufu.chat.heartbeat-interval-ms:30000}")
    public void heartbeat() {
        if (sessionStates.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (SessionState state : sessionStates.values()) {
            if (!state.session.isOpen()) {
                unregister(state.roomId, state.session);
                continue;
            }
            if (now - state.lastActive > idleTimeoutMs) {
                log.info("WebSocket 空闲超时，断开连接: sessionId={}, roomId={}",
                        state.session.getId(), state.roomId);
                unregister(state.roomId, state.session);
                closeQuietly(state.session, CloseStatus.SESSION_NOT_RELIABLE);
                continue;
            }
            state.offer(new PingMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        sendExecutor.shutdownNow();
        sessionStates.clear();
        roomSessions.clear();
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (Exception e) {
            log.debug("关闭 WebSocket 连接失败: sessionId={}, error={}",
                    session.getId(), e.getMessage());
        }
    }

    /**
     * 单条连接的发送状态：有界队列 + 单飞排空，保证顺序且不阻塞调用方
     */
    private final class SessionState {

        private final Long roomId;
        private final WebSocketSession session;
        private final BlockingQueue<WebSocketMessage<?>> queue =
                new LinkedBlockingQueue<>(MAX_PENDING_MESSAGES);
        private final AtomicBoolean draining = new AtomicBoolean(false);
        private volatile long lastActive;

        private SessionState(Long roomId, WebSocketSession session, long lastActive) {
            this.roomId = roomId;
            this.session = session;
            this.lastActive = lastActive;
        }

        private void offer(WebSocketMessage<?> message) {
            if (!session.isOpen()) {
                return;
            }
            if (!queue.offer(message)) {
                // 慢客户端：队列积压已满，丢弃并断开该连接，不影响同房间其它观众
                log.warn("WebSocket 发送队列已满，断开慢客户端: sessionId={}, roomId={}",
                        session.getId(), roomId);
                unregister(roomId, session);
                closeQuietly(session, CloseStatus.SERVICE_OVERLOAD);
                return;
            }
            scheduleDrain();
        }

        private void scheduleDrain() {
            if (draining.compareAndSet(false, true)) {
                sendExecutor.execute(this::drain);
            }
        }

        private void drain() {
            try {
                WebSocketMessage<?> message;
                while ((message = queue.poll()) != null) {
                    if (!session.isOpen()) {
                        break;
                    }
                    // WebSocketSession 不是线程安全的，同一连接的发送需串行
                    synchronized (session) {
                        session.sendMessage(message);
                    }
                }
            } catch (Exception e) {
                log.warn("WebSocket 消息发送失败，断开连接: sessionId={}, error={}",
                        session.getId(), e.getMessage());
                queue.clear();
                unregister(roomId, session);
                closeQuietly(session, CloseStatus.SESSION_NOT_RELIABLE);
            } finally {
                draining.set(false);
                if (!queue.isEmpty() && session.isOpen()) {
                    scheduleDrain();
                }
            }
        }

        private void discard() {
            queue.clear();
        }
    }
}
