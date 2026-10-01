package com.finovara.reportservice.sharedaccount.report.cache.evict.service;

import com.finovara.contracts.cache.RedisCacheEvictor;
import com.finovara.contracts.notification.event.sharedaccount.deletion.SharedAccountDeletedEvent;
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
                "financial-report:shared*:" + userId + ":*",
                "financial-report:shared*:*:" + userId + ":*",
                "financial-report:shared*::" + userId
        ));
    }

    public void evictSharedAccountSecurityDataForUser(Long userId) {
        redisCacheEvictor.evictByPatterns(List.of(
                "security-report:shared:*:" + userId + "*"
        ));
    }

    @KafkaListener(topics = "shared-account.deleted", groupId = "shared-account.report")
    public void deleteSharedReportHistoryCache(SharedAccountDeletedEvent event) {
        evictDataForUser(event.ownerId());
        evictDataForUser(event.memberId());
        evictSharedAccountSecurityDataForUser(event.ownerId());
        evictSharedAccountSecurityDataForUser(event.memberId());
    }
}