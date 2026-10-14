package com.finovara.reportservice.feignclient;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.util.PeriodType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "finance-backend", url = "${finance-backend.url}", contextId = "financeSharedReportClient")
public interface FinanceBackendSharedReportClient {

    @GetMapping("/internal/shared/reports/financial-report")
    FinancialReportDto getFinancialReport(@RequestParam Long ownerId, @RequestParam Long memberId, @RequestParam PeriodType periodType);
}