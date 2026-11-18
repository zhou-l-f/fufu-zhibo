package com.zlf.fufuzhibo.server.task;

import com.zlf.fufuzhibo.server.service.AdminLiveRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时解封：封禁截止时间已到的直播间自动恢复为未开播
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveRoomUnbanTask {

    private final AdminLiveRoomService adminLiveRoomService;

    @Scheduled(fixedDelayString = "${fufu.admin.unban-scan-interval-ms:60000}")
    public void unbanExpiredRooms() {
        try {
            int count = adminLiveRoomService.unbanExpired();
            if (count > 0) {
                log.info("封禁到期自动解封直播间数量={}", count);
            }
        } catch (Exception e) {
            log.error("封禁到期自动解封失败", e);
        }
    }
}
