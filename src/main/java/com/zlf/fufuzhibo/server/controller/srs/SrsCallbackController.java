package com.zlf.fufuzhibo.server.controller.srs;

import com.zlf.fufuzhibo.common.properties.SrsProperties;
import com.zlf.fufuzhibo.pojo.dto.SrsCallbackDTO;
import com.zlf.fufuzhibo.server.service.LiveStreamService;
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

@Slf4j
@RestController
@RequestMapping("/srs")
@RequiredArgsConstructor
public class SrsCallbackController {

    private final LiveStreamService liveStreamService;
    private final SrsProperties srsProperties;

    /**
     * SRS 推流开始回调：OBS 点开始直播、推流连接成功后触发
     * 返回约定：0 = 允许推流；非 0 = 拒绝推流（业务鉴权用）
     */
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
