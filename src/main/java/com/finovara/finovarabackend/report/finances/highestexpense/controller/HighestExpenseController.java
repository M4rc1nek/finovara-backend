package com.finovara.finovarabackend.report.finances.highestexpense.controller;

import com.finovara.finovarabackend.report.finances.highestexpense.dto.HighestExpenseDto;
import com.finovara.finovarabackend.report.finances.highestexpense.service.HighestExpenseService;
import com.finovara.finovarabackend.util.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports/highest-expense")
@RequiredArgsConstructor
public class HighestExpenseController {
    private final HighestExpenseService highestExpenseService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<HighestExpenseDto>> getHighestExpense(@PathVariable Long userId, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(highestExpenseService.getHighestExpense(userId, periodType));
    }
}
