package com.finovara.authbackend.usersetting.factory;

import com.finovara.authbackend.piggybank.model.PiggyBank;
import com.finovara.authbackend.user.model.User;
import com.finovara.authbackend.usersetting.account.model.AccountSettings;
import com.finovara.authbackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.authbackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.authbackend.usersetting.finances.recurring.model.RecurringType;
import com.finovara.authbackend.usersetting.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.authbackend.usersetting.piggybank.model.PiggyBankSettings;
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

    public AccountSettings createDefaultAccountSettings(User user) {
        return AccountSettings.builder()
                .userAssigned(user)
                .emailChangeCode(null)
                .pendingEmail(null)
                .resetPasswordCode(null)
                .resetPasswordCodeExpiresAt(null)
                .attemptsEmailExpiresAt(null)
                .attemptsPasswordExpiresAt(null)
                .build();
    }

}
