package com.finovara.financeservice.history.revenuehistory.cache.refresh.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshRevenueCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void evictDataForUser(Long userId) {
        log.info("Trying to delete keys for userId: {}", userId);
            ScanOptions options = ScanOptions.scanOptions()
                    .match("revenue:historyByCategory::*" + userId + ":*")
                    .count(100)
                    .build();

            try (var cursor = redisTemplate.scan(options)) {
                cursor.forEachRemaining(key -> redisTemplate.delete(key));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to delete redis keys for Revenue History");
        }

        log.info("Revenue History keys deleted for userId: {}", userId);
    }
}