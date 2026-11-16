package com.zlf.fufuzhibo.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public class JwtUtil {

    /**
     * 生成 JWT（0.12.6 适配版）
     * 使用 HS256 算法，密钥由传入的 secretKey 字符串经 Keys.hmacShaKeyFor 转换为 SecretKey
     */
    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        long expMillis = System.currentTimeMillis() + ttlMillis;
        Date exp = new Date(expMillis);

        JwtBuilder builder = Jwts.builder()
                .claims(claims)          // 0.12.x 推荐用 claims() 设置自定义声明
                .expiration(exp)         // setExpiration 已废弃，改用 expiration()
                .signWith(key);          // 不再传 SignatureAlgorithm，JJWT 根据密钥长度自动选择 HS256

        return builder.compact();
    }

    /**
     * 解析并验证 JWT（0.12.6 适配版）
     * 密钥验证使用 verifyWith，解析结果用 parseSignedClaims，载荷用 getPayload 获取
     */
    public static Claims parseJWT(String secretKey, String token) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)              // 替代旧的 setSigningKey
                .build()
                .parseSignedClaims(token)     // 替代 parseClaimsJws
                .getPayload();                // 替代 getBody
    }
}