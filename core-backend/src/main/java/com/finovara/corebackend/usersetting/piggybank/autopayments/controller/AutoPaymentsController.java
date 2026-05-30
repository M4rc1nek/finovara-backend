package com.finovara.corebackend.usersetting.piggybank.autopayments.controller;

import com.finovara.corebackend.security.SecurityUtils;
import com.finovara.corebackend.usersetting.piggybank.autopayments.dto.AutoPaymentsDto;
import com.finovara.corebackend.usersetting.piggybank.autopayments.service.AutoPaymentsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/piggybank-settings/auto-payment")
@RequiredArgsConstructor
public class AutoPaymentsController {

    private final AutoPaymentsService autoPaymentsService;

    @PutMapping("/{piggyBankId}")
    public ResponseEntity<Void> createAutomation(@RequestBody @Valid AutoPaymentsDto autoPaymentsDto, @PathVariable Long piggyBankId) {
        autoPaymentsService.createAutomation(SecurityUtils.getCurrentUserId(), piggyBankId, autoPaymentsDto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{piggyBankId}")
    public ResponseEntity<AutoPaymentsDto> getAutomation(@PathVariable Long piggyBankId) {
        return ResponseEntity.ok(autoPaymentsService.getAutomation(SecurityUtils.getCurrentUserId(), piggyBankId));
    }

    @PatchMapping("/{piggyBankId}")
    public ResponseEntity<Void> saveAutoPaymentsPiggyBank(@RequestBody @Valid AutoPaymentsDto autoPaymentsDto, @PathVariable Long piggyBankId) {
        autoPaymentsService.saveAutoPaymentsPiggyBank(SecurityUtils.getCurrentUserId(), piggyBankId, autoPaymentsDto);
        return ResponseEntity.noContent().build();
    }

}
