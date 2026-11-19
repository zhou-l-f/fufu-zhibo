package com.zlf.fufuzhibo.server.service;

import com.zlf.fufuzhibo.common.constant.MessageConstant;
import com.zlf.fufuzhibo.common.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 登录失败次数限制：同一账号连续失败达到阈值后锁定一段时间。
 * Redis 不可用时放行，避免频控故障导致所有人无法登录。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginAttemptLimiter {

    private static final String KEY_PREFIX = "login:fail:";
    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_TTL = Duration.ofMinutes(15);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 登录前校验是否已被锁定
     *
     * @param scope    用户端 user / 管理端 admin
     * @param username 登录账号
     */
    public void check(String scope, String username) {
        try {
            String value = stringRedisTemplate.opsForValue().get(key(scope, username));
            if (value != null && Long.parseLong(value) >= MAX_FAILURES) {
                throw new BaseException(MessageConstant.LOGIN_ATTEMPTS_EXCEEDED);
            }
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.warn("登录失败次数校验异常，本次放行: scope={}, error={}", scope, e.getMessage());
        }
    }

    /**
     * 记录一次登录失败，首次失败时设置锁定窗口
     */
    public void recordFailure(String scope, String username) {
        try {
            String key = key(scope, username);
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(key, LOCK_TTL);
            }
        } catch (Exception e) {
            log.warn("登录失败次数写入异常: scope={}, error={}", scope, e.getMessage());
        }
    }

    /**
     * 登录成功后清除失败计数
     */
    public void clear(String scope, String username) {
        try {
            stringRedisTemplate.delete(key(scope, username));
        } catch (Exception e) {
            log.warn("登录失败次数清理异常: scope={}, error={}", scope, e.getMessage());
        }
    }

    private String key(String scope, String username) {
        return KEY_PREFIX + scope + ":" + username;
    }
}
