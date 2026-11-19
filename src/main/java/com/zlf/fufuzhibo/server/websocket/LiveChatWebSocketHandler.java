package com.zlf.fufuzhibo.server.websocket;

import com.zlf.fufuzhibo.common.constant.MessageConstant;
import com.zlf.fufuzhibo.common.exception.BaseException;
import com.zlf.fufuzhibo.common.properties.ChatProperties;
import com.zlf.fufuzhibo.pojo.vo.ChatMessageVO;
import com.zlf.fufuzhibo.server.event.LiveStatusEventPublisher;
import com.zlf.fufuzhibo.server.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 直播间聊天处理器：连接建立时下发最近消息，聊天帧向同房间所有连接广播
 *
 * <p>协议（JSON）：
 * 连接：`/ws/live/{roomId}?token={用户JWT}&sessionId={场次id}`，sessionId 必传
 * 客户端：{"type":"chat","content":"..."}、{"type":"ping"}
 * 服务端：{"type":"init","roomId":1,"sessionId":12,"messages":[...]}、
 * {"type":"chat","data":{"seq":1,...}}、{"type":"pong"}、{"type":"error","msg":"..."}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveChatWebSocketHandler extends TextWebSocketHandler {

    private static final String TYPE_CHAT = "chat";
    private static final String TYPE_PING = "ping";
    private static final String TYPE_PONG = "pong";
    private static final String TYPE_INIT = "init";
    private static final String TYPE_ERROR = "error";

    private final ChatService chatService;
    private final ChatProperties chatProperties;
    private final JsonMapper jsonMapper;
    private final LiveRoomSessionRegistry sessionRegistry;
    private final LiveStatusEventPublisher liveStatusEventPublisher;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long roomId = roomId(session);
        Long sessionId = sessionId(session);
        if (roomId == null || sessionId == null || userId(session) == null) {
            close(session);
            return;
        }
        sessionRegistry.register(roomId, session);

        List<ChatMessageVO> recent = chatService.listRecent(sessionId, chatProperties.getInitSize());
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("type", TYPE_INIT);
        frame.put("roomId", roomId);
        frame.put("sessionId", sessionId);
        frame.put("messages", recent);
        send(session, frame);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long roomId = roomId(session);
        Long sessionId = sessionId(session);
        Long userId = userId(session);
        if (roomId == null || sessionId == null || userId == null) {
            close(session);
            return;
        }

        JsonNode node;
        try {
            node = jsonMapper.readTree(message.getPayload());
        } catch (Exception e) {
            sendError(session, MessageConstant.CHAT_TYPE_UNSUPPORTED);
            return;
        }

        String type = node.path("type").asText(TYPE_CHAT);
        if (TYPE_PING.equals(type)) {
            Map<String, Object> frame = new LinkedHashMap<>();
            frame.put("type", TYPE_PONG);
            send(session, frame);
            return;
        }
        if (TYPE_PONG.equals(type)) {
            return;
        }
        if (!TYPE_CHAT.equals(type)) {
            sendError(session, MessageConstant.CHAT_TYPE_UNSUPPORTED);
            return;
        }

        try {
            ChatMessageVO vo = chatService.sendMessage(
                    roomId, sessionId, userId, node.path("content").asText(""));
            Map<String, Object> frame = new LinkedHashMap<>();
            frame.put("type", TYPE_CHAT);
            frame.put("data", vo);
            // 经 Redis Pub/Sub 扇出到所有实例，避免多实例部署时各实例观众收不到彼此的弹幕；
            // 仍按直播间广播，前端用 data.sessionId 过滤，避免场次切换瞬间串消息
            liveStatusEventPublisher.publish(roomId, frame);
        } catch (BaseException e) {
            sendError(session, e.getMessage());
        } catch (Exception e) {
            log.error("发送聊天消息失败: roomId={}, sessionId={}, userId={}",
                    roomId, sessionId, userId, e);
            sendError(session, MessageConstant.CHAT_SEND_FAILED);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(roomId(session), session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("聊天连接异常: sessionId={}, error={}", session.getId(), exception.getMessage());
    }

    private void send(WebSocketSession session, Map<String, Object> frame) {
        if (!session.isOpen()) {
            return;
        }
        try {
            sessionRegistry.send(session,
                    new TextMessage(jsonMapper.writeValueAsString(frame)));
        } catch (Exception e) {
            log.warn("聊天消息发送失败: sessionId={}, error={}",
                    session.getId(), e.getMessage());
        }
    }

    private void sendError(WebSocketSession session, String msg) {
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("type", TYPE_ERROR);
        frame.put("msg", msg == null ? MessageConstant.CHAT_SEND_FAILED : msg);
        send(session, frame);
    }

    private void close(WebSocketSession session) {
        try {
            session.close(CloseStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            log.warn("关闭聊天连接失败: sessionId={}, error={}", session.getId(), e.getMessage());
        }
    }

    private Long roomId(WebSocketSession session) {
        return (Long) session.getAttributes().get(LiveChatHandshakeInterceptor.ATTR_ROOM_ID);
    }

    private Long sessionId(WebSocketSession session) {
        return (Long) session.getAttributes().get(LiveChatHandshakeInterceptor.ATTR_SESSION_ID);
    }

    private Long userId(WebSocketSession session) {
        return (Long) session.getAttributes().get(LiveChatHandshakeInterceptor.ATTR_USER_ID);
    }
}
