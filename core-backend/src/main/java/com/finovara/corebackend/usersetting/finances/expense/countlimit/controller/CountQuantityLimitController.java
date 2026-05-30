package com.finovara.corebackend.usersetting.finances.expense.countlimit.controller;

import com.finovara.corebackend.security.SecurityUtils;
import com.finovara.corebackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.corebackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitEmergencyModeDto;
import com.finovara.corebackend.usersetting.finances.expense.countlimit.service.CountQuantityLimitEmergencyModeService;
import com.finovara.corebackend.usersetting.finances.expense.countlimit.service.CountQuantityLimitService;
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
