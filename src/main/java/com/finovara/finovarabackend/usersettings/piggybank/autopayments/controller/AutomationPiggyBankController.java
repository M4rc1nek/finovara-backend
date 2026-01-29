package com.finovara.finovarabackend.usersettings.piggybank.autopayments.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersettings.piggybank.autopayments.dto.AutomationPiggyBankDto;
import com.finovara.finovarabackend.usersettings.piggybank.autopayments.service.AutomationPiggyBankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance-autopayment")
@RequiredArgsConstructor
public class AutomationPiggyBankController {

    private final AutomationPiggyBankService automationPiggyBankService;

    @PutMapping("/{piggyBankId}")
    public ResponseEntity<Void> createAutomation(@PathVariable Long piggyBankId, @Valid @RequestBody AutomationPiggyBankDto automationPiggyBankDto) {
        automationPiggyBankService.createAutomation(SecurityUtils.getCurrentUserEmail(), piggyBankId, automationPiggyBankDto);
        return ResponseEntity.noContent().build();
    }

    @PutMapping
    public ResponseEntity<Void> updateRoundUpSettings(@RequestBody List<AutomationPiggyBankDto> settings) {
        automationPiggyBankService.updatePiggyBank(SecurityUtils.getCurrentUserEmail(), settings);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{piggyBankId}")
    public ResponseEntity<AutomationPiggyBankDto> getAutomation(@PathVariable Long piggyBankId) {
        return ResponseEntity.ok(automationPiggyBankService.getAutomation(SecurityUtils.getCurrentUserEmail(), piggyBankId));
    }

}
