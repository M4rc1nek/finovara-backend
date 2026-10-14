package com.finovara.reportservice.consumer;

import com.finovara.contracts.mainaccount.activity.event.expense.ExpenseActivityEvent;
import com.finovara.contracts.mainaccount.activity.event.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.mainaccount.activity.event.revenue.RevenueActivityEvent;
import com.finovara.contracts.sharedaccount.event.deletion.SharedAccountDeletedEvent;
import com.finovara.contracts.user.event.account.UserAccountDeletedEvent;
/*import com.finovara.reportservice.healthscore.cache.evict.HealthScoreCacheEvictor;*/
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
   // private final HealthScoreCacheEvictor healthScoreCacheEvictor;

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
     //   healthScoreCacheEvictor.evict(event.userId());

    }

  /*  @KafkaListener(topics = "expense.created", groupId = "report.health-score-evict")
    public void onExpense(ExpenseActivityEvent event) {
        healthScoreCacheEvictor.evict(event.userId());
    }

    @KafkaListener(topics = "revenue.created", groupId = "report.health-score-evict")
    public void onRevenue(RevenueActivityEvent event) {
        healthScoreCacheEvictor.evict(event.userId());
    }

    @KafkaListener(topics = "piggybank.transaction.created", groupId = "report.health-score-evict")
    public void onPiggyBankTransaction(PiggyBankActivityEvent event) {
        healthScoreCacheEvictor.evict(event.userId());
    }*/
}
