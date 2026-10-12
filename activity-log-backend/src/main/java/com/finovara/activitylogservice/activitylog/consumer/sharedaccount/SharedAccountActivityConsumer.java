package com.finovara.activitylogservice.activitylog.consumer.sharedaccount;

import com.finovara.activitylogservice.activitylog.sharedaccount.service.SharedAccountActivityLogService;
import com.finovara.activitylogservice.cache.evict.ActivityCache;
import com.finovara.activitylogservice.cache.evict.EvictActivityCacheService;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.contracts.sharedaccount.event.deletion.SharedAccountDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class SharedAccountActivityConsumer {

    private final SharedAccountActivityDataDeleter sharedAccountActivityDataDeleter;
    private final SharedAccountActivityLogService sharedAccountActivityLogService;
    private final EvictActivityCacheService evictActivityCacheService;

    @KafkaListener(topics = "shared-account.activity")
    public void consumeSharedAccountActivityLog(SharedAccountActivityLogEvent event) {
        sharedAccountActivityLogService.createSharedAccountActivityLog(event);
    }

    @KafkaListener(topics = "shared-account.deleted")
    public void deleteDataFromSharedAccount(SharedAccountDeletedEvent event) {
        sharedAccountActivityDataDeleter.deleteSharedAccountActivityLogData(event.ownerId(), event.memberId());
        evictActivityCacheService.evict(ActivityCache.SHARED_ACCOUNT_ACTIVITY_LOG, event.ownerId(), event.memberId());
    }
}
