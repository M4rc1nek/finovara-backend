package com.finovara.activitylogservice.cache.evict;

import com.finovara.contracts.cache.RedisCacheEvictor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EvictActivityCacheService {

    private final RedisCacheEvictor redisCacheEvictor;

    public void evict(ActivityCache cache, Long... userIds) {
        redisCacheEvictor.evictByPatterns(Arrays.stream(userIds)
                .filter(Objects::nonNull)
                .map(cache::patternFor)
                .toList());
    }

    public void evictAllForUser(Long userId) {
        redisCacheEvictor.evictByPatterns(Arrays.stream(ActivityCache.values())
                .map(cache -> cache.patternFor(userId))
                .toList());
    }
}