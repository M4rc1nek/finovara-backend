package com.finovara.finovarabackend.usersetting.finances.expense.countlimit.service;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.exception.conflict.StateConflictException;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.validator.CountQuantityLimitValidator;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordConfirmationService;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CountQuantityLimitService {

    private final UserManagerService userManagerService;
    private final ExpenseRepository expenseRepository;
    private final PasswordConfirmationService passwordConfirmationService;
    private final SettingsActivityService settingsActivityService;
    private final CountQuantityLimitValidator countQuantityLimitValidator;

    @Transactional
    public void saveCountQuantityLimit(String email, CountQuantityLimitDto dto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        expenseSettings.setCountQuantityLimitEnabled(dto.expenseCountLimitEnabled());

        createActivity(email, dto.expenseCountLimitEnabled());

        if (!dto.expenseCountLimitEnabled()) {
            handleDisable(expenseSettings);
            return;
        }
        long countedExpenses = countExpensesInPeriod(user, dto.periodType());
        if (dto.numberOfQuantityLimit() < countedExpenses) {
            throw new StateConflictException("You cannot add a limit " + dto.numberOfQuantityLimit() + ", because you have already "
                    + countedExpenses + " expenses in that period");
        }

        if (expenseSettings.getPeriodType() != dto.periodType()) {
            expenseSettings.setQuantityLimitEmergencyModeUsed(false);
        }

        expenseSettings.setNumberOfQuantityLimit(dto.numberOfQuantityLimit());
        expenseSettings.setPeriodType(dto.periodType());

    }

    @Transactional
    public void handleExpenseLimitExceeded(String email, CountQuantityLimitDto dto, PeriodType periodType, ConfirmPasswordDto confirmPasswordDto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        if (!expenseSettings.isCountQuantityLimitEnabled()) return;

        long countedExpenses = countExpensesInPeriod(user, periodType);
        if (countedExpenses + 1 > dto.numberOfQuantityLimit()) {

            countQuantityLimitValidator.validateEmergencyMode(countedExpenses, confirmPasswordDto,expenseSettings);

            passwordConfirmationService.confirmPassword(email, confirmPasswordDto);
            expenseSettings.setQuantityLimitEmergencyModeEnabled(false);
            expenseSettings.setQuantityLimitEmergencyModeUsed(true);
        }
    }

    @Transactional
    public CountQuantityLimitDto getCountQuantityLimit(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        return new CountQuantityLimitDto(expenseSettings.isCountQuantityLimitEnabled(),
                expenseSettings.getPeriodType(), expenseSettings.getNumberOfQuantityLimit());
    }

    private long countExpensesInPeriod(User user, PeriodType periodType) {
        LocalDate today = LocalDate.now();
        LocalDate start = periodType.getStartDate(today);

        return expenseRepository.countExpensesByUserAssignedIdAndCreatedAtBetween(user.getId(), start, today);
    }

    private void createActivity(String email, boolean enabled) {
        settingsActivityService.createSettingActivity(
                email,
                enabled ? SettingActivityStatus.ENABLED : SettingActivityStatus.DISABLED,
                SettingType.EXPENSE_COUNT_LIMIT
        );
    }

    private void handleDisable(ExpenseSettings settings) {
        settings.setQuantityLimitEmergencyModeUsed(false);
    }

}
