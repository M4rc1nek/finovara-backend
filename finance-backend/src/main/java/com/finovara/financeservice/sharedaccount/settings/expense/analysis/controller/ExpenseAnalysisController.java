package com.finovara.financeservice.sharedaccount.settings.expense.analysis.controller;

import com.finovara.financeservice.security.SecurityUtils;
import com.finovara.financeservice.sharedaccount.settings.expense.analysis.dto.ExpenseAnalysisDto;
import com.finovara.financeservice.sharedaccount.settings.expense.analysis.service.ExpenseAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/shared-accounts/settings/expense-analysis")
@RequiredArgsConstructor
public class ExpenseAnalysisController {

    private final ExpenseAnalysisService expenseAnalysisService;

    @PatchMapping
    public ResponseEntity<Void> saveSmartScan(@RequestBody ExpenseAnalysisDto expenseAnalysisDto) {
        expenseAnalysisService.saveExpenseAnalysis(SecurityUtils.getCurrentUserId(), expenseAnalysisDto);
        return ResponseEntity.noContent().build();
    }
    @GetMapping

    public ResponseEntity<ExpenseAnalysisDto> getSmartScan() {
        return ResponseEntity.ok(expenseAnalysisService.getExpenseAnalysis(SecurityUtils.getCurrentUserId()));
    }

}
