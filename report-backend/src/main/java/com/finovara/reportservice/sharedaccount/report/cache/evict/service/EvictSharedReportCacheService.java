package com.finovara.reportservice.sharedaccount.report.finances.cache.evict.service;

import com.finovara.contracts.cache.RedisCacheEvictor;
import com.finovara.contracts.event.user.delete.account.UserAccountDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvictSharedReportCacheService {

    private final RedisCacheEvictor redisCacheEvictor;

    public void evictDataForUser(Long userId) {
        // Only evict caches belonging to shared finances (prefix 'shared')
        redisCacheEvictor.evictByPatterns(List.of(
                "report:shared*:" + userId + "*",
                "report:shared*::" + userId
        ));
    }

    @KafkaListener(topics = "user-account.deleted", groupId = "shared-account.report")
    public void deleteSharedReportHistoryCache(UserAccountDeletedEvent event){
        evictDataForUser(event.userId());
    }

}