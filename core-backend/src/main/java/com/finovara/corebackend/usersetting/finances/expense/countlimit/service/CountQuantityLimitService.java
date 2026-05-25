package com.finovara.corebackend.usersetting.finances.expense.countlimit.service;

import com.finovara.activityservice.contracts.event.settings.SettingsActivityEvent;
import com.finovara.activityservice.contracts.model.activity.SettingActivityStatus;
import com.finovara.activityservice.contracts.model.activity.SettingType;
import com.finovara.corebackend.exception.conflict.StateConflictException;
import com.finovara.corebackend.expense.repository.ExpenseRepository;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.corebackend.usersetting.finances.expense.countlimit.validator.CountQuantityLimitValidator;
import com.finovara.corebackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.corebackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.corebackend.util.confirmationpassword.service.PasswordValidator;
import com.finovara.activityservice.contracts.model.PeriodType;
import com.finovara.corebackend.util.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CountQuantityLimitService {

    private final UserManagerService userManagerService;
    private final ExpenseRepository expenseRepository;
    private final PasswordValidator passwordValidator;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CountQuantityLimitValidator countQuantityLimitValidator;

    @Transactional
    public void saveCountQuantityLimit(Long userId, CountQuantityLimitDto dto) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        expenseSettings.setCountQuantityLimitEnabled(dto.expenseCountLimitEnabled());

        createActivity(userId, dto.expenseCountLimitEnabled());

        if (!dto.expenseCountLimitEnabled()) {
            handleDisable(expenseSettings);
            return;
        }
        long countedExpenses = countExpensesInPeriod(user, dto.periodType());
        if (dto.numberOfQuantityLimit() < countedExpenses) {
            throw new StateConflictException("You cannot add a limit " + dto.numberOfQuantityLimit() + ", because you have already " + countedExpenses + " expenses in that period");
        }

        if (expenseSettings.getPeriodType() != dto.periodType()) {
            expenseSettings.setQuantityLimitEmergencyModeUsed(false);
        }

        expenseSettings.setNumberOfQuantityLimit(dto.numberOfQuantityLimit());
        expenseSettings.setPeriodType(dto.periodType());

    }

    @Transactional
    public void handleExpenseLimitExceeded(Long userId, CountQuantityLimitDto dto, PeriodType periodType, ConfirmPasswordDto confirmPasswordDto) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        if (!expenseSettings.isCountQuantityLimitEnabled()) return;

        long countedExpenses = countExpensesInPeriod(user, periodType);
        if (countedExpenses + 1 > dto.numberOfQuantityLimit()) {

            countQuantityLimitValidator.validateEmergencyMode(countedExpenses, confirmPasswordDto, expenseSettings);

            passwordValidator.validatePassword(userId, confirmPasswordDto);
            expenseSettings.setQuantityLimitEmergencyModeEnabled(false);
            expenseSettings.setQuantityLimitEmergencyModeUsed(true);
        }
    }

    @Transactional
    public CountQuantityLimitDto getCountQuantityLimit(Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        return new CountQuantityLimitDto(expenseSettings.isCountQuantityLimitEnabled(), expenseSettings.getPeriodType(), expenseSettings.getNumberOfQuantityLimit());
    }

    private long countExpensesInPeriod(User user, PeriodType periodType) {
        LocalDate today = LocalDate.now();
        LocalDate start = periodType.getStartDate(today);

        return expenseRepository.countExpensesByUserAssignedIdAndCreatedAtBetween(user.getId(), start, today);
    }

    private void createActivity(Long userId, boolean enabled) {
        kafkaTemplate.send("activity.settings", new SettingsActivityEvent(userId, SettingType.EXPENSE_COUNT_LIMIT, enabled ? SettingActivityStatus.ENABLED : SettingActivityStatus.DISABLED, LocalDateTime.now()));
    }

    private void handleDisable(ExpenseSettings settings) {
        settings.setQuantityLimitEmergencyModeUsed(false);
    }

}
