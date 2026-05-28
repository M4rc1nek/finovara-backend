package com.finovara.corebackend.expensehistory.controller;

import com.finovara.corebackend.expense.dto.ExpenseDto;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.corebackend.expensehistory.service.ExpenseHistoryService;
import com.finovara.corebackend.security.SecurityUtils;
import com.finovara.contracts.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/expense-history")
@RequiredArgsConstructor
public class ExpenseHistoryController {

    private final ExpenseHistoryService expenseHistoryService;

    @GetMapping
    public ResponseEntity<List<ExpenseDto>> getExpenseHistory(@RequestParam PeriodType periodType, @RequestParam ExpenseCategory category) {
        return ResponseEntity.ok(expenseHistoryService.getExpenseByCategory(SecurityUtils.getCurrentUserId(), periodType, category));
    }

}
