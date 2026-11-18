package com.zlf.fufuzhibo.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置
 */
@Component
@ConfigurationProperties("fufu.alioss")
@Data
public class AliOssProperties {

    /**
     * 访问密钥 id
     */
    private String accessKeyId;

    /**
     * 访问密钥 secret
     */
    private String accessKeySecret;

    /**
     * Bucket 名称
     */
    private String bucketName;

    /**
     * OSS 接入点，例如 oss-cn-hangzhou.aliyuncs.com
     */
    private String endpoint;
}
