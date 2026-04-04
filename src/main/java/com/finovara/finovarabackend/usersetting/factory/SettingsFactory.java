package com.finovara.finovarabackend.usersetting.factory;

import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.usersetting.finances.revenue.model.RevenueSettings;
import com.finovara.finovarabackend.usersetting.notification.model.NotificationSettings;
import com.finovara.finovarabackend.usersetting.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.util.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

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
                .numberOfQuantityLimit(0)
                .periodType(PeriodType.DAILY)
                .quantityLimitEmergencyModeEnabled(false)
                .build();
    }

    public RevenueSettings createDefaultRevenueSettings(User user) {
        return RevenueSettings.builder()
                .userAssigned(user)
                .recurringRevenuesEnable(false)
                .recurringAmount(BigDecimal.ZERO)
                .revenueCategory(RevenueCategory.OTHER)
                .periodType(PeriodType.MONTHLY)
                .recurringStartDate(null)
                .nextExecutionDate(null)
                .createdAt(LocalDate.now())
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

    public NotificationSettings createDefaultNotificationSettings(User user) {
        return NotificationSettings.builder()
                .userAssigned(user)
                .notifyOnPasswordChange(false)
                .notifyOnUsernameChange(false)
                .notifyOnAccountDeleted(false)
                .build();
    }

}
