package com.finovara.activitylogservice.cache;

import com.finovara.contracts.cache.RedisCacheEvictor;
import com.finovara.contracts.sharedaccount.event.deletion.SharedAccountDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvictActivityCacheService {

    private final RedisCacheEvictor redisCacheEvictor;

    public void evictSharedAccountActivityCacheData(Long userId) {
        redisCacheEvictor.evictByPatterns(List.of(
                "shared-account:activity::" + userId + ":*"
        ));
    }

    @KafkaListener(topics = "shared-account.deleted", groupId = "shared-account.report")
    public void deleteSharedAccountActivityHistoryCache(SharedAccountDeletedEvent event){
        evictSharedAccountActivityCacheData(event.remainingUserId());

    }
}
