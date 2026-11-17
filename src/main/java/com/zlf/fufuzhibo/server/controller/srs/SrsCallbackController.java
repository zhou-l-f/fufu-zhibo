package com.zlf.fufuzhibo.server.controller.srs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/srs")
public class SrsCallbackController {

    /**
     * SRS 推流开始回调：OBS 点开始直播、推流连接成功后触发
     * 返回约定：0 = 允许推流；非 0 = 拒绝推流（业务鉴权用）
     */
    @PostMapping("/on_publish")
    public Map<String,Object> onPublish(@RequestBody Map<String, Object> body) {
        log.info("========== SRS 推流回调 on_publish ==========");
        log.info("回调报文: {}", body);
        // 阶段二：直接放行，后续阶段在这里校验鉴权、创建直播间记录
        // 返回给SRS必须是 {"code":0}
        Map<String,Object> resp = new HashMap<>();
        resp.put("code", 0);
        return resp;
    }

    /**
     * SRS 推流结束回调：OBS 停止直播、连接断开后触发
     */
    @PostMapping("/on_unpublish")
    public Map<String,Object> onUnpublish(@RequestBody Map<String, Object> body) {
        log.info("========== SRS 断流回调 on_unpublish ==========");
        log.info("回调报文: {}", body);

        Map<String,Object> resp = new HashMap<>();
        resp.put("code", 0);
        return resp;
    }
}
