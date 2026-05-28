package com.finovara.corebackend.usersetting.finances.recurring.service.validator;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.corebackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.corebackend.usersetting.finances.expense.smartscan.dto.SmartScanMode;
import com.finovara.corebackend.usersetting.finances.expense.smartscan.exception.conflict.SmartScanConfirmationRequiredException;
import com.finovara.corebackend.usersetting.finances.expense.smartscan.service.SmartScanService;
import com.finovara.corebackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.corebackend.usersetting.finances.recurring.service.validator.util.RecurringBasicValidator;
import com.finovara.corebackend.wallet.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RecurringExpenseValidator {

    private static final int MAX_ITERATIONS = 100;

    private final SmartScanService smartScanService;
    private final RecurringBasicValidator recurringBasicValidator;

    public void validate(RecurringSettings settings, ExpenseSettings expenseSettings, Wallet wallet) {
        recurringBasicValidator.validateBasics(settings, settings.getExpenseCategory());

        validateBalance(settings, wallet);
        validateQuantityLimit(settings, expenseSettings);
        validateAmountThreshold(settings, expenseSettings);
        validateSmartScan(settings, expenseSettings);
    }

    private void validateBalance(RecurringSettings settings, Wallet wallet) {
        BigDecimal amount = settings.getAmount();

        if (amount.compareTo(wallet.getBalance()) > 0) {
            throw new InvalidInputException("Insufficient funds");
        }
    }

    private void validateQuantityLimit(RecurringSettings settings, ExpenseSettings expenseSettings) {
        if (!expenseSettings.isCountQuantityLimitEnabled()) {
            return;
        }

        int planned = countPlannedExecutions(settings, LocalDate.now());
        int limit = expenseSettings.getNumberOfQuantityLimit();

        if (planned > limit) {
            throw new InvalidInputException("Expense count limit exceeded. Limit: " + limit + ", required: " + planned);
        }
    }

    private void validateAmountThreshold(RecurringSettings settings, ExpenseSettings expenseSettings) {
        if (!expenseSettings.isAmountThresholdEnabled()) {
            return;
        }

        BigDecimal amount = settings.getAmount();
        BigDecimal blockedAmount = expenseSettings.getBlockedAmount();

        if (amount.compareTo(blockedAmount) > 0) {
            throw new InvalidInputException("Expense amount exceeds the allowed limit: " + blockedAmount);
        }
    }

    private void validateSmartScan(RecurringSettings settings, ExpenseSettings expenseSettings) {
        if (!expenseSettings.isSmartScanEnabled()) {
            return;
        }

        try {
            smartScanService.handleSmartScan(settings.getUserAssigned().getId(), null, settings.getAmount(), SmartScanMode.ADD);
        } catch (SmartScanConfirmationRequiredException exception) {
            throw new InvalidInputException("You cannot create this recurring expense because the amount is considered unusual. " +
                    "Try lowering the amount or disable Smart Scan.");
        }
    }

    private int countPlannedExecutions(RecurringSettings settings, LocalDate today) {
        int count = 0;
        int iterationGuard = 0;

        LocalDate nextDate = settings.getNextExecutionDate();

        while (!nextDate.isAfter(today) && iterationGuard++ < MAX_ITERATIONS) {
            count++;
            nextDate = settings.getPeriodType().addPeriod(nextDate);
        }

        return count;
    }
}