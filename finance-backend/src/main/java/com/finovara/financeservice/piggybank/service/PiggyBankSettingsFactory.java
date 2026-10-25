package com.finovara.financeservice.piggybank.service;

import com.finovara.financeservice.settings.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.financeservice.settings.piggybank.model.PiggyBankSettings;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PiggyBankSettingsFactory {
    public PiggyBankSettings createDefaultPiggyBankSettings() {
        return PiggyBankSettings.builder()
                .automationActive(false)
                .automationPercentage(BigDecimal.ZERO)
                .roundUpActive(false)
                .goalCompletionStrategy(GoalCompletionStrategy.NONE)
                .build();
    }
}