package com.finovara.financeservice.expense.controller;

import com.finovara.financeservice.expense.dto.ExpenseDto;
import com.finovara.financeservice.expense.dto.ExpenseRequestDto;
import com.finovara.financeservice.expense.service.ExpenseService;
import com.finovara.financeservice.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions/expense")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<Long> addExpense(@RequestBody @Valid ExpenseRequestDto expenseRequestDto) {
        return ResponseEntity.ok(expenseService.addExpense(expenseRequestDto, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/edit/{expenseId}")
    public ResponseEntity<Long> editExpense(@RequestBody @Valid ExpenseRequestDto expenseRequestDto, @PathVariable Long expenseId) {
        return ResponseEntity.ok(expenseService.editExpense(expenseRequestDto, SecurityUtils.getCurrentUserId(), expenseId));
    }

    @GetMapping
    public ResponseEntity<List<ExpenseDto>> getExpense() {
        return ResponseEntity.ok(expenseService.getExpense(SecurityUtils.getCurrentUserId()));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long expenseId) {
        expenseService.deleteExpense(expenseId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

}
