package com.finovara.finovarabackend.usersettings.finances.expense.countlimit.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersettings.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.finovarabackend.usersettings.finances.expense.countlimit.dto.CountQuantityLimitEmergencyModeDto;
import com.finovara.finovarabackend.usersettings.finances.expense.countlimit.service.CountQuantityLimitEmergencyModeService;
import com.finovara.finovarabackend.usersettings.finances.expense.countlimit.service.CountQuantityLimitService;
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

    @PutMapping
    public ResponseEntity<Void> saveCountQuantityExpenseLimit(@RequestBody @Valid CountQuantityLimitDto countQuantityLimitDto) {
        countQuantityLimitService.saveCountQuantityLimit(SecurityUtils.getCurrentUserEmail(), countQuantityLimitDto);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/emergency-mode")
    public ResponseEntity<Void> saveCountQuantityExpenseEmergencyMode(@RequestBody CountQuantityLimitEmergencyModeDto countQuantityLimitEmergencyModeDto) {
        countQuantityLimitEmergencyModeService.saveEmergencyMode(SecurityUtils.getCurrentUserEmail(), countQuantityLimitEmergencyModeDto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<CountQuantityLimitDto> getCountQuantityExpenseLimit() {
        return ResponseEntity.ok(countQuantityLimitService.getCountQuantityLimit(SecurityUtils.getCurrentUserEmail()));
    }
}
