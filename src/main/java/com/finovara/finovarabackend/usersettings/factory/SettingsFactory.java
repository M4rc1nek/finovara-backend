package com.finovara.finovarabackend.usersettings.factory;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersettings.finances.expense.countlimit.model.CountQuantityLimitStrategy;
import com.finovara.finovarabackend.usersettings.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.usersettings.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.finovarabackend.usersettings.piggybank.model.PiggyBankSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SettingsFactory {

    public ExpenseSettings createDefaultExpenseSettings(User user) {
        return ExpenseSettings.builder()
                .userAssigned(user)
                .expenseAmountThresholdEnabled(false)
                .blockedAmount(BigDecimal.ZERO)
                .smartScanEnabled(false)
                .expenseCountQuantityLimitEnabled(false)
                .numberOfQuantityLimit(0)
                .countQuantityLimitStrategy(CountQuantityLimitStrategy.DAILY)
                .expenseQuantityLimitEmergencyModeEnabled(false)
                .build();

    }

    public PiggyBankSettings createDefaultPiggyBankSettings(User user) {
        return PiggyBankSettings.builder()
                .userAssigned(user)
                .automationActive(false)
                .automationPercentage(BigDecimal.ZERO)
                .roundUpActive(false)
                .goalCompletionStrategy(GoalCompletionStrategy.NONE)
                .build();

    }

}
