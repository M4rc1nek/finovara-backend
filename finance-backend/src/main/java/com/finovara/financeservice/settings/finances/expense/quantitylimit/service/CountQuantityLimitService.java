package com.finovara.financeservice.settings.finances.expense.quantitylimit.service;

import com.finovara.contracts.activity.event.settings.SettingsActivityEvent;
import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.model.activity.SettingType;
import com.finovara.financeservice.exception.conflict.QuantityLimitOperationException;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.settings.finances.expense.quantitylimit.dto.CountQuantityLimitDto;
import com.finovara.financeservice.settings.finances.expense.quantitylimit.validator.CountQuantityLimitValidator;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.expense.repository.ExpenseSettingsRepository;
import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.model.PeriodType;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CountQuantityLimitService {

    private final ExpenseSettingsRepository expenseSettingsRepository;
    private final ExpenseRepository expenseRepository;
    private final AuthBackendClient authBackendClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CountQuantityLimitValidator countQuantityLimitValidator;
    private final AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Transactional
    public void saveCountQuantityLimit(Long userId, CountQuantityLimitDto dto) {
        ExpenseSettings expenseSettings = expenseSettingsRepository.findByUserId(userId);
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(dto.authorizationCode()));
        expenseSettings.setCountQuantityLimitEnabled(dto.expenseCountLimitEnabled());

        createActivity(userId, dto.expenseCountLimitEnabled());

        if (!dto.expenseCountLimitEnabled()) {
            handleDisable(expenseSettings);
            return;
        }
        long countedExpenses = countExpensesInPeriod(userId, dto.periodType());
        if (dto.numberOfQuantityLimit() < countedExpenses) {
            throw new QuantityLimitOperationException("You cannot add a limit " + dto.numberOfQuantityLimit() + ", because you have already " + countedExpenses + " expenses in that period");
        }

        if (expenseSettings.getPeriodType() != dto.periodType()) {
            expenseSettings.setQuantityLimitEmergencyModeUsed(false);
        }

        expenseSettings.setNumberOfQuantityLimit(dto.numberOfQuantityLimit());
        expenseSettings.setPeriodType(dto.periodType());

    }

    @Transactional
    public void handleExpenseLimitExceeded(Long userId, CountQuantityLimitDto dto, PeriodType periodType, ConfirmPasswordDto confirmPasswordDto) {
        ExpenseSettings expenseSettings = expenseSettingsRepository.findByUserId(userId);

        if (!expenseSettings.isCountQuantityLimitEnabled()) return;

        long countedExpenses = countExpensesInPeriod(userId, periodType);
        if (countedExpenses + 1 > dto.numberOfQuantityLimit()) {

            countQuantityLimitValidator.validateEmergencyMode(countedExpenses, confirmPasswordDto, expenseSettings);

            authBackendClient.verifyPassword(userId, confirmPasswordDto);

            expenseSettings.setQuantityLimitEmergencyModeEnabled(false);
            expenseSettings.setQuantityLimitEmergencyModeUsed(true);
        }
    }

    @Transactional
    public CountQuantityLimitDto getCountQuantityLimit(Long userId) {
        ExpenseSettings expenseSettings = expenseSettingsRepository.findByUserId(userId);

        return new CountQuantityLimitDto(expenseSettings.isCountQuantityLimitEnabled(), expenseSettings.getPeriodType(), expenseSettings.getNumberOfQuantityLimit(), null);
    }

    private long countExpensesInPeriod(Long userId, PeriodType periodType) {
        LocalDate today = LocalDate.now();
        LocalDate start = periodType.getStartDate(today);

        return expenseRepository.countExpensesByUserIdAndCreatedAtBetween(userId, start, today);
    }

    private void createActivity(Long userId, boolean enabled) {
        kafkaTemplate.send("activity.settings", new SettingsActivityEvent(userId, SettingType.EXPENSE_COUNT_LIMIT, enabled ? SettingActivityStatus.ENABLED : SettingActivityStatus.DISABLED, LocalDateTime.now()));
    }

    private void handleDisable(ExpenseSettings settings) {
        settings.setQuantityLimitEmergencyModeUsed(false);
    }

}
