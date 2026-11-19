package com.zlf.fufuzhibo.server.task;

import com.zlf.fufuzhibo.common.properties.ChatProperties;
import com.zlf.fufuzhibo.pojo.entity.LiveMessage;
import com.zlf.fufuzhibo.server.mapper.LiveMessageMapper;
import com.zlf.fufuzhibo.server.service.impl.ChatServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 聊天消息批量落库：从 Redis 缓冲队列取一批消息写入 MySQL。
 * 写入失败时把整批消息按原顺序放回队头，下一轮重试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageFlushTask {

    private final StringRedisTemplate stringRedisTemplate;
    private final LiveMessageMapper liveMessageMapper;
    private final JsonMapper jsonMapper;
    private final ChatProperties chatProperties;

    @Scheduled(fixedDelayString = "${fufu.chat.flush-interval-ms:2000}")
    public void flush() {
        int batchSize = chatProperties.getFlushBatchSize();
        List<String> payloads;
        try {
            payloads = stringRedisTemplate.opsForList()
                    .leftPop(ChatServiceImpl.BUFFER_KEY, batchSize);
        } catch (Exception e) {
            log.warn("读取聊天缓冲区失败，跳过本轮: {}", e.getMessage());
            return;
        }
        if (payloads == null || payloads.isEmpty()) {
            return;
        }

        List<LiveMessage> messages = new ArrayList<>(payloads.size());
        for (String payload : payloads) {
            try {
                messages.add(jsonMapper.readValue(payload, LiveMessage.class));
            } catch (Exception e) {
                log.warn("聊天缓冲消息解析失败，丢弃: {}", e.getMessage());
            }
        }
        if (messages.isEmpty()) {
            return;
        }

        try {
            int inserted = liveMessageMapper.insertBatch(messages);
            log.debug("聊天消息批量落库: 取出={}, 解析={}, 写入={}",
                    payloads.size(), messages.size(), inserted);
        } catch (Exception e) {
            log.error("聊天消息批量落库失败，回滚回缓冲区: size={}", payloads.size(), e);
            // LPUSH 按参数顺序逐条压入头部，反转后原顺序保持不变
            List<String> retry = new ArrayList<>(payloads);
            Collections.reverse(retry);
            try {
                stringRedisTemplate.opsForList()
                        .leftPushAll(ChatServiceImpl.BUFFER_KEY, retry);
            } catch (Exception ex) {
                log.error("回滚聊天缓冲区失败，本批消息可能丢失: size={}", payloads.size(), ex);
            }
        }
    }
}
