package com.finovara.reportservice.feignclient;

import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.mainaccount.report.security.dto.SecurityReportDto;
import com.finovara.contracts.sharedaccount.report.security.dto.SharedAccountSecurityOverviewDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "activity-log-backend", url = "${activity-log-backend.url}", contextId = "activityLogBackendReportClient")
public interface ActivityLogBackendClient {

    @GetMapping("/internal/activity/security/report")
    SecurityReportDto getSecurityReport(@RequestHeader("X-User-Id") Long userId, @RequestParam PeriodType periodType);

    @GetMapping("/internal/shared-accounts/reports/security")
    SharedAccountSecurityOverviewDto getSharedAccountSecurityReport(@RequestHeader("X-User-Id") Long userId, @RequestParam PeriodType periodType);
}
