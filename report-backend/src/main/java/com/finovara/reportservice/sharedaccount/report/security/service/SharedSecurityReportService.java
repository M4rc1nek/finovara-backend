package com.finovara.reportservice.sharedaccount.report.security.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.sharedaccount.report.security.dto.SharedAccountSecurityOverviewDto;
import com.finovara.reportservice.feignclient.ActivityLogBackendClient;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SharedSecurityReportService {

    private final ActivityLogBackendClient activityLogBackendClient;

    @Cacheable(value = "security-report:shared", key = "#userId + ':' + #periodType")
    public SharedAccountSecurityOverviewDto buildReport(Long userId, PeriodType periodType) {
        if (periodType == null) {
            throw new InvalidInputException("Unsupported report period type.");
        }
        return activityLogBackendClient.getSharedAccountSecurityReport(userId, periodType);
    }
}