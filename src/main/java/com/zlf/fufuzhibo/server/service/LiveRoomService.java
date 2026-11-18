package com.zlf.fufuzhibo.server.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zlf.fufuzhibo.common.constant.LiveStatusConstant;
import com.zlf.fufuzhibo.common.constant.MessageConstant;
import com.zlf.fufuzhibo.common.exception.BaseException;
import com.zlf.fufuzhibo.common.properties.PushProperties;
import com.zlf.fufuzhibo.common.properties.SrsProperties;
import com.zlf.fufuzhibo.common.utils.PushTokenUtil;
import com.zlf.fufuzhibo.pojo.dto.StartLiveDTO;
import com.zlf.fufuzhibo.pojo.entity.LiveRoom;
import com.zlf.fufuzhibo.pojo.entity.LiveSession;
import com.zlf.fufuzhibo.pojo.vo.LiveRoomVO;
import com.zlf.fufuzhibo.pojo.vo.StartLiveVO;
import com.zlf.fufuzhibo.server.mapper.LiveRoomMapper;
import com.zlf.fufuzhibo.server.mapper.LiveSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * 直播间与开播申请
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveRoomService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final LiveRoomMapper liveRoomMapper;
    private final LiveSessionMapper liveSessionMapper;
    private final PublisherValidator publisherValidator;
    private final PushProperties pushProperties;
    private final SrsProperties srsProperties;

    /**
     * 注册直播间：校验开播资格后分配唯一流名，重复调用返回已有直播间
     */
    @Transactional
    public LiveRoomVO registerRoom(Long userId) {
        publisherValidator.check(userId);

        LiveRoom existing = liveRoomMapper.selectOne(
                Wrappers.<LiveRoom>lambdaQuery().eq(LiveRoom::getUserId, userId));
        if (existing != null) {
            return toVO(existing);
        }

        LiveRoom room = new LiveRoom();
        room.setUserId(userId);
        room.setStreamName(generateStreamName());
        room.setStatus(LiveStatusConstant.ROOM_NOT_STARTED);
        try {
            liveRoomMapper.insert(room);
        } catch (DuplicateKeyException e) {
            // 并发重复注册，返回已存在的直播间
            LiveRoom current = liveRoomMapper.selectOne(
                    Wrappers.<LiveRoom>lambdaQuery().eq(LiveRoom::getUserId, userId));
            if (current == null) {
                throw e;
            }
            return toVO(current);
        }

        log.info("直播间注册成功: userId={}, streamName={}", userId, room.getStreamName());
        return toVO(room);
    }

    /**
     * 申请开始直播：创建待开播场次并签发短效推流 token
     */
    @Transactional
    public StartLiveVO startLive(Long userId, StartLiveDTO dto) {
        publisherValidator.check(userId);

        // 行锁串行化同一用户的申请，避免并发创建多个待开播场次
        LiveRoom room = liveRoomMapper.getByUserIdForUpdate(userId);
        if (room == null) {
            throw new BaseException(MessageConstant.PUSH_ROOM_NOT_FOUND);
        }
        if (LiveStatusConstant.ROOM_BANNED.equals(room.getStatus())) {
            throw new BaseException(MessageConstant.PUSH_ROOM_BANNED);
        }
        if (LiveStatusConstant.ROOM_LIVE.equals(room.getStatus())) {
            throw new BaseException(MessageConstant.LIVE_ALREADY_LIVE);
        }

        LocalDateTime now = LocalDateTime.now(ZONE);

        // 旧的待开播场次作废，确保同一时刻只有最新场次可推流
        liveSessionMapper.update(null,
                Wrappers.<LiveSession>lambdaUpdate()
                        .eq(LiveSession::getRoomId, room.getId())
                        .eq(LiveSession::getStatus, LiveStatusConstant.SESSION_PENDING)
                        .set(LiveSession::getStatus, LiveStatusConstant.SESSION_ENDED)
                        .set(LiveSession::getEndTime, now)
                        .set(LiveSession::getDuration, 0)
                        .set(LiveSession::getEndReason, LiveStatusConstant.END_REASON_CANCELLED)
                        .set(LiveSession::getUpdateTime, now));

        LiveSession session = new LiveSession();
        session.setRoomId(room.getId());
        session.setUserId(userId);
        session.setTitle(dto.getTitle().trim());
        session.setCoverUrl(dto.getCoverUrl());
        session.setStreamName(room.getStreamName());
        session.setStatus(LiveStatusConstant.SESSION_PENDING);
        liveSessionMapper.insert(session);

        String token = PushTokenUtil.create(
                pushProperties.getSecretKey(),
                userId,
                session.getId(),
                room.getStreamName(),
                pushProperties.getExpireTime());

        log.info("申请开播成功: userId={}, roomId={}, sessionId={}, streamName={}",
                userId, room.getId(), session.getId(), room.getStreamName());
        return buildStartLiveVO(room, session, token);
    }

    private StartLiveVO buildStartLiveVO(LiveRoom room, LiveSession session, String token) {
        String host = srsProperties.getRtmpHost();
        String app = srsProperties.getApp();
        Integer rtmpPort = srsProperties.getRtmpPort();
        Integer httpPort = srsProperties.getHttpPort();

        String streamName = session.getStreamName();
        String rtmpBase = String.format("%s:%d/%s/%s", host, rtmpPort, app, streamName);
        String httpBase = String.format("%s:%d/%s/%s", host, httpPort, app, streamName);

        return StartLiveVO.builder()
                .roomId(room.getId())
                .sessionId(session.getId())
                .streamName(streamName)
                .serverUrl(String.format("rtmp://%s:%d/%s", host, rtmpPort, app))
                .streamKey(streamName + "?token=" + token)
                .rtmpPlayUrl("rtmp://" + rtmpBase)
                .httpFlvPlayUrl("http://" + httpBase + ".flv")
                .hlsPlayUrl(Boolean.TRUE.equals(srsProperties.getHlsEnabled())
                        ? "http://" + httpBase + ".m3u8" : null)
                .build();
    }

    private LiveRoomVO toVO(LiveRoom room) {
        return LiveRoomVO.builder()
                .id(room.getId())
                .userId(room.getUserId())
                .streamName(room.getStreamName())
                .status(room.getStatus())
                .build();
    }

    private String generateStreamName() {
        return "s" + UUID.randomUUID().toString().replace("-", "");
    }
}
