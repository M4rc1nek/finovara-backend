package com.finovara.finovarabackend.usersettings.piggybank.savesettings.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersettings.piggybank.autopayments.service.AutoPaymentsService;
import com.finovara.finovarabackend.usersettings.piggybank.completion.service.GoalCompletionService;
import com.finovara.finovarabackend.usersettings.piggybank.roundup.service.RoundUpService;
import com.finovara.finovarabackend.usersettings.piggybank.savesettings.dto.SavePiggyBankSettingsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/piggybank-settings/save-all")
@RequiredArgsConstructor
public class SavePiggyBankSettingsController {
    private final AutoPaymentsService autoPaymentsService;
    private final RoundUpService roundUpService;
    private final GoalCompletionService goalCompletionService;

    @PutMapping
    public ResponseEntity<Void> savePiggyBankSettings(@RequestBody SavePiggyBankSettingsDto request) {
        autoPaymentsService.saveAutoPaymentsPiggyBank(SecurityUtils.getCurrentUserEmail(), request.autoPaymentsDto());
        roundUpService.saveRoundUpPiggyBank(SecurityUtils.getCurrentUserEmail(), request.roundUpDto());
        goalCompletionService.saveGoalCompletion(SecurityUtils.getCurrentUserEmail(), request.goalCompletionDto());

        return ResponseEntity.noContent().build();
    }

}
