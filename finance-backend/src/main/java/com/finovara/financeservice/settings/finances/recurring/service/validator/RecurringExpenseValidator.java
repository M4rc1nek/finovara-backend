package com.finovara.financeservice.settings.finances.recurring.service.validator;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.expense.smartscan.dto.SmartScanMode;
import com.finovara.financeservice.exception.conflict.SmartScanConfirmationRequiredException;
import com.finovara.financeservice.settings.finances.expense.smartscan.service.SmartScanService;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.service.validator.util.RecurringBasicValidator;
import com.finovara.financeservice.util.periodbalance.FinancialPeriodService;
import com.finovara.financeservice.wallet.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringExpenseValidator {

    private static final int MAX_ITERATIONS = 100;

    private final SmartScanService smartScanService;
    private final RecurringBasicValidator recurringBasicValidator;
    private final FinancialPeriodService financialPeriodService;

    public void validate(RecurringSettings settings, ExpenseSettings expenseSettings, Wallet wallet, List<Limit> limits) {
        recurringBasicValidator.validateBasics(settings, settings.getExpenseCategory());

        validateBalance(settings, wallet);
        validateQuantityLimit(settings, expenseSettings);
        validateAmountThreshold(settings, expenseSettings);
        validateSmartScan(settings, expenseSettings);
        validateLimits(settings, limits);
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
            smartScanService.handleSmartScan(settings.getUserId(), null, settings.getAmount(), SmartScanMode.ADD);
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

    private void validateLimits(RecurringSettings recurringSettings, List<Limit> limits) {
        limits.forEach(limit -> {
            if (limit.getCategory() != null && !limit.getCategory().equals(recurringSettings.getExpenseCategory())) {
                return;
            }

            BigDecimal spent = financialPeriodService.getExpensesSum(
                    recurringSettings.getUserId(), limit.getPeriodType(), limit.getCategory());

            BigDecimal totalAmount = spent.add(recurringSettings.getAmount());

            if (totalAmount.compareTo(limit.getAmount()) > 0) {
                String msg = limit.getCategory() == null
                        ? "You cannot create this recurring expense because it exceeds your general limit."
                        : "You cannot create this recurring expense because it exceeds the limit for category " + limit.getCategory() + ".";
                throw new InvalidInputException(msg);
            }
        });
    }
}