package com.finovara.reportservice.feignclient;

import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.transaction.report.dto.DailyCashDto;
import com.finovara.contracts.transaction.report.dto.HighestExpenseDto;
import com.finovara.contracts.transaction.report.dto.HighestRevenueDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@FeignClient(name = "finance-backend", url = "${finance-backend.url}", contextId = "financeSharedReportClient")
public interface FinanceBackendSharedReportClient {

    @GetMapping("/internal/shared/reports/expenses/sum")
    BigDecimal sumExpenses(@RequestParam Long ownerId, @RequestParam Long memberId, @RequestParam LocalDate from, @RequestParam LocalDate to);

    @GetMapping("/internal/shared/reports/revenues/sum")
    BigDecimal sumRevenues(@RequestParam Long ownerId, @RequestParam Long memberId, @RequestParam LocalDate from, @RequestParam LocalDate to);

    @GetMapping("/internal/shared/reports/expenses/sum-all")
    BigDecimal sumAllExpenses(@RequestParam Long ownerId, @RequestParam Long memberId);

    @GetMapping("/internal/shared/reports/revenues/sum-all")
    BigDecimal sumAllRevenues(@RequestParam Long ownerId, @RequestParam Long memberId);

    @GetMapping("/internal/shared/reports/expenses/highest")
    List<HighestExpenseDto> highestExpenses(@RequestParam Long ownerId, @RequestParam Long memberId, @RequestParam LocalDate from, @RequestParam LocalDate to, @RequestParam int pageSize);

    @GetMapping("/internal/shared/reports/revenues/highest")
    List<HighestRevenueDto> highestRevenues(@RequestParam Long ownerId, @RequestParam Long memberId, @RequestParam LocalDate from, @RequestParam LocalDate to, @RequestParam int pageSize);

    @GetMapping("/internal/shared/reports/expenses/by-category")
    BigDecimal expensesByCategory(@RequestParam Long ownerId, @RequestParam Long memberId, @RequestParam LocalDate from, @RequestParam LocalDate to, @RequestParam ExpenseCategory category);

    @GetMapping("/internal/shared/reports/revenues/by-category")
    BigDecimal revenuesByCategory(@RequestParam Long ownerId, @RequestParam Long memberId, @RequestParam LocalDate from, @RequestParam LocalDate to, @RequestParam RevenueCategory category);

    @GetMapping("/internal/shared/reports/expenses/grouped-by-date")
    List<DailyCashDto> expensesGroupedByDate(@RequestParam Long ownerId, @RequestParam Long memberId);

    @GetMapping("/internal/shared/reports/expenses/avg-grouped-by-date")
    List<DailyCashDto> expensesAvgGroupedByDate(@RequestParam Long ownerId, @RequestParam Long memberId);

    @GetMapping("/internal/shared/reports/revenues/avg-grouped-by-date")
    List<DailyCashDto> revenuesAvgGroupedByDate(@RequestParam Long ownerId, @RequestParam Long memberId);

    @GetMapping("/internal/shared/reports/revenues/grouped-by-date")
    List<DailyCashDto> revenuesGroupedByDate(@RequestParam Long ownerId, @RequestParam Long memberId);

    @GetMapping("/internal/shared/reports/wallet/balance")
    BigDecimal walletBalance(@RequestParam Long ownerId, @RequestParam Long memberId);
}