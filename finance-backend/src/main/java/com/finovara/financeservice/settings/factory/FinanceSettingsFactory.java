package com.finovara.financeservice.settings.factory;

import com.finovara.contracts.model.PeriodType;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.expense.repository.ExpenseSettingsRepository;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.contracts.model.RecurringType;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
    public void createDefaultExpenseSettingsIfNotExist(Long userId) {
        try {
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
        } catch (DataIntegrityViolationException e) {
            log.debug("Expense settings already exist for userId={}, skipping", userId);
        }
    }

    @Transactional
    public void createDefaultRecurringSettingsIfNotExist(Long userId) {
        Arrays.stream(RecurringType.values()).forEach(type -> {
            if (recurringSettingsRepository.findByUserIdAndType(userId, type).isPresent()) {
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
}