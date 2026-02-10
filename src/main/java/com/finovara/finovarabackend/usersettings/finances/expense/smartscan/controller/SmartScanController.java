package com.finovara.finovarabackend.usersettings.finances.expense.smartscan.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersettings.finances.expense.smartscan.dto.SmartScanDto;
import com.finovara.finovarabackend.usersettings.finances.expense.smartscan.service.SmartScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/expense-settings/smart-scan")
@RequiredArgsConstructor
public class SmartScanController {

    private final SmartScanService smartScanService;

    @PutMapping
    public ResponseEntity<Void> saveSmartScan(@RequestBody SmartScanDto smartScanDto) {
        smartScanService.saveSmartScan(SecurityUtils.getCurrentUserEmail(), smartScanDto);
        return ResponseEntity.noContent().build();
    }
    @GetMapping

    public ResponseEntity<SmartScanDto> getSmartScan() {
        return ResponseEntity.ok(smartScanService.getSmartScan(SecurityUtils.getCurrentUserEmail()));
    }

}
