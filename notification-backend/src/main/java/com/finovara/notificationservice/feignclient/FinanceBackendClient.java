package com.finovara.notificationservice.feignclient;

import com.finovara.contracts.notification.email.digest.report.finance.WeeklyFinanceDigestReportDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "finance-backend", url = "${finance-backend.url}")
public interface FinanceBackendClient {

    @GetMapping("/internal/digest/weekly-email/report")
    List<WeeklyFinanceDigestReportDto> getWeeklyFinanceDigestReports();
}