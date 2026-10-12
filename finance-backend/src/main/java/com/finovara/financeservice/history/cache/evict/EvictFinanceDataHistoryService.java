package com.finovara.financeservice.history.cache.evict;

import com.finovara.contracts.cache.RedisCacheEvictor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EvictFinanceDataHistoryService {

    private final RedisCacheEvictor redisCacheEvictor;

    public void evictFinanceDataHistory(Long userId){
        evictRevenueHistoryForUser(userId);
        evictExpenseHistoryForUser(userId);
    }

    public void evictRevenueHistoryForUser(Long userId) {
        redisCacheEvictor.evictByPattern("historyByCategory:revenue::" + userId + ":*");
    }

    public void evictExpenseHistoryForUser(Long userId) {
        redisCacheEvictor.evictByPattern("historyByCategory:expense::" + userId + ":*");
    }

}
