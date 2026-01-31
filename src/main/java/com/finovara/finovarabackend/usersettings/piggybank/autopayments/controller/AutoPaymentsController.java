package com.finovara.finovarabackend.usersettings.piggybank.autopayments.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersettings.piggybank.autopayments.dto.AutoPaymentsDto;
import com.finovara.finovarabackend.usersettings.piggybank.autopayments.service.AutoPaymentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/piggybank-settings/auto-payment")
@RequiredArgsConstructor
public class AutoPaymentsController {

    private final AutoPaymentsService autoPaymentsService;

    @PutMapping("/{piggyBankId}")
    public ResponseEntity<Void> createAutomation(@PathVariable Long piggyBankId, @RequestBody AutoPaymentsDto autoPaymentsDto) {
        autoPaymentsService.createAutomation(SecurityUtils.getCurrentUserEmail(), piggyBankId, autoPaymentsDto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{piggyBankId}")
    public ResponseEntity<AutoPaymentsDto> getAutomation(@PathVariable Long piggyBankId) {
        return ResponseEntity.ok(autoPaymentsService.getAutomation(SecurityUtils.getCurrentUserEmail(), piggyBankId));
    }

}
