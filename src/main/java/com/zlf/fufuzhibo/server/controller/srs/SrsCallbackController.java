package com.zlf.fufuzhibo.server.controller.srs;

import com.zlf.fufuzhibo.common.properties.SrsProperties;
import com.zlf.fufuzhibo.pojo.dto.SrsCallbackDTO;
import com.zlf.fufuzhibo.server.service.LiveStreamService;
import com.zlf.fufuzhibo.server.service.ViewerCountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "SRS 回调接口", description = "供 SRS 服务端调用的内部回调，非前端业务接口")
@Slf4j
@RestController
@RequestMapping("/srs")
@RequiredArgsConstructor
public class SrsCallbackController {

    private final LiveStreamService liveStreamService;
    private final ViewerCountService viewerCountService;
    private final SrsProperties srsProperties;

    /**
     * SRS 推流开始回调：OBS 点开始直播、推流连接成功后触发
     * 返回约定：0 = 允许推流；非 0 = 拒绝推流（业务鉴权用）
     */
    @Operation(summary = "SRS 推流开始回调",
            description = "OBS 建立推流连接时由 SRS 触发，校验回调密钥、推流 token 与场次状态；返回 code=0 允许推流，非 0 拒绝推流。")
    @PostMapping("/on_publish")
    public Map<String, Object> onPublish(
            @RequestParam(value = "secret", required = false) String secret,
            @RequestBody SrsCallbackDTO callback) {
        log.info("========== SRS 推流回调 on_publish ==========");
        log.info("回调参数: app={}, stream={}, clientId={}, ip={}",
                callback.getApp(), callback.getStream(), callback.getClientId(), callback.getIp());

        if (!secretValid(secret)) {
            return fail();
        }

        try {
            liveStreamService.onPublish(callback);
            return ok();
        } catch (Exception e) {
            log.warn("on_publish 拒绝推流: stream={}, clientId={}, reason={}",
                    callback.getStream(), callback.getClientId(), e.getMessage());
            return fail();
        }
    }

    /**
     * SRS 推流结束回调：OBS 停止直播、连接断开后触发
     */
    @Operation(summary = "SRS 推流结束回调",
            description = "推流断开时由 SRS 触发，结束对应直播场次并释放推流锁；异常仅记录日志，不阻塞 SRS。")
    @PostMapping("/on_unpublish")
    public Map<String, Object> onUnpublish(
            @RequestParam(value = "secret", required = false) String secret,
            @RequestBody SrsCallbackDTO callback) {
        log.info("========== SRS 断流回调 on_unpublish ==========");
        log.info("回调参数: app={}, stream={}, clientId={}, ip={}",
                callback.getApp(), callback.getStream(), callback.getClientId(), callback.getIp());

        if (!secretValid(secret)) {
            return fail();
        }

        try {
            liveStreamService.onUnpublish(callback);
        } catch (Exception e) {
            // 断流回调不阻塞 SRS，异常只记录日志
            log.error("on_unpublish 处理失败: clientId={}", callback.getClientId(), e);
        }
        return ok();
    }

    /**
     * SRS 播放开始回调：观众开始拉流时触发，在线人数 +1。
     * 该回调是同步阻塞的，处理必须极快，且任何情况下都返回 0 放行。
     */
    @Operation(summary = "SRS 播放开始回调",
            description = "观众开始拉流时由 SRS 触发，在线人数加一；处理异常不影响播放，始终返回 code=0。")
    @PostMapping("/on_play")
    public Map<String, Object> onPlay(
            @RequestParam(value = "secret", required = false) String secret,
            @RequestBody SrsCallbackDTO callback) {
        if (!secretValid(secret)) {
            return fail();
        }
        log.debug("SRS 播放回调 on_play: stream={}, clientId={}",
                callback.getStream(), callback.getClientId());
        viewerCountService.onPlay(callback.getStream());
        return ok();
    }

    /**
     * SRS 播放结束回调：观众断开拉流时触发，在线人数 -1
     */
    @Operation(summary = "SRS 播放结束回调",
            description = "观众停止拉流时由 SRS 触发，在线人数减一（最低为 0）；异常仅记录日志，不阻塞 SRS。")
    @PostMapping("/on_stop")
    public Map<String, Object> onStop(
            @RequestParam(value = "secret", required = false) String secret,
            @RequestBody SrsCallbackDTO callback) {
        if (!secretValid(secret)) {
            return fail();
        }
        log.debug("SRS 播放回调 on_stop: stream={}, clientId={}",
                callback.getStream(), callback.getClientId());
        viewerCountService.onStop(callback.getStream());
        return ok();
    }

    private boolean secretValid(String secret) {
        String expected = srsProperties.getCallbackSecret();

        boolean secretReceived = secret != null && !secret.isBlank();
        boolean expectedConfigured = expected != null && !expected.isBlank();
        if (!secretReceived || !expectedConfigured) {
            log.warn("回调密钥校验失败: secretReceived={}, expectedConfigured={}",
                    secretReceived, expectedConfigured);
            return false;
        }

        boolean matched = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                secret.getBytes(StandardCharsets.UTF_8));
        if (!matched) {
            log.warn("回调密钥不一致: receivedPrefix={}, receivedLength={}, expectedPrefix={}, expectedLength={}",
                    mask(secret), secret.length(), mask(expected), expected.length());
        }
        return matched;
    }

    private String mask(String value) {
        return value.substring(0, Math.min(3, value.length())) + "***";
    }

    private Map<String, Object> ok() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        return resp;
    }

    private Map<String, Object> fail() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 1);
        return resp;
    }
}
