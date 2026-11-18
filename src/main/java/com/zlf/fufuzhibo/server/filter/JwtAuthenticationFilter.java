package com.zlf.fufuzhibo.server.filter;

import com.zlf.fufuzhibo.common.constant.JwtClaimsConstants;
import com.zlf.fufuzhibo.common.properties.JwtProperties;
import com.zlf.fufuzhibo.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProperties jwtProperties;

    private static final String ADMIN_PATH_PREFIX = "/admin";
    private static final String USER_PATH_PREFIX = "/user";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        try {
            if (uri.startsWith(ADMIN_PATH_PREFIX)) {
                // 管理员端：优先读配置的 token 头，其次兼容 Authorization: Bearer
                String token = resolveToken(request, jwtProperties.getAdminTokenName());
                if (token != null
                        && SecurityContextHolder.getContext().getAuthentication() == null) {
                    Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
                    Object adminId = claims.get(JwtClaimsConstants.ADMIN_ID);
                    if (adminId != null) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        adminId,
                                        null,
                                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } else if (uri.startsWith(USER_PATH_PREFIX)) {
                // 用户端：优先读配置的 token 头，其次兼容 Authorization: Bearer
                String token = resolveToken(request, jwtProperties.getUserTokenName());
                if (token != null
                        && SecurityContextHolder.getContext().getAuthentication() == null) {
                    Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
                    Object userId = claims.get(JwtClaimsConstants.USER_ID);
                    if (userId != null) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userId,
                                        null,
                                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                                );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
            // 其他路径（如 /doc.html、/actuator/**）不解析 token，直接放行
        } catch (Exception e) {
            // token 过期、签名不匹配、用错密钥、格式错误等，统一放行，
            // 由后续的 hasRole 规则拒绝未认证的请求
            log.warn("JWT 解析失败, uri={}, error={}", uri, e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 取 token：优先使用配置的请求头名（如 token），值为裸 token 或 Bearer token 均可；
     * 未配置或未携带时，兼容 Authorization: Bearer
     */
    private String resolveToken(HttpServletRequest request, String headerName) {
        if (headerName != null && !headerName.isBlank()) {
            String value = request.getHeader(headerName);
            String token = stripBearer(value);
            if (token != null) {
                return token;
            }
        }

        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    private String stripBearer(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String token = value.trim();
        if (token.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            token = token.substring(BEARER_PREFIX.length()).trim();
        }
        return token.isEmpty() ? null : token;
    }
}
