package com.finovara.reportservice.feignclient;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.mainaccount.report.savingsplan.dto.SavingsPlanMonthDto;
import com.finovara.contracts.util.PeriodType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "finance-backend", url = "${finance-backend.url}", contextId = "financeReportClient")
public interface FinanceBackendReportClient {

    @GetMapping("/internal/reports/financial-report")
    FinancialReportDto getFinancialReport(@RequestParam("userId") Long userId, @RequestParam("periodType") PeriodType periodType);

    @GetMapping("/internal/reports/wallet/balance")
    BigDecimal walletBalance(@RequestParam Long userId);

    @GetMapping("/internal/savings-plan/monthly-data")
    List<SavingsPlanMonthDto> getSavingsPlanMonthlyData(@RequestParam("userId") Long userId);
}