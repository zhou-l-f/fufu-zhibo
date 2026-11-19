package com.zlf.fufuzhibo.server.service.impl;

import com.zlf.fufuzhibo.common.constant.ErrorCode;
import com.zlf.fufuzhibo.common.constant.LiveStatusConstant;
import com.zlf.fufuzhibo.common.constant.MessageConstant;
import com.zlf.fufuzhibo.common.exception.BaseException;
import com.zlf.fufuzhibo.common.properties.ChatProperties;
import com.zlf.fufuzhibo.pojo.entity.LiveMessage;
import com.zlf.fufuzhibo.pojo.entity.LiveRoom;
import com.zlf.fufuzhibo.pojo.entity.LiveSession;
import com.zlf.fufuzhibo.pojo.entity.User;
import com.zlf.fufuzhibo.pojo.vo.ChatMessageVO;
import com.zlf.fufuzhibo.server.mapper.LiveMessageMapper;
import com.zlf.fufuzhibo.server.mapper.LiveRoomMapper;
import com.zlf.fufuzhibo.server.mapper.LiveSessionMapper;
import com.zlf.fufuzhibo.server.mapper.UserMapper;
import com.zlf.fufuzhibo.server.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 聊天消息：以直播场次为维度，发言只写 Redis（最近消息 + 缓冲队列）并立即广播，
 * 由定时任务批量落库，避免每条消息一次 MySQL 写入
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    /**
     * 场次最近消息缓存：live:chat:recent:{sessionId}，列表头为最新一条
     */
    private static final String RECENT_KEY_PREFIX = "live:chat:recent:";

    /**
     * 发言限流键：live:chat:limit:{sessionId}:{userId}
     */
    private static final String LIMIT_KEY_PREFIX = "live:chat:limit:";

    /**
     * 场次内消息序号：live:chat:seq:{sessionId}
     */
    private static final String SEQ_KEY_PREFIX = "live:chat:seq:";

    /**
     * 待落库消息缓冲区：live:chat:pending，右进左出保证顺序
     */
    public static final String BUFFER_KEY = "live:chat:pending";

    /**
     * 序号键过期时间，每次生成序号时刷新，避免场次结束后残留
     */
    private static final long SEQ_TTL_SECONDS = Duration.ofDays(7).toSeconds();

    /**
     * INCR 与 EXPIRE 合成一次调用，减少往返
     */
    private static final DefaultRedisScript<Long> NEXT_SEQ_SCRIPT = new DefaultRedisScript<>(
            "local v = redis.call('incr', KEYS[1]) "
                    + "redis.call('expire', KEYS[1], ARGV[1]) "
                    + "return v",
            Long.class);

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MESSAGE_STATUS_NORMAL = 1;

    private final LiveRoomMapper liveRoomMapper;
    private final LiveSessionMapper liveSessionMapper;
    private final LiveMessageMapper liveMessageMapper;
    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final JsonMapper jsonMapper;
    private final ChatProperties chatProperties;

    @Override
    public ChatMessageVO sendMessage(Long roomId, Long sessionId, Long userId, String content) {
        String text = content == null ? "" : content.trim();
        int maxLength = chatProperties.getMaxLength();
        if (text.isEmpty()) {
            throw new BaseException(MessageConstant.CHAT_CONTENT_EMPTY);
        }
        if (text.length() > maxLength) {
            throw new BaseException(
                    String.format(MessageConstant.CHAT_CONTENT_TOO_LONG, maxLength));
        }

        // 场次必须属于该直播间，且尚未彻底结束（待开播与直播中都可聊天）
        LiveSession session = liveSessionMapper.selectById(sessionId);
        if (session == null || !roomId.equals(session.getRoomId())) {
            throw new BaseException(MessageConstant.CHAT_SESSION_INVALID);
        }
        if (LiveStatusConstant.SESSION_ENDED.equals(session.getStatus())) {
            throw new BaseException(MessageConstant.CHAT_SESSION_ENDED);
        }

        LiveRoom room = liveRoomMapper.selectById(roomId);
        if (room == null) {
            throw new BaseException(ErrorCode.LIVE_ROOM_NOT_FOUND);
        }
        if (LiveStatusConstant.ROOM_BANNED.equals(room.getStatus())) {
            throw new BaseException(MessageConstant.CHAT_ROOM_BANNED);
        }

        tryAcquireRateLimit(sessionId, userId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BaseException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        LiveMessage message = new LiveMessage();
        message.setRoomId(roomId);
        message.setSessionId(sessionId);
        message.setSeq(nextSeq(sessionId));
        message.setUserId(userId);
        message.setContent(filterSensitiveWords(text));
        message.setStatus(MESSAGE_STATUS_NORMAL);
        message.setCreateTime(LocalDateTime.now(ZONE));

        ChatMessageVO vo = toVO(message, user);
        // 先入缓冲区保证可落库，再写最近消息缓存
        enqueue(message);
        cacheRecent(sessionId, vo);
        return vo;
    }

    @Override
    public List<ChatMessageVO> listHistory(Long roomId, Long sessionId, Long lastSeq, int size) {
        LiveSession session = liveSessionMapper.selectById(sessionId);
        if (session == null || !roomId.equals(session.getRoomId())) {
            throw new BaseException(MessageConstant.CHAT_SESSION_INVALID);
        }

        int limit = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        List<ChatMessageVO> records = liveMessageMapper.selectHistory(sessionId, lastSeq, limit);
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }
        // 数据库按 seq 倒序取最新一页，返回给前端时翻转为正序
        List<ChatMessageVO> ordered = new ArrayList<>(records);
        Collections.reverse(ordered);
        return ordered;
    }

    @Override
    public List<ChatMessageVO> listRecent(Long sessionId, int size) {
        int limit = Math.max(1, Math.min(size, chatProperties.getRecentSize()));
        List<ChatMessageVO> cached = readRecentCache(sessionId, limit);
        if (!cached.isEmpty()) {
            return cached;
        }
        List<ChatMessageVO> records = liveMessageMapper.selectHistory(sessionId, null, limit);
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }
        List<ChatMessageVO> ordered = new ArrayList<>(records);
        Collections.reverse(ordered);
        return ordered;
    }

    /**
     * Redis SET NX EX 限流，按场次维度隔离；Redis 不可用时放行，避免聊天整体不可用
     */
    private void tryAcquireRateLimit(Long sessionId, Long userId) {
        String key = LIMIT_KEY_PREFIX + sessionId + ":" + userId;
        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, "1",
                            Duration.ofSeconds(chatProperties.getRateLimitSeconds()));
            if (Boolean.FALSE.equals(acquired)) {
                throw new BaseException(MessageConstant.CHAT_TOO_FREQUENT);
            }
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.warn("聊天限流校验失败，放行本次发言: {}", e.getMessage());
        }
    }

    private long nextSeq(Long sessionId) {
        String key = SEQ_KEY_PREFIX + sessionId;
        try {
            // Redis 重启/淘汰后序号键会丢失，若直接从 1 开始会与已落库消息冲突并被幂等写入静默丢弃
            if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
                Long maxSeq = liveMessageMapper.selectMaxSeq(sessionId);
                stringRedisTemplate.opsForValue().setIfAbsent(
                        key, String.valueOf(maxSeq == null ? 0L : maxSeq),
                        Duration.ofSeconds(SEQ_TTL_SECONDS));
            }
            Long seq = stringRedisTemplate.execute(
                    NEXT_SEQ_SCRIPT, List.of(key), String.valueOf(SEQ_TTL_SECONDS));
            if (seq == null) {
                throw new IllegalStateException("序号生成返回空");
            }
            return seq;
        } catch (Exception e) {
            log.error("生成消息序号失败: sessionId={}", sessionId, e);
            throw new BaseException(MessageConstant.CHAT_SEND_FAILED);
        }
    }

    private String filterSensitiveWords(String text) {
        String filtered = text;
        List<String> words = chatProperties.getSensitiveWords();
        if (words == null || words.isEmpty()) {
            return filtered;
        }
        for (String word : words) {
            if (word == null || word.isBlank() || !filtered.contains(word)) {
                continue;
            }
            filtered = filtered.replace(word, "*".repeat(word.length()));
        }
        return filtered;
    }

    /**
     * 消息放入 Redis 缓冲队列，由定时任务批量写入 MySQL
     */
    private void enqueue(LiveMessage message) {
        try {
            stringRedisTemplate.opsForList()
                    .rightPush(BUFFER_KEY, jsonMapper.writeValueAsString(message));
        } catch (Exception e) {
            log.error("写入聊天缓冲区失败: sessionId={}, seq={}",
                    message.getSessionId(), message.getSeq(), e);
            throw new BaseException(MessageConstant.CHAT_SEND_FAILED);
        }
    }

    private void cacheRecent(Long sessionId, ChatMessageVO vo) {
        String key = RECENT_KEY_PREFIX + sessionId;
        try {
            stringRedisTemplate.opsForList().leftPush(key, jsonMapper.writeValueAsString(vo));
            stringRedisTemplate.opsForList().trim(key, 0, chatProperties.getRecentSize() - 1L);
            stringRedisTemplate.expire(key, Duration.ofHours(chatProperties.getCacheHours()));
        } catch (Exception e) {
            log.warn("写入最近消息缓存失败: sessionId={}, error={}", sessionId, e.getMessage());
        }
    }

    /**
     * 读取 Redis 最近消息并翻转为正序；缓存异常或无数据时返回空列表
     */
    private List<ChatMessageVO> readRecentCache(Long sessionId, int limit) {
        String key = RECENT_KEY_PREFIX + sessionId;
        List<String> values;
        try {
            values = stringRedisTemplate.opsForList().range(key, 0, limit - 1L);
        } catch (Exception e) {
            log.warn("读取最近消息缓存失败: sessionId={}, error={}", sessionId, e.getMessage());
            return List.of();
        }
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        List<ChatMessageVO> messages = new ArrayList<>(values.size());
        for (String value : values) {
            try {
                messages.add(jsonMapper.readValue(value, ChatMessageVO.class));
            } catch (Exception e) {
                log.warn("最近消息缓存解析失败: sessionId={}, error={}", sessionId, e.getMessage());
            }
        }
        Collections.reverse(messages);
        return messages;
    }

    private ChatMessageVO toVO(LiveMessage message, User user) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
        vo.setSeq(message.getSeq());
        vo.setRoomId(message.getRoomId());
        vo.setSessionId(message.getSessionId());
        vo.setUserId(message.getUserId());
        vo.setUserName(user.getName());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setContent(message.getContent());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
