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

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");

        // 没有 Bearer Token，直接放行，最终由授权规则决定是否拒绝
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (uri.startsWith(ADMIN_PATH_PREFIX)) {
                // 管理员端：用管理员密钥解析，身份放进 ROLE_ADMIN
                Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
                Object adminId = claims.get(JwtClaimsConstants.ADMIN_ID);
                if (adminId != null
                        && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    adminId,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                            );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } else if (uri.startsWith(USER_PATH_PREFIX)) {
                // 用户端：用用户密钥解析，身份放进 ROLE_USER
                Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
                Object userId = claims.get(JwtClaimsConstants.USER_ID);
                if (userId != null
                        && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userId,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
                            );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
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
}