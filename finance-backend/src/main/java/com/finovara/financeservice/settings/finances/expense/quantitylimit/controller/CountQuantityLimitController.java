package com.finovara.financeservice.settings.finances.expense.quantitylimit.controller;

import com.finovara.financeservice.security.SecurityUtils;
import com.finovara.financeservice.settings.finances.expense.quantitylimit.dto.CountQuantityLimitDto;
import com.finovara.financeservice.settings.finances.expense.quantitylimit.dto.CountQuantityLimitEmergencyModeDto;
import com.finovara.financeservice.settings.finances.expense.quantitylimit.service.CountQuantityLimitEmergencyModeService;
import com.finovara.financeservice.settings.finances.expense.quantitylimit.service.CountQuantityLimitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expense-settings/count-quantity-expense")
@RequiredArgsConstructor
public class CountQuantityLimitController {

    private final CountQuantityLimitService countQuantityLimitService;
    private final CountQuantityLimitEmergencyModeService countQuantityLimitEmergencyModeService;

    @PatchMapping
    public ResponseEntity<Void> saveCountQuantityExpenseLimit(@RequestBody @Valid CountQuantityLimitDto countQuantityLimitDto) {
        countQuantityLimitService.saveCountQuantityLimit(SecurityUtils.getCurrentUserId(), countQuantityLimitDto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/emergency-mode")
    public ResponseEntity<Void> saveCountQuantityExpenseEmergencyMode(@RequestBody CountQuantityLimitEmergencyModeDto countQuantityLimitEmergencyModeDto) {
        countQuantityLimitEmergencyModeService.saveEmergencyMode(SecurityUtils.getCurrentUserId(), countQuantityLimitEmergencyModeDto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<CountQuantityLimitDto> getCountQuantityExpenseLimit() {
        return ResponseEntity.ok(countQuantityLimitService.getCountQuantityLimit(SecurityUtils.getCurrentUserId()));
    }
}
