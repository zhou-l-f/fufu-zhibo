package com.zlf.fufuzhibo.common.utils;

import com.zlf.fufuzhibo.common.constant.MessageConstant;
import com.zlf.fufuzhibo.common.exception.BaseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

/**
 * 推流令牌工具
 * <p>
 * 令牌格式：userId.sessionId.expireAt.signature
 * 签名内容：userId:streamName:sessionId:expireAt（HMAC-SHA256）
 * 令牌只用于本次推流鉴权，有效期短，不携带登录态
 */
public class PushTokenUtil {

    private static final String SEPARATOR = ".";
    private static final String SEPARATOR_REGEX = "\\.";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private PushTokenUtil() {
    }

    /**
     * 令牌解析结果
     */
    public record Payload(Long userId, Long sessionId) {
    }

    /**
     * 生成推流令牌
     *
     * @param secret     签名密钥
     * @param userId     主播用户 id
     * @param sessionId  直播场次 id
     * @param streamName 推流流名
     * @param ttlSeconds 有效期（秒）
     * @return 推流令牌
     */
    public static String create(String secret, Long userId, Long sessionId,
                                String streamName, long ttlSeconds) {
        long expireAt = Instant.now().getEpochSecond() + ttlSeconds;
        return userId + SEPARATOR + sessionId + SEPARATOR + expireAt + SEPARATOR
                + sign(secret, userId, streamName, sessionId, expireAt);
    }

    /**
     * 校验推流令牌
     *
     * @param secret     签名密钥
     * @param token      推流令牌
     * @param streamName 推流流名，必须与签发时一致
     * @return 令牌中的用户 id 与场次 id
     * @throws BaseException 令牌缺失、格式错误、签名不匹配或已过期
     */
    public static Payload verify(String secret, String token, String streamName) {
        if (token == null || token.isBlank() || streamName == null || streamName.isBlank()) {
            throw new BaseException(MessageConstant.PUSH_TOKEN_INVALID);
        }

        String[] parts = token.split(SEPARATOR_REGEX);
        if (parts.length != 4) {
            throw new BaseException(MessageConstant.PUSH_TOKEN_INVALID);
        }

        Long userId;
        Long sessionId;
        long expireAt;
        try {
            userId = Long.valueOf(parts[0]);
            sessionId = Long.valueOf(parts[1]);
            expireAt = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            throw new BaseException(MessageConstant.PUSH_TOKEN_INVALID);
        }

        if (expireAt <= Instant.now().getEpochSecond()) {
            throw new BaseException(MessageConstant.PUSH_TOKEN_EXPIRED);
        }

        String expected = sign(secret, userId, streamName, sessionId, expireAt);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                parts[3].getBytes(StandardCharsets.UTF_8))) {
            throw new BaseException(MessageConstant.PUSH_TOKEN_INVALID);
        }

        return new Payload(userId, sessionId);
    }

    private static String sign(String secret, Long userId, String streamName,
                               Long sessionId, long expireAt) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            String content = userId + ":" + streamName + ":" + sessionId + ":" + expireAt;
            return HexFormat.of().formatHex(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("推流令牌签名失败", e);
        }
    }
}
