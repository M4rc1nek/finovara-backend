package com.finovara.reportservice.consumer;

import com.finovara.contracts.sharedaccount.event.deletion.SharedAccountDeletedEvent;
import com.finovara.contracts.user.event.account.UserAccountDeletedEvent;
import com.finovara.reportservice.report.cache.evict.service.EvictReportCacheService;
import com.finovara.reportservice.sharedaccount.report.cache.evict.service.EvictSharedReportCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportDataDeleterConsumer {
    private final EvictReportCacheService evictReportCacheService;
    private final EvictSharedReportCacheService evictSharedReportCacheService;

    @KafkaListener(topics = "shared-account.deleted")
    public void deleteSharedReportHistoryCache(SharedAccountDeletedEvent event) {
        evictSharedReportCacheService.evictDataForUser(event.ownerId());
        evictSharedReportCacheService.evictDataForUser(event.memberId());
        evictSharedReportCacheService.evictSharedAccountSecurityDataForUser(event.ownerId());
        evictSharedReportCacheService.evictSharedAccountSecurityDataForUser(event.memberId());
    }

    @KafkaListener(topics = "user-account.deleted")
    public void deleteReportHistoryCache(UserAccountDeletedEvent event) {
        evictReportCacheService.evictFinancialDataForUser(event.userId());
        evictReportCacheService.evictSecurityDataForUser(event.userId());
    }
}
