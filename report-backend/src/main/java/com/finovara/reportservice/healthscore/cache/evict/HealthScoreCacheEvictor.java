package com.finovara.reportservice.healthscore.cache.evict;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HealthScoreCacheEvictor {

    @CacheEvict(value = "healthScore", key = "#userId")
    public void evict(Long userId) {
        log.info("Health score cache evicted for userId={}", userId);
    }
}