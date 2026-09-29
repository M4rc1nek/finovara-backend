package com.finovara.reportservice.feignclient;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.report.dto.SecurityReportDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "activity-log-backend", url = "${activity-log-backend.url}", contextId = "activityLogBackendReportClient")
public interface ActivityLogBackendClient {

    @GetMapping("/internal/activity/security/report")
    SecurityReportDto getSecurityReport(@RequestHeader("X-User-Id") Long userId, @RequestParam PeriodType periodType);
}
