package com.finovara.financeservice.expense.controller;

import com.finovara.financeservice.expense.dto.ExpenseDto;
import com.finovara.financeservice.expense.dto.ExpenseRequestDto;
import com.finovara.financeservice.expense.service.ExpenseService;
import com.finovara.contracts.model.PeriodType;
import com.finovara.financeservice.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping("/addExpense")
    public ResponseEntity<Long> addExpense(@RequestBody @Valid ExpenseRequestDto expenseRequestDto, @RequestParam(required = false) PeriodType periodType) {
        return ResponseEntity.ok(expenseService.addExpense(expenseRequestDto, SecurityUtils.getCurrentUserId(), periodType));
    }

    @PutMapping("/editExpense/{expenseId}")
    public ResponseEntity<Long> editExpense(@RequestBody @Valid ExpenseRequestDto expenseRequestDto, @PathVariable Long expenseId, @RequestParam(required = false) PeriodType periodType) {
        return ResponseEntity.ok(expenseService.editExpense(expenseRequestDto, SecurityUtils.getCurrentUserId(), expenseId, periodType));
    }

    @DeleteMapping("/deleteExpense/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long expenseId) {
        expenseService.deleteExpense(expenseId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/getExpense")
    public ResponseEntity<List<ExpenseDto>> getExpense() {
        return ResponseEntity.ok(expenseService.getExpense(SecurityUtils.getCurrentUserId()));
    }

}
