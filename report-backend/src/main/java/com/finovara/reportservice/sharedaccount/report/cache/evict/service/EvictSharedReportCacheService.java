package com.finovara.reportservice.sharedaccount.report.cache.evict.service;

import com.finovara.contracts.cache.RedisCacheEvictor;
import com.finovara.contracts.event.finance.sharedaccount.SharedAccountDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvictSharedReportCacheService {

    private final RedisCacheEvictor redisCacheEvictor;

    public void evictDataForUser(Long userId) {
        redisCacheEvictor.evictByPatterns(List.of(
                "report:shared*:" + userId + ":*",
                "report:shared*:*:" + userId + ":*",
                "report:shared*::" + userId
        ));
    }

    @KafkaListener(topics = "shared-account.deleted", groupId = "shared-account.report")
    public void deleteSharedReportHistoryCache(SharedAccountDeletedEvent event) {
        evictDataForUser(event.ownerId());
        evictDataForUser(event.memberId());
    }
}