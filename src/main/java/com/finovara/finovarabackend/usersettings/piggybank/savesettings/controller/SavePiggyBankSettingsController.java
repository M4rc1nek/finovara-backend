package com.finovara.finovarabackend.usersettings.piggybank.savesettings;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersettings.piggybank.autopayments.dto.AutoPaymentsDto;
import com.finovara.finovarabackend.usersettings.piggybank.autopayments.service.AutoPaymentsService;
import com.finovara.finovarabackend.usersettings.piggybank.completion.dto.GoalCompletionDto;
import com.finovara.finovarabackend.usersettings.piggybank.completion.service.GoalCompletionService;
import com.finovara.finovarabackend.usersettings.piggybank.roundup.dto.RoundUpDto;
import com.finovara.finovarabackend.usersettings.piggybank.roundup.service.RoundUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SavePiggyBankSettingsController {
    private final AutoPaymentsService autoPaymentsService;
    private final RoundUpService roundUpService;
    private final GoalCompletionService goalCompletionService;

    @PutMapping
    public ResponseEntity<Void> savePiggyBankSettings(@RequestBody List<AutoPaymentsDto> autoPaymentsDto,
                                                      @RequestBody List<RoundUpDto> roundUpDto,
                                                      @RequestBody List<GoalCompletionDto> goalCompletionDto) {
        autoPaymentsService.saveAutoPaymentsPiggyBank(SecurityUtils.getCurrentUserEmail(), autoPaymentsDto);
        roundUpService.saveRoundUpPiggyBank(SecurityUtils.getCurrentUserEmail(), roundUpDto);
        goalCompletionService.saveGoalCompletion(SecurityUtils.getCurrentUserEmail(), goalCompletionDto);

        return ResponseEntity.noContent().build();
    }

}
