package com.zlf.fufuzhibo.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * SRS 调用专用 RestTemplate：复用同一实例，避免每次踢流/对账都新建
 */
@Configuration
public class SrsRestTemplateConfig {

    /**
     * 踢流接口：连接 2 秒、读取 3 秒，作为默认注入，结束直播时不会长时间等待
     */
    @Bean
    @Primary
    public RestTemplate srsKickRestTemplate() {
        return build(Duration.ofSeconds(2), Duration.ofSeconds(3));
    }

    /**
     * 对账查询接口：连接 3 秒、读取 5 秒
     */
    @Bean("srsApiRestTemplate")
    public RestTemplate srsApiRestTemplate() {
        return build(Duration.ofSeconds(3), Duration.ofSeconds(5));
    }

    private RestTemplate build(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }
}
