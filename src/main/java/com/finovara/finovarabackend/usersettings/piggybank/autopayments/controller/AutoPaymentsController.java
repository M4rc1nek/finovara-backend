package com.finovara.finovarabackend.usersettings.piggybank.autopayments.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersettings.piggybank.autopayments.dto.AutoPaymentsDto;
import com.finovara.finovarabackend.usersettings.piggybank.autopayments.service.AutoPaymentsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance-autopayment")
@RequiredArgsConstructor
public class AutoPaymentsController {

    private final AutoPaymentsService autoPaymentsService;

    @PutMapping("/{piggyBankId}")
    public ResponseEntity<Void> createAutomation(@PathVariable Long piggyBankId, @Valid @RequestBody AutoPaymentsDto autoPaymentsDto) {
        autoPaymentsService.createAutomation(SecurityUtils.getCurrentUserEmail(), piggyBankId, autoPaymentsDto);
        return ResponseEntity.noContent().build();
    }

    @PutMapping
    public ResponseEntity<Void> updateRoundUpSettings(@RequestBody List<AutoPaymentsDto> settings) {
        autoPaymentsService.updatePiggyBank(SecurityUtils.getCurrentUserEmail(), settings);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{piggyBankId}")
    public ResponseEntity<AutoPaymentsDto> getAutomation(@PathVariable Long piggyBankId) {
        return ResponseEntity.ok(autoPaymentsService.getAutomation(SecurityUtils.getCurrentUserEmail(), piggyBankId));
    }

}
