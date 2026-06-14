package com.finovara.authbackend.expense.controller;

import com.finovara.authbackend.expense.dto.ExpenseDto;
import com.finovara.authbackend.expense.dto.ExpenseRequestDto;
import com.finovara.authbackend.expense.service.ExpenseService;
import com.finovara.contracts.model.PeriodType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.finovara.authbackend.security.SecurityUtils.getCurrentUserId;

@RestController
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping("/addExpense")
    public ResponseEntity<Long> addExpense(@RequestBody @Valid ExpenseRequestDto expenseRequestDto, @RequestParam(required = false) PeriodType periodType) {
        return ResponseEntity.ok(expenseService.addExpense(expenseRequestDto, getCurrentUserId(), periodType));
    }

    @PutMapping("/editExpense/{expenseId}")
    public ResponseEntity<Long> editExpense(@RequestBody @Valid ExpenseRequestDto expenseRequestDto, @PathVariable Long expenseId, @RequestParam(required = false) PeriodType periodType) {
        return ResponseEntity.ok(expenseService.editExpense(expenseRequestDto, getCurrentUserId(), expenseId, periodType));
    }

    @DeleteMapping("/deleteExpense/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long expenseId) {
        expenseService.deleteExpense(expenseId, getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/getExpense")
    public ResponseEntity<List<ExpenseDto>> getExpense() {
        return ResponseEntity.ok(expenseService.getExpense(getCurrentUserId()));
    }

}
