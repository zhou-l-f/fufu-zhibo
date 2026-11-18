package com.zlf.fufuzhibo.server.service.impl;

import com.zlf.fufuzhibo.server.service.ViewerCountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 在线观看人数：基于 SRS on_play / on_stop 回调在 Redis 中计数。
 * 所有 Redis 异常都在内部吞掉，计数失败不能影响观众拉流。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewerCountServiceImpl implements ViewerCountService {

    private static final String KEY_PREFIX = "live:viewers:";
    private static final long TTL_SECONDS = Duration.ofHours(24).getSeconds();

    /**
     * +1 并刷新过期时间
     */
    private static final DefaultRedisScript<Long> INCR_SCRIPT = new DefaultRedisScript<>(
            "local v = redis.call('incr', KEYS[1]) "
                    + "redis.call('expire', KEYS[1], ARGV[1]) "
                    + "return v",
            Long.class);

    /**
     * -1 并夹到 0，避免重复 on_stop 造成负数
     */
    private static final DefaultRedisScript<Long> DECR_SCRIPT = new DefaultRedisScript<>(
            "local v = redis.call('decr', KEYS[1]) "
                    + "if v < 0 then redis.call('set', KEYS[1], 0) v = 0 end "
                    + "redis.call('expire', KEYS[1], ARGV[1]) "
                    + "return v",
            Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void onPlay(String streamName) {
        if (isBlank(streamName)) {
            return;
        }
        try {
            stringRedisTemplate.execute(INCR_SCRIPT,
                    List.of(KEY_PREFIX + streamName), String.valueOf(TTL_SECONDS));
        } catch (Exception e) {
            log.warn("在线人数 +1 失败，忽略: stream={}", streamName, e);
        }
    }

    @Override
    public void onStop(String streamName) {
        if (isBlank(streamName)) {
            return;
        }
        try {
            stringRedisTemplate.execute(DECR_SCRIPT,
                    List.of(KEY_PREFIX + streamName), String.valueOf(TTL_SECONDS));
        } catch (Exception e) {
            log.warn("在线人数 -1 失败，忽略: stream={}", streamName, e);
        }
    }

    @Override
    public int current(String streamName) {
        if (isBlank(streamName)) {
            return 0;
        }
        try {
            String value = stringRedisTemplate.opsForValue().get(KEY_PREFIX + streamName);
            return value == null ? 0 : Math.max(Integer.parseInt(value), 0);
        } catch (Exception e) {
            log.warn("读取在线人数失败，降级为 0: stream={}", streamName, e);
            return 0;
        }
    }

    @Override
    public void clear(String streamName) {
        if (isBlank(streamName)) {
            return;
        }
        try {
            stringRedisTemplate.delete(KEY_PREFIX + streamName);
        } catch (Exception e) {
            log.warn("清空在线人数失败: stream={}", streamName, e);
        }
    }

    @Override
    public void syncCounts(Map<String, Integer> countByStream) {
        if (countByStream == null || countByStream.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Integer> entry : countByStream.entrySet()) {
            if (isBlank(entry.getKey()) || entry.getValue() == null) {
                continue;
            }
            try {
                stringRedisTemplate.opsForValue().set(KEY_PREFIX + entry.getKey(),
                        String.valueOf(Math.max(entry.getValue(), 0)),
                        Duration.ofSeconds(TTL_SECONDS));
            } catch (Exception e) {
                log.warn("校正在线人数失败: stream={}", entry.getKey(), e);
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
