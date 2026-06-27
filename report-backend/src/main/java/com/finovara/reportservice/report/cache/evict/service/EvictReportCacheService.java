package com.finovara.reportservice.report.cache.refresh.service;

import com.finovara.contracts.cache.RedisCacheEvictor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RefreshReportCacheService {

    private final RedisCacheEvictor redisCacheEvictor;

    public void evictDataForUser(Long userId) {
        redisCacheEvictor.evictByPatterns(List.of(
                "report:*:" + userId + "*",
                "report:*::" + userId
        ));
    }

}