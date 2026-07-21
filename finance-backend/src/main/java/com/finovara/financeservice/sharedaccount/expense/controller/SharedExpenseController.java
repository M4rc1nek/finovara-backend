package com.finovara.financeservice.sharedaccount.expense.controller;

import com.finovara.financeservice.security.SecurityUtils;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseDto;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseRequest;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseResponse;
import com.finovara.financeservice.sharedaccount.expense.service.SharedExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shared-accounts/transactions/expense")
@RequiredArgsConstructor
public class SharedExpenseController {

    private final SharedExpenseService expenseService;

    @PostMapping
    public ResponseEntity<SharedExpenseResponse> addSharedExpense(@RequestBody @Valid SharedExpenseRequest sharedExpenseRequest) {
        return ResponseEntity.ok(expenseService.addExpense(sharedExpenseRequest, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/edit/{expenseId}")
    public ResponseEntity<Long> editSharedExpense(@RequestBody @Valid SharedExpenseRequest sharedExpenseRequest, @PathVariable Long expenseId) {
        return ResponseEntity.ok(expenseService.editExpense(sharedExpenseRequest, SecurityUtils.getCurrentUserId(), expenseId));
    }

    @GetMapping
    public ResponseEntity<List<SharedExpenseDto>> getSharedExpense() {
        return ResponseEntity.ok(expenseService.getExpense(SecurityUtils.getCurrentUserId()));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteSharedExpense(@PathVariable Long expenseId) {
        expenseService.deleteExpense(expenseId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

}
