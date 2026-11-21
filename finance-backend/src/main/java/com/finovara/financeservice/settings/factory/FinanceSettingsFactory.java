package com.finovara.financeservice.settings.factory;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        List<RecurringSettings> recurringSettings = recurringSettingsRepository.findAllByUserId(userId);
        if (!recurringSettings.isEmpty() && expenseSettingsRepository.existsByUserId(userId)) {
            ExpenseSettings expenseSettings = expenseSettingsRepository.findByUserId(userId)
                    .orElseThrow(()-> new RequestedEntityNotFoundException("Expense settings not found for userId = " + userId));
            recurringSettingsRepository.deleteAll(recurringSettings);
            expenseSettingsRepository.delete(expenseSettings);
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
        Set<RecurringType> existingTypes = recurringSettingsRepository.findAllByUserId(userId).stream()
                .map(RecurringSettings::getType)
                .collect(Collectors.toSet());

        List<RecurringSettings> missingSettings = Arrays.stream(RecurringType.values())
                .filter(type -> !existingTypes.contains(type))
                .map(type -> RecurringSettings.builder()
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
                    .build())
                .toList();

        if (!missingSettings.isEmpty()) {
            recurringSettingsRepository.saveAll(missingSettings);
            missingSettings.forEach(settings ->
                    log.info("Recurring settings created for userId={}, type={}", userId, settings.getType()));
        }
    }
}
