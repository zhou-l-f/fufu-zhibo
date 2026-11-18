package com.zlf.fufuzhibo.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 直播间聊天配置
 */
@Component
@ConfigurationProperties("fufu.chat")
@Data
public class ChatProperties {

    /**
     * Redis 中每个直播间保留的最近消息条数
     */
    private Integer recentSize = 100;

    /**
     * 连接建立时下发的历史消息条数
     */
    private Integer initSize = 50;

    /**
     * 同一用户在同一直播间的发言间隔（秒）
     */
    private Integer rateLimitSeconds = 2;

    /**
     * 单条消息最大字符数
     */
    private Integer maxLength = 100;

    /**
     * 最近消息缓存过期时间（小时）
     */
    private Integer cacheHours = 6;

    /**
     * 敏感词，命中后按字符数替换为 *
     */
    private List<String> sensitiveWords = new ArrayList<>();
}
