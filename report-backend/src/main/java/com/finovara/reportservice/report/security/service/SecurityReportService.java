package com.finovara.reportservice.report.security.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.report.dto.security.SecurityReportDto;
import com.finovara.reportservice.feignclient.ActivityLogBackendClient;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityReportService {
    private final ActivityLogBackendClient activityLogBackendClient;

    @Cacheable(value = "security-report", key = "#userId + ':' + #periodType")
    public SecurityReportDto buildReport(Long userId, PeriodType periodType) {
        if (periodType == null) {
            throw new InvalidInputException("Unsupported report period type.");
        }
        return activityLogBackendClient.getSecurityReport(userId, periodType);
    }

}
