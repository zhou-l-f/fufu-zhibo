package com.zlf.fufuzhibo.server.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 直播间 WebSocket 连接注册表：聊天与直播状态广播共用同一批连接
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveRoomSessionRegistry {

    private final JsonMapper jsonMapper;

    /**
     * roomId -> 该房间内的全部连接
     */
    private final Map<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    public void register(Long roomId, WebSocketSession session) {
        if (roomId == null) {
            return;
        }
        roomSessions.computeIfAbsent(roomId, key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(Long roomId, WebSocketSession session) {
        if (roomId == null) {
            return;
        }
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            roomSessions.remove(roomId, sessions);
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
            send(session, textMessage);
        }
    }

    /**
     * WebSocketSession 不是线程安全的，同一连接的发送需串行
     */
    public void send(WebSocketSession session, TextMessage textMessage) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(textMessage);
            }
        } catch (Exception e) {
            log.warn("WebSocket 消息发送失败: sessionId={}, error={}",
                    session.getId(), e.getMessage());
        }
    }
}
