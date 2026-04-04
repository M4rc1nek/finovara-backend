package com.finovara.finovarabackend.report.finances.highesttransactions.controller;

import com.finovara.finovarabackend.report.finances.highesttransactions.highestexpense.dto.HighestExpenseDto;
import com.finovara.finovarabackend.report.finances.highesttransactions.highestexpense.service.HighestExpenseService;
import com.finovara.finovarabackend.report.finances.highesttransactions.highestrevenue.dto.HighestRevenueDto;
import com.finovara.finovarabackend.report.finances.highesttransactions.highestrevenue.service.HighestRevenueService;
import com.finovara.finovarabackend.util.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports/highest-transaction")
@RequiredArgsConstructor
public class HighestTransactionsController {
    private final HighestExpenseService highestExpenseService;
    private final HighestRevenueService highestRevenueService;


    @GetMapping("/expense/{userId}")
    public ResponseEntity<List<HighestExpenseDto>> getHighestExpense(@PathVariable Long userId, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(highestExpenseService.getHighestExpense(userId, periodType));
    }

    @GetMapping("/revenue/{userId}")
    public ResponseEntity<List<HighestRevenueDto>> getHighestRevenue(@PathVariable Long userId, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(highestRevenueService.getHighestRevenue(userId, periodType));
    }


}
