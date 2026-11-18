package com.zlf.fufuzhibo.server.config;

import com.zlf.fufuzhibo.server.websocket.LiveChatHandshakeInterceptor;
import com.zlf.fufuzhibo.server.websocket.LiveChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

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
}
