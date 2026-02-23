package com.finovara.finovarabackend.usersetting.finances.expense.savesettings.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersetting.finances.expense.controlamount.service.ControlAmountService;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.service.CountQuantityLimitService;
import com.finovara.finovarabackend.usersetting.finances.expense.savesettings.dto.SaveExpenseSettingsDto;
import com.finovara.finovarabackend.usersetting.finances.expense.smartscan.service.SmartScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/expense-settings/save-all")
@RequiredArgsConstructor
public class SaveExpenseSettingsController {

    private final CountQuantityLimitService countQuantityLimitService;
    private final SmartScanService smartScanService;
    private final ControlAmountService controlAmountService;

    @PutMapping
    public ResponseEntity<Void> saveExpenseSettings(@RequestBody SaveExpenseSettingsDto request) {
        controlAmountService.saveExpenseAmountControl(SecurityUtils.getCurrentUserEmail(), request.controlAmountDto());
        smartScanService.saveSmartScan(SecurityUtils.getCurrentUserEmail(), request.smartScanDto());
        countQuantityLimitService.saveCountQuantityLimit(SecurityUtils.getCurrentUserEmail(), request.countQuantityLimitDto());

        return ResponseEntity.noContent().build();
    }

}

