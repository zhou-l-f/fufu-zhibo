package com.zlf.fufuzhibo.server.config;

import com.zlf.fufuzhibo.server.websocket.LiveChatHandshakeInterceptor;
import com.zlf.fufuzhibo.server.websocket.LiveChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * 直播间聊天 WebSocket 注册，握手阶段由拦截器校验 token
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final LiveChatWebSocketHandler liveChatWebSocketHandler;
    private final LiveChatHandshakeInterceptor liveChatHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(liveChatWebSocketHandler, "/ws/live/{roomId}")
                .addInterceptors(liveChatHandshakeInterceptor)
                .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*");
    }

    /**
     * 容器级空闲超时兜底：连接长时间无任何帧交互时由 WebSocket 容器关闭，
     * 配合应用层心跳剔除半开连接
     */
    @Bean
    public ServletServerContainerFactoryBean webSocketContainer(
            @Value("${fufu.chat.idle-timeout-ms:120000}") long idleTimeoutMs) {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxSessionIdleTimeout(idleTimeoutMs + 60_000L);
        container.setMaxTextMessageBufferSize(16 * 1024);
        return container;
    }
}
