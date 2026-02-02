package com.finovara.finovarabackend.usersettings.finances.expense.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersettings.finances.expense.dto.ExpenseControlAmountDto;
import com.finovara.finovarabackend.usersettings.finances.expense.service.ExpenseControlAmountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expense-and-revenue/expense-control-amount")
@RequiredArgsConstructor
public class ExpenseControlAmountController {

    private final ExpenseControlAmountService expenseControlAmountService;

    @PutMapping("/{expenseId}")
    public ResponseEntity<Void> addExpenseAmountControl(@PathVariable Long expenseId, @RequestBody ExpenseControlAmountDto expenseControlAmountDto) {
        expenseControlAmountService.addExpenseAmountControl(SecurityUtils.getCurrentUserEmail(), expenseId, expenseControlAmountDto);
        return ResponseEntity.noContent().build();
    }

    @PutMapping
    public ResponseEntity<Void> saveExpenseAmountControl(@RequestBody List<ExpenseControlAmountDto> expenseControlAmountDto) {
        expenseControlAmountService.saveExpenseAmountControl(SecurityUtils.getCurrentUserEmail(), expenseControlAmountDto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<ExpenseControlAmountDto> getExpenseAmountControl(@PathVariable Long expenseId) {
        return ResponseEntity.ok(expenseControlAmountService.getExpenseAmountControl(SecurityUtils.getCurrentUserEmail(), expenseId));
    }

}
