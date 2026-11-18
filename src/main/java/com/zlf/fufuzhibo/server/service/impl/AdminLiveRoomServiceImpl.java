package com.zlf.fufuzhibo.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zlf.fufuzhibo.common.constant.LiveStatusConstant;
import com.zlf.fufuzhibo.common.constant.MessageConstant;
import com.zlf.fufuzhibo.common.exception.BaseException;
import com.zlf.fufuzhibo.pojo.entity.LiveRoom;
import com.zlf.fufuzhibo.pojo.vo.AdminLiveRoomVO;
import com.zlf.fufuzhibo.pojo.vo.PageResult;
import com.zlf.fufuzhibo.server.mapper.LiveRoomMapper;
import com.zlf.fufuzhibo.server.service.AdminLiveRoomService;
import com.zlf.fufuzhibo.server.service.LiveStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 管理端直播房间业务：列表查询、封禁与解封
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLiveRoomServiceImpl implements AdminLiveRoomService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final LiveRoomMapper liveRoomMapper;
    private final LiveStreamService liveStreamService;

    @Override
    public PageResult<AdminLiveRoomVO> listRooms(int page, int size, Integer status, String keyword) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        String trimmedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();

        IPage<AdminLiveRoomVO> result = liveRoomMapper.selectAdminLiveRoomPage(
                new Page<>(safePage, safeSize), status, trimmedKeyword);
        List<AdminLiveRoomVO> records = result.getRecords() == null ? List.of() : result.getRecords();

        return PageResult.<AdminLiveRoomVO>builder()
                .page(result.getCurrent())
                .size(result.getSize())
                .total(result.getTotal())
                .pages(result.getPages())
                .records(records)
                .build();
    }

    @Override
    @Transactional
    public void ban(Long userId, LocalDateTime banTime) {
        LocalDateTime now = LocalDateTime.now(ZONE);
        if (banTime == null || !banTime.isAfter(now)) {
            throw new BaseException(MessageConstant.BAN_TIME_INVALID);
        }

        LiveRoom room = liveRoomMapper.selectOne(
                Wrappers.<LiveRoom>lambdaQuery().eq(LiveRoom::getUserId, userId));
        if (room == null) {
            throw new BaseException(MessageConstant.PUSH_ROOM_NOT_FOUND);
        }

        // 先结束当前场次并断开 SRS 推流，避免封禁后主播仍在直播
        liveStreamService.endCurrentLive(userId);

        LiveRoom update = new LiveRoom();
        update.setId(room.getId());
        update.setStatus(LiveStatusConstant.ROOM_BANNED);
        update.setBanTime(banTime);
        liveRoomMapper.updateById(update);

        log.info("封禁直播间: userId={}, roomId={}, banTime={}", userId, room.getId(), banTime);
    }

    @Override
    public void unban(Long userId) {
        LiveRoom room = liveRoomMapper.selectOne(
                Wrappers.<LiveRoom>lambdaQuery().eq(LiveRoom::getUserId, userId));
        if (room == null) {
            throw new BaseException(MessageConstant.PUSH_ROOM_NOT_FOUND);
        }

        LambdaUpdateWrapper<LiveRoom> update = Wrappers.<LiveRoom>lambdaUpdate()
                .eq(LiveRoom::getId, room.getId())
                .set(LiveRoom::getStatus, LiveStatusConstant.ROOM_NOT_STARTED)
                .set(LiveRoom::getBanTime, null);
        liveRoomMapper.update(null, update);

        log.info("解封直播间: userId={}, roomId={}", userId, room.getId());
    }

    @Override
    public int unbanExpired() {
        LambdaUpdateWrapper<LiveRoom> update = Wrappers.<LiveRoom>lambdaUpdate()
                .eq(LiveRoom::getStatus, LiveStatusConstant.ROOM_BANNED)
                .isNotNull(LiveRoom::getBanTime)
                .le(LiveRoom::getBanTime, LocalDateTime.now(ZONE))
                .set(LiveRoom::getStatus, LiveStatusConstant.ROOM_NOT_STARTED)
                .set(LiveRoom::getBanTime, null);
        return liveRoomMapper.update(null, update);
    }
}
