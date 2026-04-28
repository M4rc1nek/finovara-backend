package com.finovara.finovarabackend.usersetting.finances.recurring.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersetting.finances.recurring.dto.RecurringSettingsDto;
import com.finovara.finovarabackend.usersetting.finances.recurring.service.RecurringSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recurring-settings/revenues")
@RequiredArgsConstructor
public class RecurringRevenueController {

    private final RecurringSettingsService recurringSettingsService;

    @PatchMapping
    public ResponseEntity<Void> saveRecurringRevenueSetting(@RequestBody RecurringSettingsDto dto) {
        recurringSettingsService.saveRevenueSettings(SecurityUtils.getCurrentUserId(), dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<RecurringSettingsDto> getRecurringRevenue() {
        return ResponseEntity.ok(recurringSettingsService.getRevenueSettings(SecurityUtils.getCurrentUserId()));
    }

}
