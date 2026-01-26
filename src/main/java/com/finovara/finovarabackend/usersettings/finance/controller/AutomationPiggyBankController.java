package com.finovara.finovarabackend.usersettings.finance.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersettings.finance.dto.AutomationPiggyBankDto;
import com.finovara.finovarabackend.usersettings.finance.service.AutomationPiggyBankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance-settings")
@RequiredArgsConstructor
public class AutomationPiggyBankController {

    private final AutomationPiggyBankService automationPiggyBankService;

    @PutMapping("/{piggyBankId}")
    public ResponseEntity<Void> createAutomation(@PathVariable Long piggyBankId, @Valid @RequestBody AutomationPiggyBankDto automationPiggyBankDto) {
        automationPiggyBankService.createAutomation(SecurityUtils.getCurrentUserEmail(), piggyBankId, automationPiggyBankDto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{piggyBankId}")
    public ResponseEntity<AutomationPiggyBankDto> getAutomation(@PathVariable Long piggyBankId) {
        return ResponseEntity.ok(automationPiggyBankService.getAutomation(SecurityUtils.getCurrentUserEmail(), piggyBankId));
    }

}
