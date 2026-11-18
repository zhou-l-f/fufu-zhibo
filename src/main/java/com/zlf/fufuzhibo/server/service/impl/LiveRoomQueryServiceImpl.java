package com.zlf.fufuzhibo.server.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zlf.fufuzhibo.common.constant.ErrorCode;
import com.zlf.fufuzhibo.common.exception.BaseException;
import com.zlf.fufuzhibo.common.properties.SrsProperties;
import com.zlf.fufuzhibo.pojo.vo.LiveRoomDetailVO;
import com.zlf.fufuzhibo.pojo.vo.LiveRoomItemVO;
import com.zlf.fufuzhibo.pojo.vo.PageResult;
import com.zlf.fufuzhibo.server.mapper.LiveRoomMapper;
import com.zlf.fufuzhibo.server.service.LiveRoomQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 直播间浏览：列表与详情均只返回正在直播的直播间
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveRoomQueryServiceImpl implements LiveRoomQueryService {

    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;

    /**
     * 在线观看人数计数键：live:viewers:{streamName}
     */
    private static final String VIEWER_KEY_PREFIX = "live:viewers:";

    private final LiveRoomMapper liveRoomMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final SrsProperties srsProperties;

    @Override
    public PageResult<LiveRoomItemVO> listLiveRooms(int page, int size) {
        // 防御式修正：即使上层校验被绕过也不会产生非法分页
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        // 分页插件自动执行 COUNT 并拼接 LIMIT，页码超出总页数时返回空列表
        IPage<LiveRoomItemVO> result = liveRoomMapper.selectLiveRoomPage(
                new Page<>(safePage, safeSize));
        List<LiveRoomItemVO> records = result.getRecords() == null
                ? List.of()
                : result.getRecords();
        fillViewerCounts(records);

        return PageResult.<LiveRoomItemVO>builder()
                .page(result.getCurrent())
                .size(result.getSize())
                .total(result.getTotal())
                .pages(result.getPages())
                .records(records)
                .build();
    }

    @Override
    public LiveRoomDetailVO getLiveRoom(Long roomId) {
        LiveRoomDetailVO detail = liveRoomMapper.selectLiveRoomDetail(roomId);
        if (detail == null) {
            // 列表数据可能来自前端缓存或已下播，统一返回业务错误码
            throw new BaseException(ErrorCode.LIVE_ROOM_NOT_FOUND);
        }

        detail.setViewerCount(readViewerCount(detail.getStreamName()));
        detail.setHttpFlvPlayUrl(buildHttpFlvUrl(detail.getStreamName()));
        detail.setHlsPlayUrl(buildHlsUrl(detail.getStreamName()));

        log.info("查询直播间详情: roomId={}, sessionId={}", roomId, detail.getSessionId());
        return detail;
    }

    /**
     * 批量读取在线人数，Redis 异常时整体降级为 0，不影响列表返回
     */
    private void fillViewerCounts(List<LiveRoomItemVO> records) {
        if (records.isEmpty()) {
            return;
        }
        try {
            List<String> keys = records.stream()
                    .map(item -> VIEWER_KEY_PREFIX + item.getStreamName())
                    .toList();
            List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
            for (int i = 0; i < records.size(); i++) {
                String value = values == null || values.size() <= i ? null : values.get(i);
                records.get(i).setViewerCount(parseCount(value));
            }
        } catch (Exception e) {
            log.warn("批量读取在线人数失败，降级为 0: {}", e.getMessage());
            records.forEach(item -> item.setViewerCount(0));
        }
    }

    private int readViewerCount(String streamName) {
        try {
            return parseCount(stringRedisTemplate.opsForValue().get(VIEWER_KEY_PREFIX + streamName));
        } catch (Exception e) {
            log.warn("读取在线人数失败，降级为 0: streamName={}, error={}", streamName, e.getMessage());
            return 0;
        }
    }

    private int parseCount(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Math.max(Integer.parseInt(value.trim()), 0);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String buildHttpFlvUrl(String streamName) {
        String host = srsProperties.getRtmpHost();
        Integer httpPort = srsProperties.getHttpPort();
        String app = srsProperties.getApp();
        if (host == null || httpPort == null || app == null) {
            return null;
        }
        return String.format("http://%s:%d/%s/%s.flv", host, httpPort, app, streamName);
    }

    private String buildHlsUrl(String streamName) {
        if (!Boolean.TRUE.equals(srsProperties.getHlsEnabled())) {
            return null;
        }
        String host = srsProperties.getRtmpHost();
        Integer httpPort = srsProperties.getHttpPort();
        String app = srsProperties.getApp();
        if (host == null || httpPort == null || app == null) {
            return null;
        }
        return String.format("http://%s:%d/%s/%s.m3u8", host, httpPort, app, streamName);
    }
}
