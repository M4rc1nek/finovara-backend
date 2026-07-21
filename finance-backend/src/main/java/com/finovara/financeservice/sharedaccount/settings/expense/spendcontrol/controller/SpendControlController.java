package com.finovara.financeservice.sharedaccount.settings.spendcontrol.controller;

import com.finovara.financeservice.security.SecurityUtils;
import com.finovara.financeservice.sharedaccount.settings.spendcontrol.dto.SpendControlDto;
import com.finovara.financeservice.sharedaccount.settings.spendcontrol.service.SpendControlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shared-accounts/settings/spend-control")
@RequiredArgsConstructor
public class SpendControlController {
    private final SpendControlService spendControlService;

    @PatchMapping
    public ResponseEntity<Void> saveSpendControl(@RequestBody @Valid SpendControlDto spendControlDto) {
        spendControlService.saveSpendControlService(SecurityUtils.getCurrentUserId(), spendControlDto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<SpendControlDto> getSpendControl() {
        return ResponseEntity.ok(spendControlService.getSmartScan(SecurityUtils.getCurrentUserId()));
    }
}
