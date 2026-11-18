package com.zlf.fufuzhibo.server.websocket;

import com.zlf.fufuzhibo.common.constant.JwtClaimsConstants;
import com.zlf.fufuzhibo.common.properties.JwtProperties;
import com.zlf.fufuzhibo.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * 握手鉴权：从查询参数取用户 token 校验，校验失败直接返回 401
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveChatHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_ROOM_ID = "roomId";
    public static final String ATTR_USER_ID = "userId";

    private final JwtProperties jwtProperties;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        Long roomId = parseRoomId(request.getURI().getPath());
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");

        if (roomId == null || token == null || token.isBlank()) {
            reject(response);
            return false;
        }

        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            Object userId = claims.get(JwtClaimsConstants.USER_ID);
            if (userId == null) {
                reject(response);
                return false;
            }
            attributes.put(ATTR_ROOM_ID, roomId);
            attributes.put(ATTR_USER_ID, Long.valueOf(userId.toString()));
            return true;
        } catch (Exception e) {
            log.warn("聊天握手鉴权失败: path={}, error={}",
                    request.getURI().getPath(), e.getMessage());
            reject(response);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // 无需处理
    }

    private void reject(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    /**
     * 解析 /ws/live/{roomId} 中的 roomId
     */
    private Long parseRoomId(String path) {
        if (path == null) {
            return null;
        }
        int index = path.lastIndexOf('/');
        if (index < 0 || index == path.length() - 1) {
            return null;
        }
        try {
            long roomId = Long.parseLong(path.substring(index + 1));
            return roomId > 0 ? roomId : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
