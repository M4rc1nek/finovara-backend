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
@RequestMapping("/internal/shared/reports")
@RequiredArgsConstructor
public class InternalSharedReportDataController {

    private final InternalSharedReportDataService internalSharedReportDataService;

    @GetMapping("/expenses/sum")
    public ResponseEntity<BigDecimal> sumExpenses(@RequestParam Long ownerId, @RequestParam Long memberId, @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return ResponseEntity.ok(internalSharedReportDataService.sumExpenses(ownerId, memberId, from, to));
    }

    @GetMapping("/revenues/sum")
    public ResponseEntity<BigDecimal> sumRevenues(@RequestParam Long ownerId, @RequestParam Long memberId, @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return ResponseEntity.ok(internalSharedReportDataService.sumRevenues(ownerId, memberId, from, to));
    }

    @GetMapping("/revenues/highest")
    public ResponseEntity<List<HighestRevenueDto>> highestRevenues(@RequestParam Long ownerId, @RequestParam Long memberId, @RequestParam LocalDate from, @RequestParam LocalDate to, @RequestParam int pageSize) {
        return ResponseEntity.ok(internalSharedReportDataService.highestRevenues(ownerId, memberId, from, to, pageSize));
    }

    @GetMapping("/expenses/highest")
    public ResponseEntity<List<HighestExpenseDto>> highestExpenses(@RequestParam Long ownerId, @RequestParam Long memberId, @RequestParam LocalDate from, @RequestParam LocalDate to, @RequestParam int pageSize) {
        return ResponseEntity.ok(internalSharedReportDataService.highestExpenses(ownerId, memberId, from, to, pageSize));
    }

    @GetMapping("/expenses/by-category")
    public ResponseEntity<BigDecimal> expensesByCategory(@RequestParam Long ownerId, @RequestParam Long memberId, @RequestParam LocalDate from, @RequestParam LocalDate to, @RequestParam ExpenseCategory category) {
        return ResponseEntity.ok(internalSharedReportDataService.expensesByCategory(ownerId, memberId, from, to, category));
    }

    @GetMapping("/revenues/by-category")
    public ResponseEntity<BigDecimal> revenuesByCategory(@RequestParam Long ownerId, @RequestParam Long memberId, @RequestParam LocalDate from, @RequestParam LocalDate to, @RequestParam RevenueCategory category) {
        return ResponseEntity.ok(internalSharedReportDataService.revenuesByCategory(ownerId, memberId, from, to, category));
    }

    @GetMapping("/revenues/sum-all")
    public ResponseEntity<BigDecimal> sumAllRevenues(@RequestParam Long ownerId, @RequestParam Long memberId) {
        return ResponseEntity.ok(internalSharedReportDataService.sumAllRevenues(ownerId, memberId));
    }

    @GetMapping("/expenses/sum-all")
    public ResponseEntity<BigDecimal> sumAllExpenses(@RequestParam Long ownerId, @RequestParam Long memberId) {
        return ResponseEntity.ok(internalSharedReportDataService.sumAllExpenses(ownerId, memberId));
    }

    @GetMapping("/expenses/grouped-by-date")
    public ResponseEntity<List<DailyCashDto>> expensesGroupedByDate(@RequestParam Long ownerId, @RequestParam Long memberId) {
        return ResponseEntity.ok(internalSharedReportDataService.expensesGroupedByDate(ownerId, memberId));
    }

    @GetMapping("/expenses/avg-grouped-by-date")
    public ResponseEntity<List<DailyCashDto>> expensesAvgGroupedByDate(@RequestParam Long ownerId, @RequestParam Long memberId) {
        return ResponseEntity.ok(internalSharedReportDataService.expensesAvgGroupedByDate(ownerId, memberId));
    }

    @GetMapping("/revenues/grouped-by-date")
    public ResponseEntity<List<DailyCashDto>> revenuesGroupedByDate(@RequestParam Long ownerId, @RequestParam Long memberId) {
        return ResponseEntity.ok(internalSharedReportDataService.revenuesGroupedByDate(ownerId, memberId));
    }

    @GetMapping("/revenues/avg-grouped-by-date")
    public ResponseEntity<List<DailyCashDto>> revenuesAvgGroupedByDate(@RequestParam Long ownerId, @RequestParam Long memberId) {
        return ResponseEntity.ok(internalSharedReportDataService.revenuesAvgGroupedByDate(ownerId, memberId));
    }

    @GetMapping("/wallet/balance")
    public ResponseEntity<BigDecimal> walletBalance(@RequestParam Long ownerId, @RequestParam Long memberId) {
        return ResponseEntity.ok(internalSharedReportDataService.walletBalance(ownerId, memberId));
    }
}