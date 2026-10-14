package com.finovara.reportservice.report.cache.evict.service;

import com.finovara.contracts.cache.RedisCacheEvictor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvictReportCacheService {

    private static final String FINANCIAL_PREFIX = "financial-report";
    private static final String SECURITY_PREFIX = "security-report";

    private final RedisCacheEvictor redisCacheEvictor;

    public void evictFinancialDataForUser(Long userId) {
        redisCacheEvictor.evictByPatterns(buildPatterns(FINANCIAL_PREFIX, userId));
    }

    public void evictSecurityDataForUser(Long userId) {redisCacheEvictor.evictByPatterns(buildPatterns(SECURITY_PREFIX, userId));
    }

    private List<String> buildPatterns(String prefix, Long userId) {
        return List.of(prefix + "::" + userId + ":*");
    }
}