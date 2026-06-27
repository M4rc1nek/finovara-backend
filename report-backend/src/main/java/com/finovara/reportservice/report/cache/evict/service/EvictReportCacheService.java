package com.finovara.reportservice.report.cache.evict.service;

import com.finovara.contracts.cache.RedisCacheEvictor;
import com.finovara.contracts.event.user.UserAccountDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvictReportCacheService {

    private final RedisCacheEvictor redisCacheEvictor;

    public void evictDataForUser(Long userId) {
        redisCacheEvictor.evictByPatterns(List.of(
                "report:*:" + userId + "*",
                "report:*::" + userId
        ));
    }

    @KafkaListener(topics = "user-account.deleted")
    public void deleteReportHistoryCache(UserAccountDeletedEvent event){
        evictDataForUser(event.userId());
    }

}