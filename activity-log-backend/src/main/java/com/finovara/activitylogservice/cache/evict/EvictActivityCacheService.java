package com.finovara.activitylogservice.cache.evict;

import com.finovara.contracts.cache.RedisCacheEvictor;
import com.finovara.contracts.sharedaccount.event.deletion.SharedAccountDeletedEvent;
import com.finovara.contracts.util.SortType;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EvictActivityCacheService {

    private static final String KEY_PREFIX = "shared-account:activity::";

    private final RedisCacheEvictor redisCacheEvictor;

    public void evictSharedAccountActivityCacheData(Long... userIds) {
        List<String> keys = Arrays.stream(userIds)
                .filter(Objects::nonNull)
                .flatMap(id -> Arrays.stream(SortType.values())
                        .map(sort -> KEY_PREFIX + id + ":" + sort))
                .toList();

        redisCacheEvictor.evictKeys(keys);
    }

    @KafkaListener(topics = "shared-account.deleted", groupId = "activity-log-service.cache-evict")
    public void deleteSharedAccountActivityHistoryCache(SharedAccountDeletedEvent event) {
        evictSharedAccountActivityCacheData(event.ownerId(), event.memberId());
    }
}