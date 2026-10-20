package com.finovara.financeservice.settings.factory;

import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.RecurringType;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.expense.repository.ExpenseSettingsRepository;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinanceSettingsFactory {

    private final ExpenseSettingsRepository expenseSettingsRepository;
    private final RecurringSettingsRepository recurringSettingsRepository;

    @Transactional
    public void createDefaultSettingsIfNotExist(Long userId) {
        createExpenseSettingsIfMissing(userId);
        createRecurringSettingsIfMissing(userId);
    }

    @Transactional
    public void deleteSettingsIfExist(Long userId) {
        if (recurringSettingsExist(userId) && expenseSettingsRepository.existsByUserId(userId)) {
            recurringSettingsRepository.deleteAllByUserId(userId);
            expenseSettingsRepository.deleteByUserId(userId);
        }
    }

    private void createExpenseSettingsIfMissing(Long userId) {
        if (expenseSettingsRepository.existsByUserId(userId)) {
            log.debug("Expense settings already exist for userId={}, skipping", userId);
            return;
        }
        expenseSettingsRepository.save(ExpenseSettings.builder()
                .userId(userId)
                .amountThresholdEnabled(false)
                .blockedAmount(BigDecimal.ZERO)
                .smartScanEnabled(false)
                .countQuantityLimitEnabled(false)
                .numberOfQuantityLimit(1)
                .periodType(PeriodType.DAILY)
                .quantityLimitEmergencyModeEnabled(false)
                .build());
        log.info("Expense settings created for userId={}", userId);
    }

    private void createRecurringSettingsIfMissing(Long userId) {
        Arrays.stream(RecurringType.values()).forEach(type -> {
            if (recurringSettingsRepository.existsByUserIdAndType(userId, type)) {
                log.debug("Recurring settings already exist for userId={}, type={}, skipping", userId, type);
                return;
            }

            recurringSettingsRepository.save(RecurringSettings.builder()
                    .userId(userId)
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
                    .build());

            log.info("Recurring settings created for userId={}, type={}", userId, type);
        });
    }

    private boolean recurringSettingsExist(Long userId) {
        return Arrays.stream(RecurringType.values())
                .anyMatch(type ->
                        recurringSettingsRepository.existsByUserIdAndType(userId, type));
    }
}
