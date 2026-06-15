package com.finovara.authservice.settings.factory;

import com.finovara.authservice.piggybank.model.PiggyBank;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.settings.account.model.AccountSettings;
import com.finovara.authservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.authservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.authservice.settings.finances.recurring.model.RecurringType;
import com.finovara.authservice.settings.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.authservice.settings.piggybank.model.PiggyBankSettings;
import com.finovara.contracts.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SettingsFactory {

    public ExpenseSettings createDefaultExpenseSettings(User user) {
        return ExpenseSettings.builder()
                .userId(user.getId())
                .amountThresholdEnabled(false)
                .blockedAmount(BigDecimal.ZERO)
                .smartScanEnabled(false)
                .countQuantityLimitEnabled(false)
                .numberOfQuantityLimit(1)
                .periodType(PeriodType.DAILY)
                .quantityLimitEmergencyModeEnabled(false)
                .build();
    }

    public List<RecurringSettings> createDefaultRecurringSettings(User user) {
        return Arrays.stream(RecurringType.values())
                .map(type -> RecurringSettings.builder()
                        .userId(user.getId())
                        .enable(false)
                        .amount(BigDecimal.ONE)
                        .type(type)
                        .revenueCategory(null)
                        .expenseCategory(null)
                        .piggyBankId(null)
                        .periodType(PeriodType.MONTHLY)
                        .startDate(null)
                        .nextExecutionDate(null)
                        .createdAt(LocalDate.now())
                        .build()
                )
                .toList();
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
