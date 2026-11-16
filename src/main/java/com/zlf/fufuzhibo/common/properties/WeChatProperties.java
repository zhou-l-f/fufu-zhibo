package com.zlf.fufuzhibo.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("fufu.wechat")
@Data
public class WeChatProperties {
    private String appid;
    private String secret;
}
