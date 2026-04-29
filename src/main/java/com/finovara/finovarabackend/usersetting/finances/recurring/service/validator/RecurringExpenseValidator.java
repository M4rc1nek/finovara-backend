package com.finovara.finovarabackend.usersetting.finances.recurring.service.validator;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.usersetting.finances.expense.smartscan.dto.SmartScanMode;
import com.finovara.finovarabackend.usersetting.finances.expense.smartscan.exception.conflict.SmartScanConfirmationRequiredException;
import com.finovara.finovarabackend.usersetting.finances.expense.smartscan.service.SmartScanService;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.finovarabackend.wallet.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RecurringExpenseValidator {
    private final SmartScanService smartScanService;

    public void validate(RecurringSettings recurringSettings, ExpenseSettings expenseSettings, Wallet wallet) {
        if (recurringSettings.getAmount().compareTo(wallet.getBalance()) > 0) {
            throw new InvalidInputException("Insufficient funds");
        }

        if (expenseSettings.isCountQuantityLimitEnabled()) {
            int planned = countPlannedExecutions(recurringSettings, LocalDate.now());
            int limit = expenseSettings.getNumberOfQuantityLimit();

            if (planned > limit) {
                throw new InvalidInputException("Expense count limit exceeded. Limit: " + limit + ", required: " + planned);
            }
        }

        if (expenseSettings.isAmountThresholdEnabled() && recurringSettings.getAmount().compareTo(expenseSettings.getBlockedAmount()) > 0) {
            throw new InvalidInputException("Expense amount exceeds the allowed limit: " + expenseSettings.getBlockedAmount());
        }


        if (expenseSettings.isSmartScanEnabled()) {
            try {
                smartScanService.handleSmartScan(recurringSettings.getUserAssigned().getId(), null, recurringSettings.getAmount(), SmartScanMode.ADD);
            } catch (SmartScanConfirmationRequiredException exception) {
                throw new InvalidInputException("You cannot create this recurring expense because the amount is considered unusual. Try lowering the amount or disable Smart Scan.");
            }
        }

    }

    private int countPlannedExecutions(RecurringSettings settings, LocalDate today) {
        int count = 0;
        int safetyCounter = 0;
        int maxIterations = 100;

        LocalDate nextDate = settings.getNextExecutionDate();

        while (!nextDate.isAfter(today) && safetyCounter++ < maxIterations) {
            count++;
            nextDate = settings.getPeriodType().addPeriod(nextDate);
        }

        return count;
    }

}