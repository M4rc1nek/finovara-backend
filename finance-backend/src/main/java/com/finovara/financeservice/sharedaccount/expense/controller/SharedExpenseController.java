package com.finovara.financeservice.sharedaccount.expense.controller;

import com.finovara.contracts.sharedaccount.SharedRole;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.financeservice.security.SecurityUtils;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseDto;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseRequest;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseResponse;
import com.finovara.financeservice.sharedaccount.expense.service.SharedExpenseFilteringService;
import com.finovara.financeservice.sharedaccount.expense.service.SharedExpenseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/shared-accounts/transactions/expense")
@RequiredArgsConstructor
public class SharedExpenseController {

    private final SharedExpenseService expenseService;
    private final SharedExpenseFilteringService expenseFilteringService;

    @PostMapping
    public ResponseEntity<SharedExpenseResponse> addSharedExpense(@RequestBody @Valid SharedExpenseRequest sharedExpenseRequest) {
        return ResponseEntity.ok(expenseService.addExpense(sharedExpenseRequest, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/edit/{expenseId}")
    public ResponseEntity<Long> editSharedExpense(@RequestBody @Valid SharedExpenseRequest sharedExpenseRequest, @PathVariable Long expenseId) {
        return ResponseEntity.ok(expenseService.editExpense(sharedExpenseRequest, SecurityUtils.getCurrentUserId(), expenseId));
    }

    @GetMapping
    public ResponseEntity<List<SharedExpenseDto>> getSharedExpense(@RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(expenseService.getExpense(SecurityUtils.getCurrentUserId(), page));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<SharedExpenseDto>> filterSharedExpenses(@RequestParam(required = false) ExpenseCategory category, @RequestParam(required = false) BigDecimal amount, @RequestParam(required = false) @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate date, @RequestParam(required = false) String description, @RequestParam(required = false) SharedRole role, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(expenseFilteringService.filterExpenses(SecurityUtils.getCurrentUserId(), category, amount, date, description, role, page));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteSharedExpense(@PathVariable Long expenseId) {
        expenseService.deleteExpense(expenseId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

}
