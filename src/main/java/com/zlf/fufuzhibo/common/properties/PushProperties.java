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

    /**
     * 断流后延迟结束场次的宽限期（秒），期间 OBS 重连可无缝续播
     */
    private Integer endGraceSeconds;

    /**
     * 待开播场次自动作废时长（分钟），超过该时长仍未推流则取消场次
     */
    private Integer pendingExpireMinutes = 30;
}
