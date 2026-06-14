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

@FeignClient(name = "finance-backend", url = "${finance-backend.url}")
public interface CoreBackendReportClient {

    @GetMapping("/internal/reports/expenses/sum")
    BigDecimal sumExpenses(@RequestParam Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to);

    @GetMapping("/internal/reports/expenses/average")
    BigDecimal avgExpenses(@RequestParam Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to);

    @GetMapping("/internal/reports/expenses/highest")
    List<HighestExpenseDto> highestExpenses(@RequestParam Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to, @RequestParam int pageSize);

    @GetMapping("/internal/reports/expenses/by-category")
    BigDecimal expensesByCategory(@RequestParam Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to, @RequestParam ExpenseCategory category);

    @GetMapping("/internal/reports/expenses/sum-all")
    BigDecimal sumAllExpenses(@RequestParam Long userId);

    @GetMapping("/internal/reports/expenses/grouped-by-date")
    List<DailyCashDto> expensesGroupedByDate(@RequestParam Long userId);

    @GetMapping("/internal/reports/expenses/avg-grouped-by-date")
    List<DailyCashDto> expensesAvgGroupedByDate(@RequestParam Long userId);

    @GetMapping("/internal/reports/revenues/sum")
    BigDecimal sumRevenues(@RequestParam Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to);

    @GetMapping("/internal/reports/revenues/average")
    BigDecimal avgRevenues(@RequestParam Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to);

    @GetMapping("/internal/reports/revenues/highest")
    List<HighestRevenueDto> highestRevenues(@RequestParam Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to, @RequestParam int pageSize);

    @GetMapping("/internal/reports/revenues/by-category")
    BigDecimal revenuesByCategory(@RequestParam Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to, @RequestParam RevenueCategory category);

    @GetMapping("/internal/reports/revenues/sum-all")
    BigDecimal sumAllRevenues(@RequestParam Long userId);

    @GetMapping("/internal/reports/revenues/grouped-by-date")
    List<DailyCashDto> revenuesGroupedByDate(@RequestParam Long userId);

    @GetMapping("/internal/reports/revenues/avg-grouped-by-date")
    List<DailyCashDto> revenuesAvgGroupedByDate(@RequestParam Long userId);

    @GetMapping("/internal/reports/wallet/balance")
    BigDecimal walletBalance(@RequestParam Long userId);
}