package com.zlf.fufuzhibo.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 聊天消息：发言先落库，再写入 Redis 最近消息列表；历史消息按 id 游标回源数据库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    /**
     * 最近消息缓存键：live:chat:{roomId}，列表头为最新一条
     */
    private static final String RECENT_KEY_PREFIX = "live:chat:";

    /**
     * 发言限流键：live:chat:limit:{roomId}:{userId}
     */
    private static final String LIMIT_KEY_PREFIX = "live:chat:limit:";

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
    public ChatMessageVO sendMessage(Long roomId, Long userId, String content) {
        String text = content == null ? "" : content.trim();
        int maxLength = chatProperties.getMaxLength();
        if (text.isEmpty()) {
            throw new BaseException(MessageConstant.CHAT_CONTENT_EMPTY);
        }
        if (text.length() > maxLength) {
            throw new BaseException(
                    String.format(MessageConstant.CHAT_CONTENT_TOO_LONG, maxLength));
        }

        // 仅直播中的房间可发言，与详情接口保持一致的错误码
        LiveRoom room = liveRoomMapper.selectById(roomId);
        if (room == null || !LiveStatusConstant.ROOM_LIVE.equals(room.getStatus())) {
            throw new BaseException(ErrorCode.LIVE_ROOM_NOT_FOUND);
        }

        tryAcquireRateLimit(roomId, userId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BaseException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        LiveMessage message = new LiveMessage();
        message.setRoomId(roomId);
        message.setSessionId(findLiveSessionId(roomId));
        message.setUserId(userId);
        message.setContent(filterSensitiveWords(text));
        message.setStatus(MESSAGE_STATUS_NORMAL);
        liveMessageMapper.insert(message);

        ChatMessageVO vo = toVO(message, user);
        cacheRecent(vo);
        return vo;
    }

    @Override
    public List<ChatMessageVO> listHistory(Long roomId, Long lastId, int size) {
        int limit = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        List<ChatMessageVO> records = liveMessageMapper.selectHistory(roomId, lastId, limit);
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }
        // 数据库按 id 倒序取最新一页，返回给前端时翻转为正序
        List<ChatMessageVO> ordered = new ArrayList<>(records);
        Collections.reverse(ordered);
        return ordered;
    }

    @Override
    public List<ChatMessageVO> listRecent(Long roomId, int size) {
        int limit = Math.max(1, Math.min(size, chatProperties.getRecentSize()));
        List<ChatMessageVO> cached = readRecentCache(roomId, limit);
        if (!cached.isEmpty()) {
            return cached;
        }
        return listHistory(roomId, null, limit);
    }

    /**
     * Redis SET NX EX 限流；Redis 不可用时放行，避免聊天整体不可用
     */
    private void tryAcquireRateLimit(Long roomId, Long userId) {
        String key = LIMIT_KEY_PREFIX + roomId + ":" + userId;
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

    private Long findLiveSessionId(Long roomId) {
        LiveSession session = liveSessionMapper.selectOne(
                new LambdaQueryWrapper<LiveSession>()
                        .eq(LiveSession::getRoomId, roomId)
                        .eq(LiveSession::getStatus, LiveStatusConstant.SESSION_LIVE)
                        .orderByDesc(LiveSession::getId)
                        .last("LIMIT 1"));
        return session == null ? null : session.getId();
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

    private void cacheRecent(ChatMessageVO vo) {
        String key = RECENT_KEY_PREFIX + vo.getRoomId();
        try {
            stringRedisTemplate.opsForList().leftPush(key, jsonMapper.writeValueAsString(vo));
            stringRedisTemplate.opsForList().trim(key, 0, chatProperties.getRecentSize() - 1L);
            stringRedisTemplate.expire(key, Duration.ofHours(chatProperties.getCacheHours()));
        } catch (Exception e) {
            log.warn("写入最近消息缓存失败: roomId={}, error={}", vo.getRoomId(), e.getMessage());
        }
    }

    /**
     * 读取 Redis 最近消息并翻转为正序；缓存异常或无数据时返回空列表
     */
    private List<ChatMessageVO> readRecentCache(Long roomId, int limit) {
        String key = RECENT_KEY_PREFIX + roomId;
        List<String> values;
        try {
            values = stringRedisTemplate.opsForList().range(key, 0, limit - 1L);
        } catch (Exception e) {
            log.warn("读取最近消息缓存失败: roomId={}, error={}", roomId, e.getMessage());
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
                log.warn("最近消息缓存解析失败: roomId={}, error={}", roomId, e.getMessage());
            }
        }
        Collections.reverse(messages);
        return messages;
    }

    private ChatMessageVO toVO(LiveMessage message, User user) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
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
