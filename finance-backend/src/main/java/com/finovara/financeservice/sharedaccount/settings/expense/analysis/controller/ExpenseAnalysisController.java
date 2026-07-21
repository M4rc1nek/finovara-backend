package com.finovara.financeservice.sharedaccount.settings.expense.analysis.controller;

import com.finovara.financeservice.security.SecurityUtils;
import com.finovara.financeservice.sharedaccount.settings.expense.analysis.dto.SmartScanDto;
import com.finovara.financeservice.sharedaccount.settings.expense.analysis.service.ExpenseAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/expense-settings/smart-scan")
@RequiredArgsConstructor
public class SmartScanController {

    private final ExpenseAnalysisService expenseAnalysisService;

    @PatchMapping
    public ResponseEntity<Void> saveSmartScan(@RequestBody SmartScanDto smartScanDto) {
        expenseAnalysisService.saveSmartScan(SecurityUtils.getCurrentUserId(), smartScanDto);
        return ResponseEntity.noContent().build();
    }
    @GetMapping

    public ResponseEntity<SmartScanDto> getSmartScan() {
        return ResponseEntity.ok(expenseAnalysisService.getSmartScan(SecurityUtils.getCurrentUserId()));
    }

}
