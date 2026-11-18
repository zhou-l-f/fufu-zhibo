package com.zlf.fufuzhibo.server.task;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.zlf.fufuzhibo.common.properties.SrsProperties;
import com.zlf.fufuzhibo.server.service.LiveStreamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * 定时与 SRS 在线流对账，回收 on_unpublish 丢失导致的僵尸场次
 */
@Slf4j
@Component
public class LiveStreamReconcileTask {

    private final SrsProperties srsProperties;
    private final LiveStreamService liveStreamService;
    private final JsonMapper jsonMapper;
    private final RestTemplate restTemplate;

    public LiveStreamReconcileTask(SrsProperties srsProperties,
                                   LiveStreamService liveStreamService,
                                   JsonMapper jsonMapper) {
        this.srsProperties = srsProperties;
        this.liveStreamService = liveStreamService;
        this.jsonMapper = jsonMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restTemplate = new RestTemplate(factory);
    }

    @Scheduled(fixedDelayString = "${fufu.srs.reconcile-interval-ms:60000}")
    public void reconcile() {
        String apiHost = srsProperties.getApiHost();
        Integer apiPort = srsProperties.getApiPort();
        if (apiHost == null || apiHost.isBlank() || apiPort == null) {
            return;
        }

        String url = "http://" + apiHost + ":" + apiPort + "/api/v1/streams";
        try {
            String body = restTemplate.getForObject(url, String.class);
            if (body == null || body.isBlank()) {
                log.warn("SRS 对账跳过，接口返回空响应");
                return;
            }
            JsonNode root = jsonMapper.readTree(body);
            if (root.path("code").asInt(-1) != 0) {
                log.warn("SRS 对账跳过，接口返回异常: code={}", root.path("code").asInt(-1));
                return;
            }

            Set<String> activeStreams = new HashSet<>();
            for (JsonNode stream : root.path("streams")) {
                activeStreams.add(stream.path("app").asString() + "/" + stream.path("name").asString());
            }

            int closed = liveStreamService.reconcile(activeStreams);
            if (closed > 0) {
                log.info("SRS 对账完成: 在线流={}, 回收僵尸场次={}", activeStreams.size(), closed);
            }
        } catch (Exception e) {
            // SRS 不可达时不能误伤正在直播的场次，直接跳过本轮
            log.warn("SRS 对账失败，跳过本轮: {}", e.getMessage());
        }
    }
}
