package com.finovara.finovarabackend.usersetting.finances.revenue.recurring.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersetting.finances.revenue.recurring.dto.RecurringRevenueDto;
import com.finovara.finovarabackend.usersetting.finances.revenue.recurring.service.RecurringRevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/revenue-settings/recurring-revenues")
@RequiredArgsConstructor
public class RecurringRevenueController {

    private final RecurringRevenueService recurringRevenueService;

    @PatchMapping
    public ResponseEntity<Void> saveRecurringRevenueSetting(@RequestBody RecurringRevenueDto dto) {
        recurringRevenueService.saveRecurringRevenue(SecurityUtils.getCurrentUserId(), dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<RecurringRevenueDto> getRecurringRevenue() {
        return ResponseEntity.ok(recurringRevenueService.getRecurringRevenue(SecurityUtils.getCurrentUserId()));
    }

}
