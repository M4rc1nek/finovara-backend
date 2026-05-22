package com.finovara.finovarabackend.usersetting.finances.expense.smartscan.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersetting.finances.expense.smartscan.dto.SmartScanDto;
import com.finovara.finovarabackend.usersetting.finances.expense.smartscan.service.SmartScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/expense-settings/smart-scan")
@RequiredArgsConstructor
public class SmartScanController {

    private final SmartScanService smartScanService;

    @PatchMapping
    public ResponseEntity<Void> saveSmartScan(@RequestBody SmartScanDto smartScanDto) {
        smartScanService.saveSmartScan(SecurityUtils.getCurrentUserId(), smartScanDto);
        return ResponseEntity.noContent().build();
    }
    @GetMapping

    public ResponseEntity<SmartScanDto> getSmartScan() {
        return ResponseEntity.ok(smartScanService.getSmartScan(SecurityUtils.getCurrentUserId()));
    }

}
