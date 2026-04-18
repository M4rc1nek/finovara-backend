package com.finovara.finovarabackend.expense.controller;

import com.finovara.finovarabackend.expense.dto.ExpenseDto;
import com.finovara.finovarabackend.expense.dto.ExpenseRequestDto;
import com.finovara.finovarabackend.expense.service.ExpenseService;
import com.finovara.finovarabackend.util.model.PeriodType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.finovara.finovarabackend.security.SecurityUtils.getCurrentUserId;

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
