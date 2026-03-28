package com.finovara.finovarabackend.report.finances.highestrevenue.controller;

import com.finovara.finovarabackend.report.finances.highestrevenue.dto.HighestRevenueDto;
import com.finovara.finovarabackend.report.finances.highestrevenue.service.HighestRevenueService;
import com.finovara.finovarabackend.util.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports/highest-revenue")
@RequiredArgsConstructor

public class HighestRevenueController {
    private final HighestRevenueService highestRevenueService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<HighestRevenueDto>> getHighestRevenue(@PathVariable Long userId, @RequestParam PeriodType periodType) {
        return ResponseEntity.ok(highestRevenueService.getHighestRevenue(userId, periodType));
    }
}
