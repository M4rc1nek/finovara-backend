package com.finovara.finovarabackend.usersetting.finances.recurring.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersetting.finances.recurring.dto.RecurringExpenseDto;
import com.finovara.finovarabackend.usersetting.finances.recurring.dto.RecurringRevenueDto;
import com.finovara.finovarabackend.usersetting.finances.recurring.dto.RecurringSavingsDto;
import com.finovara.finovarabackend.usersetting.finances.recurring.service.transaction.RecurringExpenseService;
import com.finovara.finovarabackend.usersetting.finances.recurring.service.transaction.RecurringRevenueService;
import com.finovara.finovarabackend.usersetting.finances.recurring.service.transaction.RecurringSavingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recurring-settings")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringExpenseService recurringExpenseService;
    private final RecurringSavingsService recurringSavingsService;
    private final RecurringRevenueService recurringRevenueService;


    @PatchMapping("/expense")
    public ResponseEntity<Void> saveRecurringExpenseSetting(@RequestBody @Valid RecurringExpenseDto dto) {
        recurringExpenseService.saveExpenseSettings(SecurityUtils.getCurrentUserId(), dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/expense")
    public ResponseEntity<RecurringExpenseDto> getRecurringExpense() {
        return ResponseEntity.ok(recurringExpenseService.getExpenseSettings(SecurityUtils.getCurrentUserId()));
    }

    @PatchMapping("/revenue")
    public ResponseEntity<Void> saveRecurringRevenueSetting(@RequestBody @Valid RecurringRevenueDto dto) {
        recurringRevenueService.saveRevenueSettings(SecurityUtils.getCurrentUserId(), dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/revenue")
    public ResponseEntity<RecurringRevenueDto> getRecurringRevenue() {
        return ResponseEntity.ok(recurringRevenueService.getRevenueSettings(SecurityUtils.getCurrentUserId()));
    }

    @PatchMapping("/savings")
    public ResponseEntity<Void> saveRecurringSavingsSetting(@RequestBody @Valid RecurringSavingsDto dto) {
        recurringSavingsService.saveSavingsSettings(SecurityUtils.getCurrentUserId(), dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/savings")
    public ResponseEntity<RecurringSavingsDto> getRecurringSavings() {
        return ResponseEntity.ok(recurringSavingsService.getSavingsSettings(SecurityUtils.getCurrentUserId()));
    }
}
