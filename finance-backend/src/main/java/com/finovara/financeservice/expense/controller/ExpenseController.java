package com.finovara.financeservice.expense.controller;

import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.financeservice.expense.dto.ExpenseDto;
import com.finovara.financeservice.expense.dto.ExpenseRequestDto;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.service.ExpenseFilteringService;
import com.finovara.financeservice.expense.service.ExpenseService;
import com.finovara.financeservice.security.SecurityUtils;
import com.finovara.financeservice.util.transaction.TransactionOrigin;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/transactions/expense")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ExpenseFilteringService expenseFilteringService;

    @PostMapping
    public ResponseEntity<Long> addExpense(@RequestBody @Valid ExpenseRequestDto expenseRequestDto, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(expenseService.addExpense(expenseRequestDto, SecurityUtils.getCurrentUserId(), servletRequest, TransactionOrigin.USER_MANUAL));
    }

    @PutMapping("/edit/{expenseId}")
    public ResponseEntity<Long> editExpense(@RequestBody @Valid ExpenseRequestDto expenseRequestDto, @PathVariable Long expenseId, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(expenseService.editExpense(expenseRequestDto, SecurityUtils.getCurrentUserId(), expenseId, servletRequest));
    }

    @GetMapping
    public ResponseEntity<List<ExpenseDto>> getExpense(@RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(expenseService.getExpense(SecurityUtils.getCurrentUserId(), page));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<Expense>> filterExpenses(@RequestParam(required = false) ExpenseCategory category, @RequestParam(required = false) BigDecimal amount,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestParam(required = false) String description, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(expenseFilteringService.expenseFiltering(SecurityUtils.getCurrentUserId(), category, amount, date, description, page));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long expenseId, @RequestParam(required = false) String authorizationCode) {
        expenseService.deleteExpense(expenseId, SecurityUtils.getCurrentUserId(), authorizationCode);
        return ResponseEntity.noContent().build();
    }

}



