package com.zlf.fufuzhibo.server.config;

import com.zlf.fufuzhibo.server.event.LiveStatusEventPublisher;
import com.zlf.fufuzhibo.server.event.LiveStatusEventSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 直播状态广播的 Redis 订阅容器
 */
@Configuration
@RequiredArgsConstructor
public class LiveStatusRedisConfig {

    private final RedisConnectionFactory redisConnectionFactory;
    private final LiveStatusEventSubscriber liveStatusEventSubscriber;

    @Bean
    public RedisMessageListenerContainer liveStatusListenerContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(liveStatusEventSubscriber,
                new ChannelTopic(LiveStatusEventPublisher.CHANNEL));
        return container;
    }
}
