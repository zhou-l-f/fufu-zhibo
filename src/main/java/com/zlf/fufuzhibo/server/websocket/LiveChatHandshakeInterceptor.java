package com.zlf.fufuzhibo.server.websocket;

import com.zlf.fufuzhibo.common.constant.JwtClaimsConstants;
import com.zlf.fufuzhibo.common.constant.LiveStatusConstant;
import com.zlf.fufuzhibo.common.properties.JwtProperties;
import com.zlf.fufuzhibo.common.utils.JwtUtil;
import com.zlf.fufuzhibo.pojo.entity.LiveRoom;
import com.zlf.fufuzhibo.pojo.entity.LiveSession;
import com.zlf.fufuzhibo.server.mapper.LiveRoomMapper;
import com.zlf.fufuzhibo.server.mapper.LiveSessionMapper;
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
 * 握手鉴权：校验用户 token 与场次归属，校验失败直接返回 401。
 * 聊天以直播场次为维度，前端必须传 sessionId，避免同一主播多场直播的聊天串场。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveChatHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_ROOM_ID = "roomId";
    public static final String ATTR_SESSION_ID = "sessionId";
    public static final String ATTR_USER_ID = "userId";

    private final JwtProperties jwtProperties;
    private final LiveSessionMapper liveSessionMapper;
    private final LiveRoomMapper liveRoomMapper;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        Long roomId = parseRoomId(request.getURI().getPath());
        String token = queryParam(request, "token");
        Long sessionId = parseLong(queryParam(request, "sessionId"));

        if (roomId == null || sessionId == null || token == null || token.isBlank()) {
            reject(response);
            return false;
        }

        // 场次必须属于该直播间，且尚未彻底结束（待开播与直播中都可连接）
        LiveSession session = liveSessionMapper.selectById(sessionId);
        if (session == null
                || !roomId.equals(session.getRoomId())
                || LiveStatusConstant.SESSION_ENDED.equals(session.getStatus())) {
            log.warn("聊天握手场次校验失败: roomId={}, sessionId={}", roomId, sessionId);
            reject(response);
            return false;
        }

        // 封禁中的直播间不允许建立聊天连接
        LiveRoom room = liveRoomMapper.selectById(roomId);
        if (room == null || LiveStatusConstant.ROOM_BANNED.equals(room.getStatus())) {
            log.warn("聊天握手直播间已封禁: roomId={}, sessionId={}", roomId, sessionId);
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
            attributes.put(ATTR_SESSION_ID, sessionId);
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

    private String queryParam(ServerHttpRequest request, String name) {
        return UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst(name);
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
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
        return parseLong(path.substring(index + 1));
    }
}
