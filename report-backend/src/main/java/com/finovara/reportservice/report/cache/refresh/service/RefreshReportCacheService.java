package com.finovara.reportservice.report.cache.refresh.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void evictDataForUser(Long userId) {
        log.info("Trying to delete report keys for userId: {}", userId);

        List<String> patterns = List.of(
                "report:*:" + userId + "*",
                "report:*::" + userId
        );

        for (String pattern : patterns) {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(100)
                    .build();

            try (var cursor = redisTemplate.scan(options)) {
                cursor.forEachRemaining(key -> redisTemplate.delete(key));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to delete redis report keys for pattern: " + pattern);
            }
        }

        log.info("Report keys deleted for userId: {}", userId);
    }
}