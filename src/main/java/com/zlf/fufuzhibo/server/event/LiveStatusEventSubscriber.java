package com.zlf.fufuzhibo.server.event;

import com.zlf.fufuzhibo.server.websocket.LiveRoomSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

/**
 * 直播状态事件订阅：收到其它实例发布的状态帧后推送给本机对应房间的连接
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveStatusEventSubscriber implements MessageListener {

    private final JsonMapper jsonMapper;
    private final LiveRoomSessionRegistry sessionRegistry;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            JsonNode node = jsonMapper.readTree(body);
            long roomId = node.path("roomId").asLong(0);
            String payload = node.path("payload").asText("");
            if (roomId <= 0 || payload.isEmpty()) {
                return;
            }
            sessionRegistry.broadcastRaw(roomId, payload);
        } catch (Exception e) {
            log.warn("处理直播状态事件失败: error={}", e.getMessage());
        }
    }
}
