package com.zlf.fufuzhibo.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SRS 相关配置
 */
@Component
@ConfigurationProperties("fufu.srs")
@Data
public class SrsProperties {

    /**
     * SRS 回调地址上的密钥，用于校验回调来源
     */
    private String callbackSecret;

    /**
     * SRS 服务地址（推流/拉流使用）
     */
    private String rtmpHost;

    /**
     * RTMP 端口
     */
    private Integer rtmpPort;

    /**
     * HTTP 端口（HTTP-FLV / HLS）
     */
    private Integer httpPort;

    /**
     * SRS HTTP API 地址，用于定时对账
     */
    private String apiHost;

    /**
     * SRS HTTP API 端口
     */
    private Integer apiPort;

    /**
     * 是否开启 HLS，未开启时不返回 HLS 地址
     */
    private Boolean hlsEnabled = false;

    /**
     * SRS 应用名
     */
    private String app;
}
