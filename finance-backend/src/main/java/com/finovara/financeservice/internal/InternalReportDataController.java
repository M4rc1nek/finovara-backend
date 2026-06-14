package com.finovara.financeservice.internal;

import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.transaction.report.dto.DailyCashDto;
import com.finovara.contracts.transaction.report.dto.HighestExpenseDto;
import com.finovara.contracts.transaction.report.dto.HighestRevenueDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/internal/reports")
@RequiredArgsConstructor
public class InternalReportDataController {

    //Move this controller to transaction-service

    private final InternalReportDataService internalReportDataService;

    @GetMapping("/expenses/sum")
    public ResponseEntity<BigDecimal> sumExpenses(@RequestParam Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return ResponseEntity.ok(internalReportDataService.sumExpenses(userId, from, to));
    }

    @GetMapping("/expenses/average")
    public ResponseEntity<BigDecimal> avgExpenses(@RequestParam Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return ResponseEntity.ok(internalReportDataService.avgExpenses(userId, from, to));
    }

    @GetMapping("/expenses/highest")
    public ResponseEntity<List<HighestExpenseDto>> highestExpenses(@RequestParam Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to, @RequestParam int pageSize) {
        return ResponseEntity.ok(internalReportDataService.highestExpenses(userId, from, to, pageSize));
    }

    @GetMapping("/expenses/by-category")
    public ResponseEntity<BigDecimal> expensesByCategory(@RequestParam Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to, @RequestParam ExpenseCategory category) {
        return ResponseEntity.ok(internalReportDataService.expensesByCategory(userId, from, to, category));
    }

    @GetMapping("/expenses/sum-all")
    public ResponseEntity<BigDecimal> sumAllExpenses(@RequestParam Long userId) {
        return ResponseEntity.ok(internalReportDataService.sumAllExpenses(userId));
    }

    @GetMapping("/expenses/grouped-by-date")
    public ResponseEntity<List<DailyCashDto>> expensesGroupedByDate(@RequestParam Long userId) {
        return ResponseEntity.ok(internalReportDataService.expensesGroupedByDate(userId));
    }

    @GetMapping("/expenses/avg-grouped-by-date")
    public ResponseEntity<List<DailyCashDto>> expensesAvgGroupedByDate(@RequestParam Long userId) {
        return ResponseEntity.ok(internalReportDataService.expensesAvgGroupedByDate(userId));
    }

    @GetMapping("/revenues/sum")
    public ResponseEntity<BigDecimal> sumRevenues(@RequestParam Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return ResponseEntity.ok(internalReportDataService.sumRevenues(userId, from, to));
    }

    @GetMapping("/revenues/average")
    public ResponseEntity<BigDecimal> avgRevenues(@RequestParam Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return ResponseEntity.ok(internalReportDataService.avgRevenues(userId, from, to));
    }

    @GetMapping("/revenues/highest")
    public ResponseEntity<List<HighestRevenueDto>> highestRevenues(@RequestParam Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to, @RequestParam int pageSize) {
        return ResponseEntity.ok(internalReportDataService.highestRevenues(userId, from, to, pageSize));
    }

    @GetMapping("/revenues/by-category")
    public ResponseEntity<BigDecimal> revenuesByCategory(@RequestParam Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to, @RequestParam RevenueCategory category) {
        return ResponseEntity.ok(internalReportDataService.revenuesByCategory(userId, from, to, category));
    }

    @GetMapping("/revenues/sum-all")
    public ResponseEntity<BigDecimal> sumAllRevenues(@RequestParam Long userId) {
        return ResponseEntity.ok(internalReportDataService.sumAllRevenues(userId));
    }

    @GetMapping("/revenues/grouped-by-date")
    public ResponseEntity<List<DailyCashDto>> revenuesGroupedByDate(@RequestParam Long userId) {
        return ResponseEntity.ok(internalReportDataService.revenuesGroupedByDate(userId));
    }

    @GetMapping("/revenues/avg-grouped-by-date")
    public ResponseEntity<List<DailyCashDto>> revenuesAvgGroupedByDate(@RequestParam Long userId) {
        return ResponseEntity.ok(internalReportDataService.revenuesAvgGroupedByDate(userId));
    }

    @GetMapping("/wallet/balance")
    public ResponseEntity<BigDecimal> walletBalance(@RequestParam Long userId) {
        return ResponseEntity.ok(internalReportDataService.walletBalance(userId));
    }
}