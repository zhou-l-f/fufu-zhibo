package com.zlf.fufuzhibo.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 推流令牌配置
 */
@Component
@ConfigurationProperties("fufu.push")
@Data
public class PushProperties {

    /**
     * 推流令牌签名密钥
     */
    private String secretKey;

    /**
     * 推流令牌有效期（秒）
     */
    private Long expireTime;

    /**
     * 断流后允许同一场次重连的宽限期（秒），覆盖 OBS 自动重连
     */
    private Integer reconnectGraceSeconds;
}
