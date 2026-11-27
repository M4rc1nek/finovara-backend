package com.finovara.financeservice.internal.report.mainaccount;

import com.finovara.contracts.mainaccount.report.savingsplan.dto.SavingsPlanMonthDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/savings-plan")
@RequiredArgsConstructor
public class InternalSavingsPlanController {

    private final InternalReportDataService internalReportDataService;

    @GetMapping("/monthly-data")
    public ResponseEntity<List<SavingsPlanMonthDto>> monthlyData(@RequestParam Long userId) {
        return ResponseEntity.ok(internalReportDataService.monthlyData(userId));
    }
}
