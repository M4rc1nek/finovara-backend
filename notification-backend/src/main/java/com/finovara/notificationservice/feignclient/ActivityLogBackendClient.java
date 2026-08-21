package com.finovara.notificationservice.feignclient;

import com.finovara.contracts.notification.email.digest.report.security.WeeklySecurityDigestReportDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "activity-log-backend", url = "${activity-log-backend.url}")
public interface ActivityLogBackendClient {

    @GetMapping("/internal/activity/security/digest/weekly-email/log-summary")
    List<WeeklySecurityDigestReportDto> getSecurityDigestReport();
}