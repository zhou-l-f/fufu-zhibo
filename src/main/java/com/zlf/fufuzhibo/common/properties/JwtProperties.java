package com.zlf.fufuzhibo.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("fufu.jwt")
@Data
public class JwtProperties {
    // 管理员端jwt
    private String adminSecretKey;
    private Long adminExpireTime;
    private String adminTokenName;

    // 用户端jwt
    private String userSecretKey;
    private Long userExpireTime;
    private String userTokenName;
}
