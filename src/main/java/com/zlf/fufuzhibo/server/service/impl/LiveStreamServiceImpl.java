package com.zlf.fufuzhibo.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zlf.fufuzhibo.common.constant.LiveStatusConstant;
import com.zlf.fufuzhibo.common.constant.MessageConstant;
import com.zlf.fufuzhibo.common.exception.BaseException;
import com.zlf.fufuzhibo.common.properties.PushProperties;
import com.zlf.fufuzhibo.common.properties.SrsProperties;
import com.zlf.fufuzhibo.common.utils.PushTokenUtil;
import com.zlf.fufuzhibo.pojo.dto.SrsCallbackDTO;
import com.zlf.fufuzhibo.pojo.entity.LiveRoom;
import com.zlf.fufuzhibo.pojo.entity.LiveSession;
import com.zlf.fufuzhibo.server.mapper.LiveRoomMapper;
import com.zlf.fufuzhibo.server.mapper.LiveSessionMapper;
import com.zlf.fufuzhibo.server.service.LiveStreamService;
import com.zlf.fufuzhibo.server.service.PublisherValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 直播推流业务：处理 SRS 的 on_publish / on_unpublish 回调，并对账僵尸场次
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveStreamServiceImpl implements LiveStreamService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final String PUBLISH_LOCK_PREFIX = "live:publish:";
    private static final Duration PUBLISH_LOCK_TTL = Duration.ofHours(24);

    /**
     * 刚开播的场次在 SRS API 中可能还没出现，对账时先放过这段时间
     */
    private static final long RECONCILE_START_GRACE_SECONDS = 15;

    /**
     * 仅当锁的持有者仍是当前 clientId 时才删除，避免误删新的推流锁
     */
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final LiveRoomMapper liveRoomMapper;
    private final LiveSessionMapper liveSessionMapper;
    private final PublisherValidator publisherValidator;
    private final StringRedisTemplate stringRedisTemplate;
    private final PushProperties pushProperties;
    private final SrsProperties srsProperties;

    /**
     * 推流开始：校验 token 与场次，首次推流绑定 clientId，断流宽限期内允许重连恢复
     */
    @Override
    @Transactional
    public void onPublish(SrsCallbackDTO callback) {
        String streamName = callback.getStream();
        String clientId = callback.getClientId();
        if (isBlank(streamName) || isBlank(clientId)) {
            throw new BaseException(MessageConstant.PUSH_PARAM_INVALID);
        }
        if (!isBlank(srsProperties.getApp())
                && !srsProperties.getApp().equals(callback.getApp())) {
            throw new BaseException(MessageConstant.PUSH_APP_MISMATCH);
        }

        String token = extractToken(callback.getParam());
        PushTokenUtil.Payload payload =
                PushTokenUtil.verify(pushProperties.getSecretKey(), token, streamName);
        Long tokenUserId = payload.userId();
        Long sessionId = payload.sessionId();

        LiveRoom room = liveRoomMapper.selectOne(
                Wrappers.<LiveRoom>lambdaQuery().eq(LiveRoom::getStreamName, streamName));
        if (room == null) {
            throw new BaseException(MessageConstant.PUSH_ROOM_NOT_FOUND);
        }
        if (LiveStatusConstant.ROOM_BANNED.equals(room.getStatus())) {
            throw new BaseException(MessageConstant.PUSH_ROOM_BANNED);
        }
        if (!room.getUserId().equals(tokenUserId)) {
            throw new BaseException(MessageConstant.PUSH_USER_MISMATCH);
        }
        if (LiveStatusConstant.ROOM_LIVE.equals(room.getStatus())
                && !isBlank(room.getCurrentClientId())
                && !room.getCurrentClientId().equals(clientId)) {
            throw new BaseException(MessageConstant.PUSH_STREAM_OCCUPIED);
        }

        LiveSession session = liveSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BaseException(MessageConstant.LIVE_SESSION_NOT_FOUND);
        }
        if (!session.getRoomId().equals(room.getId())
                || !session.getUserId().equals(tokenUserId)) {
            throw new BaseException(MessageConstant.LIVE_SESSION_MISMATCH);
        }

        LocalDateTime now = LocalDateTime.now(ZONE);
        boolean reconnect = false;
        if (LiveStatusConstant.SESSION_ENDED.equals(session.getStatus())) {
            if (!canReconnect(session, now)) {
                throw new BaseException(MessageConstant.LIVE_SESSION_ENDED);
            }
            reconnect = true;
        } else if (LiveStatusConstant.SESSION_LIVE.equals(session.getStatus())) {
            if (clientId.equals(session.getClientId())) {
                log.info("重复的 on_publish 回调，场次已直播中: sessionId={}, clientId={}",
                        sessionId, clientId);
                return;
            }
            throw new BaseException(MessageConstant.PUSH_STREAM_OCCUPIED);
        } else if (!LiveStatusConstant.SESSION_PENDING.equals(session.getStatus())) {
            throw new BaseException(MessageConstant.LIVE_SESSION_MISMATCH);
        }

        // Redis 并发控制：一个直播间同一时刻只允许一路推流
        String lockKey = PUBLISH_LOCK_PREFIX + streamName;
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, clientId, PUBLISH_LOCK_TTL);
        if (!Boolean.TRUE.equals(locked)) {
            String holder = stringRedisTemplate.opsForValue().get(lockKey);
            if (clientId.equals(holder)) {
                log.info("on_publish 重复回调，锁已由同一 clientId 持有: stream={}, clientId={}",
                        streamName, clientId);
                return;
            }
            throw new BaseException(MessageConstant.PUSH_STREAM_OCCUPIED);
        }

        try {
            publisherValidator.check(room.getUserId());

            LambdaUpdateWrapper<LiveSession> sessionUpdate = Wrappers.<LiveSession>lambdaUpdate()
                    .eq(LiveSession::getId, sessionId)
                    .set(LiveSession::getClientId, clientId)
                    .set(LiveSession::getStatus, LiveStatusConstant.SESSION_LIVE)
                    .set(LiveSession::getUpdateTime, now);
            if (reconnect) {
                sessionUpdate.eq(LiveSession::getStatus, LiveStatusConstant.SESSION_ENDED)
                        .set(LiveSession::getEndTime, null)
                        .set(LiveSession::getEndReason, null)
                        .set(LiveSession::getDuration, null);
            } else {
                sessionUpdate.eq(LiveSession::getStatus, LiveStatusConstant.SESSION_PENDING)
                        .set(LiveSession::getStartTime, now);
            }
            if (liveSessionMapper.update(null, sessionUpdate) != 1) {
                throw new BaseException(MessageConstant.LIVE_SESSION_MISMATCH);
            }

            LiveRoom roomUpdate = new LiveRoom();
            roomUpdate.setId(room.getId());
            roomUpdate.setStatus(LiveStatusConstant.ROOM_LIVE);
            roomUpdate.setCurrentClientId(clientId);
            if (!reconnect) {
                roomUpdate.setLastStartTime(now);
            }
            liveRoomMapper.updateById(roomUpdate);

            log.info("推流{}: stream={}, userId={}, sessionId={}, clientId={}",
                    reconnect ? "重连恢复" : "开始",
                    streamName, room.getUserId(), sessionId, clientId);
        } catch (RuntimeException e) {
            releaseLock(streamName, clientId);
            throw e;
        }
    }

    /**
     * 推流结束：结束会话并恢复直播间状态，重复回调直接忽略
     */
    @Override
    @Transactional
    public void onUnpublish(SrsCallbackDTO callback) {
        String clientId = callback.getClientId();
        if (isBlank(clientId)) {
            log.warn("on_unpublish 缺少 client_id，忽略");
            return;
        }

        LiveSession session = liveSessionMapper.selectOne(
                Wrappers.<LiveSession>lambdaQuery().eq(LiveSession::getClientId, clientId));
        if (session == null) {
            log.warn("on_unpublish 未找到会话: clientId={}", clientId);
            releaseLock(callback.getStream(), clientId);
            return;
        }
        if (LiveStatusConstant.SESSION_ENDED.equals(session.getStatus())) {
            releaseLock(session.getStreamName(), clientId);
            return;
        }

        LocalDateTime now = LocalDateTime.now(ZONE);

        LiveSession sessionUpdate = new LiveSession();
        sessionUpdate.setId(session.getId());
        sessionUpdate.setStatus(LiveStatusConstant.SESSION_ENDED);
        sessionUpdate.setEndTime(now);
        if (session.getStartTime() != null) {
            long seconds = Duration.between(session.getStartTime(), now).getSeconds();
            sessionUpdate.setDuration((int) Math.max(seconds, 0));
        }
        sessionUpdate.setEndReason(LiveStatusConstant.END_REASON_NORMAL);
        liveSessionMapper.updateById(sessionUpdate);

        // 仅当直播间仍由该 client 推流时才回写，避免覆盖已经开始的下一路推流
        liveRoomMapper.update(null,
                Wrappers.<LiveRoom>lambdaUpdate()
                        .eq(LiveRoom::getId, session.getRoomId())
                        .eq(LiveRoom::getCurrentClientId, clientId)
                        .set(LiveRoom::getStatus, LiveStatusConstant.ROOM_NOT_STARTED)
                        .set(LiveRoom::getCurrentClientId, null)
                        .set(LiveRoom::getLastEndTime, now)
                        .set(LiveRoom::getUpdateTime, now));

        releaseLock(session.getStreamName(), clientId);

        log.info("推流结束: stream={}, sessionId={}, clientId={}",
                session.getStreamName(), session.getId(), clientId);
    }

    /**
     * 与 SRS 的在线流列表对账，回收回调丢失导致的僵尸场次
     */
    @Override
    @Transactional
    public int reconcile(Set<String> activeStreams) {
        List<LiveSession> sessions = liveSessionMapper.selectList(
                Wrappers.<LiveSession>lambdaQuery()
                        .eq(LiveSession::getStatus, LiveStatusConstant.SESSION_LIVE));
        if (sessions.isEmpty()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now(ZONE);
        int closed = 0;
        for (LiveSession session : sessions) {
            // 刚开播的场次可能还没出现在 SRS API 里，先放过
            if (session.getStartTime() != null
                    && session.getStartTime()
                    .isAfter(now.minusSeconds(RECONCILE_START_GRACE_SECONDS))) {
                continue;
            }
            String streamKey = srsProperties.getApp() + "/" + session.getStreamName();
            if (activeStreams.contains(streamKey)) {
                continue;
            }

            LiveSession sessionUpdate = new LiveSession();
            sessionUpdate.setId(session.getId());
            sessionUpdate.setStatus(LiveStatusConstant.SESSION_ENDED);
            sessionUpdate.setEndTime(now);
            if (session.getStartTime() != null) {
                long seconds = Duration.between(session.getStartTime(), now).getSeconds();
                sessionUpdate.setDuration((int) Math.max(seconds, 0));
            }
            sessionUpdate.setEndReason(LiveStatusConstant.END_REASON_TIMEOUT);
            liveSessionMapper.updateById(sessionUpdate);

            LambdaUpdateWrapper<LiveRoom> roomUpdate = Wrappers.<LiveRoom>lambdaUpdate()
                    .eq(LiveRoom::getId, session.getRoomId())
                    .set(LiveRoom::getStatus, LiveStatusConstant.ROOM_NOT_STARTED)
                    .set(LiveRoom::getCurrentClientId, null)
                    .set(LiveRoom::getLastEndTime, now)
                    .set(LiveRoom::getUpdateTime, now);
            if (session.getClientId() == null) {
                roomUpdate.isNull(LiveRoom::getCurrentClientId);
            } else {
                roomUpdate.eq(LiveRoom::getCurrentClientId, session.getClientId());
            }
            liveRoomMapper.update(null, roomUpdate);

            releaseLock(session.getStreamName(), session.getClientId());
            closed++;
            log.warn("对账回收僵尸场次: stream={}, sessionId={}, clientId={}",
                    session.getStreamName(), session.getId(), session.getClientId());
        }
        return closed;
    }

    /**
     * 断流后宽限期内且为正常断流的场次，允许 OBS 重连恢复
     */
    private boolean canReconnect(LiveSession session, LocalDateTime now) {
        if (!LiveStatusConstant.END_REASON_NORMAL.equals(session.getEndReason())) {
            return false;
        }
        if (session.getEndTime() == null) {
            return false;
        }
        long graceSeconds = pushProperties.getReconnectGraceSeconds() == null
                ? 60L : pushProperties.getReconnectGraceSeconds();
        long elapsed = Duration.between(session.getEndTime(), now).getSeconds();
        return elapsed >= 0 && elapsed <= graceSeconds;
    }

    private void releaseLock(String streamName, String clientId) {
        if (isBlank(streamName) || isBlank(clientId)) {
            return;
        }
        try {
            stringRedisTemplate.execute(RELEASE_LOCK_SCRIPT,
                    Collections.singletonList(PUBLISH_LOCK_PREFIX + streamName), clientId);
        } catch (Exception e) {
            // Redis 异常不能影响主流程事务
            log.warn("释放推流锁失败: stream={}, clientId={}", streamName, clientId, e);
        }
    }

    private String extractToken(String param) {
        if (isBlank(param)) {
            return null;
        }
        String query = param.startsWith("?") ? param.substring(1) : param;
        for (String pair : query.split("&")) {
            int index = pair.indexOf('=');
            if (index > 0 && "token".equals(pair.substring(0, index))) {
                try {
                    return URLDecoder.decode(pair.substring(index + 1), StandardCharsets.UTF_8);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
