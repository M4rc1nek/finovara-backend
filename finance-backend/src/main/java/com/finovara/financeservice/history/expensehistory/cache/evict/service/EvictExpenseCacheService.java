package com.finovara.financeservice.history.expensehistory.cache.evict.service;

import com.finovara.contracts.cache.RedisCacheEvictor;
import com.finovara.contracts.event.user.delete.account.UserAccountDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EvictExpenseCacheService {

    private final RedisCacheEvictor redisCacheEvictor;

    public void evictDataForUser(Long userId) {
        redisCacheEvictor.evictByPattern("expense:historyByCategory::*" + userId + ":*");
    }

    @KafkaListener(topics = "user-account.deleted", groupId = "expense-history.delete-cache")
    public void deleteExpenseHistoryCache(UserAccountDeletedEvent event){
        evictDataForUser(event.userId());
    }
}