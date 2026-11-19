package com.zlf.fufuzhibo.server.task;

import com.zlf.fufuzhibo.server.service.LiveStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 扫描断流宽限期已到期的场次并落库结束。
 * 宽限期内 OBS 重连会清除待结束标记，从而实现无缝续播。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveSessionEndTask {

    private final LiveStreamService liveStreamService;

    @Scheduled(fixedDelayString = "${fufu.push.end-scan-interval-ms:10000}")
    public void finalizeExpiredSessions() {
        try {
            int closed = liveStreamService.finalizePendingEnds();
            if (closed > 0) {
                log.info("断流宽限期到期，结束场次数量={}", closed);
            }
        } catch (Exception e) {
            log.error("处理断流宽限期场次失败", e);
        }
    }

    /**
     * 定期作废申请开播后长时间未推流的场次
     */
    @Scheduled(fixedDelayString = "${fufu.push.pending-scan-interval-ms:60000}")
    public void cancelStalePendingSessions() {
        try {
            int cancelled = liveStreamService.cancelStalePendingSessions();
            if (cancelled > 0) {
                log.info("待开播场次超时作废数量={}", cancelled);
            }
        } catch (Exception e) {
            log.error("处理待开播超时场次失败", e);
        }
    }
}
