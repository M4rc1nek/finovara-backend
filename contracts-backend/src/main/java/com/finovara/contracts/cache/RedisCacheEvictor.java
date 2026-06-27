package com.finovara.contracts.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheEvictor {

    private final RedisTemplate<String, Object> redisTemplate;

    public void evictByPattern(String pattern) {
        log.info("Trying to delete Redis keys matching pattern: {}", pattern);

        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(100)
                .build();

        try (var cursor = redisTemplate.scan(options)) {
            cursor.forEachRemaining(redisTemplate::delete);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete Redis keys for pattern: " + pattern);
        }

        log.info("Redis keys deleted for pattern: {}", pattern);
    }

    public void evictByPatterns(List<String> patterns) {
        patterns.forEach(this::evictByPattern);
    }
}