package com.finovara.contracts.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheEvictor {

    private static final int BATCH_SIZE = 500;

    private final StringRedisTemplate stringRedisTemplate;

    public void evictByPattern(String pattern) {
        log.info("Trying to delete Redis keys matching pattern: {}", pattern);

        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(100)
                .build();

        List<String> batch = new ArrayList<>(BATCH_SIZE);

        try (var cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= BATCH_SIZE) {
                    stringRedisTemplate.delete(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                stringRedisTemplate.delete(batch);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete Redis keys for pattern: " + pattern, e);
        }

        log.info("Redis keys deleted for pattern: {}", pattern);
    }

    public void evictByPatterns(List<String> patterns) {
        patterns.forEach(this::evictByPattern);
    }

    public void evictKeys(Collection<String> keys) {
        List<String> nonNull = keys.stream().filter(Objects::nonNull).toList();
        if (nonNull.isEmpty()) {
            return;
        }

        try {
            stringRedisTemplate.delete(nonNull);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete Redis keys: " + nonNull, e);
        }

        log.info("Redis keys deleted: {}", nonNull);
    }
}