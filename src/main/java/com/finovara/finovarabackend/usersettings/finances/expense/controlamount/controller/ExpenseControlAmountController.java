package com.finovara.finovarabackend.usersettings.finances.expense.controlamount.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersettings.finances.expense.controlamount.dto.ExpenseControlAmountDto;
import com.finovara.finovarabackend.usersettings.finances.expense.controlamount.service.ExpenseControlAmountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expense-and-revenue/expense-control-amount")
@RequiredArgsConstructor
public class ExpenseControlAmountController {

    private final ExpenseControlAmountService expenseControlAmountService;

    @PutMapping
    public ResponseEntity<Void> saveExpenseAmountControl(@RequestBody ExpenseControlAmountDto expenseControlAmountDto) {
        expenseControlAmountService.saveExpenseAmountControl(SecurityUtils.getCurrentUserEmail(), expenseControlAmountDto);
        return ResponseEntity.noContent().build();
    }
    @GetMapping
    public ResponseEntity<ExpenseControlAmountDto> getExpenseAmountControl() {
        return ResponseEntity.ok(expenseControlAmountService.getExpenseAmountControl(SecurityUtils.getCurrentUserEmail()));
    }

}
