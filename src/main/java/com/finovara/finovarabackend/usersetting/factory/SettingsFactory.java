package com.finovara.finovarabackend.usersetting.factory;

import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.account.model.AccountSettings;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringType;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.finovarabackend.usersetting.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.util.model.PeriodType;
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
                .userAssigned(user)
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
                        .userAssigned(user)
                        .enable(false)
                        .amount(BigDecimal.ZERO)
                        .type(type)
                        .revenueCategory(null)
                        .expenseCategory(null)
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

    public NotificationEmailSettings createDefaultNotificationSettings(User user) {
        return NotificationEmailSettings.builder()
                .userAssigned(user)
                .notifyOnPasswordChange(false)
                .notifyOnUsernameChange(false)
                .notifyOnAccountDeleted(false)
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
