package com.zlf.fufuzhibo.server.event;

import com.zlf.fufuzhibo.server.websocket.LiveRoomSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 直播状态事件发布：通过 Redis Pub/Sub 扇出到所有实例，再由各实例推送给本机连接
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveStatusEventPublisher {

    /**
     * 直播状态广播频道
     */
    public static final String CHANNEL = "live:status";

    private final StringRedisTemplate stringRedisTemplate;
    private final JsonMapper jsonMapper;
    private final LiveRoomSessionRegistry sessionRegistry;

    /**
     * 发布状态帧；Redis 不可用时降级为本机广播，保证单实例部署仍能收到状态变更
     */
    public void publish(Long roomId, Map<String, Object> frame) {
        if (roomId == null || frame == null) {
            return;
        }
        try {
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("roomId", roomId);
            // payload 以字符串承载，订阅端可原样转发，避免多实例重复序列化
            envelope.put("payload", jsonMapper.writeValueAsString(frame));
            stringRedisTemplate.convertAndSend(CHANNEL, jsonMapper.writeValueAsString(envelope));
        } catch (Exception e) {
            log.warn("发布直播状态事件失败，降级为本机广播: roomId={}, error={}",
                    roomId, e.getMessage());
            sessionRegistry.broadcast(roomId, frame);
        }
    }
}
