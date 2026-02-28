package com.finovara.finovarabackend.usersetting.factory;

import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.model.CountQuantityLimitStrategy;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.usersetting.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
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

    public PiggyBankSettings createDefaultPiggyBankSettings(PiggyBank piggyBank) {
        return PiggyBankSettings.builder()
                .piggyBankAssigned(piggyBank)
                .automationActive(false)
                .automationPercentage(BigDecimal.ZERO)
                .roundUpActive(false)
                .goalCompletionStrategy(GoalCompletionStrategy.NONE)
                .build();
    }
}
