package com.finovara.financeservice.history.revenuehistory.cache.evict.service;

import com.finovara.contracts.cache.RedisCacheEvictor;
import com.finovara.contracts.event.user.delete.account.UserAccountDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EvictRevenueCacheService {

    private final RedisCacheEvictor redisCacheEvictor;

    public void evictDataForUser(Long userId) {
        redisCacheEvictor.evictByPattern("revenue:historyByCategory::*" + userId + ":*");
    }

    @KafkaListener(topics = "user-account.deleted", groupId = "revenue-history.delete-cache")
    public void deleteRevenueHistoryCache(UserAccountDeletedEvent event){
        evictDataForUser(event.userId());
    }
}