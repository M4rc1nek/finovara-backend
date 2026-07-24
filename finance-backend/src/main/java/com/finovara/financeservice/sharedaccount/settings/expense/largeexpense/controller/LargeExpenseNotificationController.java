package com.finovara.financeservice.sharedaccount.settings.expense.largeexpense.controller;

import com.finovara.financeservice.security.SecurityUtils;
import com.finovara.financeservice.sharedaccount.settings.expense.largeexpense.dto.LargeExpenseNotificationDto;
import com.finovara.financeservice.sharedaccount.settings.expense.largeexpense.service.LargeExpenseNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shared-accounts/settings/large-expense-notification")
@RequiredArgsConstructor
public class LargeExpenseNotificationController {

    private final LargeExpenseNotificationService largeExpenseNotificationService;

    @PatchMapping
    public ResponseEntity<Void> saveLargeExpenseNotificationD(@RequestBody @Valid LargeExpenseNotificationDto largeExpenseNotificationDto) {
        largeExpenseNotificationService.saveLargeExpenseNotification(SecurityUtils.getCurrentUserId(), largeExpenseNotificationDto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<LargeExpenseNotificationDto> getLargeExpenseNotification() {
        return ResponseEntity.ok(largeExpenseNotificationService.getLargeExpenseNotification(SecurityUtils.getCurrentUserId()));
    }

}
