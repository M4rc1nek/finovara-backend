package com.finovara.reportservice.report.cache.evict.service;

import com.finovara.contracts.cache.RedisCacheEvictor;
import com.finovara.contracts.user.event.account.UserAccountDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvictReportCacheService {

    private final RedisCacheEvictor redisCacheEvictor;

    public void evictFinancialDataForUser(Long userId) {
        redisCacheEvictor.evictByPatterns(List.of(
                "financial-report:*:" + userId + "*",
                "financial-report:*::" + userId
        ));
    }

    public void evictSecurityDataForUser(Long userId) {
        redisCacheEvictor.evictByPatterns(List.of(
                "security-report:*:" + userId + "*",
                "security-report:*:" + userId

        ));
    }

    @KafkaListener(topics = "user-account.deleted")
    public void deleteReportHistoryCache(UserAccountDeletedEvent event) {
        evictFinancialDataForUser(event.userId());
        evictSecurityDataForUser(event.userId());
    }

}