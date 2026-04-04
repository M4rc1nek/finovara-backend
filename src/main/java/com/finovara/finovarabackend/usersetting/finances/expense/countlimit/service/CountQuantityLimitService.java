package com.finovara.finovarabackend.usersetting.finances.expense.countlimit.service;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.finovarabackend.exception.conflict.StateConflictException;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordConfirmationService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CountQuantityLimitService {

    private final UserManagerService userManagerService;
    private final ExpenseRepository expenseRepository;
    private final PasswordConfirmationService passwordConfirmationService;
    private final SettingsActivityService settingsActivityService;

    @Transactional
    public void saveCountQuantityLimit(String email, CountQuantityLimitDto dto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        expenseSettings.setCountQuantityLimitEnabled(dto.expenseCountLimitEnabled());
        if (!dto.expenseCountLimitEnabled()) {
            settingsActivityService.createSettingActivity(email, SettingActivityStatus.DISABLED, SettingType.EXPENSE_COUNT_LIMIT);
            expenseSettings.setQuantityLimitEmergencyModeUsed(false);
            return;
        }else{
            settingsActivityService.createSettingActivity(email, SettingActivityStatus.ENABLED, SettingType.EXPENSE_COUNT_LIMIT);
        }

        long countedExpenses = countExpensesInPeriod(user, dto.periodType());
        if (dto.numberOfQuantityLimit() < countedExpenses) {
            throw new StateConflictException("You cannot add a limit " + dto.numberOfQuantityLimit() + ", because you have already "
                    + countedExpenses + " expenses in that period");
        }

        if(expenseSettings.getPeriodType() != dto.periodType()){
            expenseSettings.setQuantityLimitEmergencyModeUsed(false);
        }

        expenseSettings.setNumberOfQuantityLimit(dto.numberOfQuantityLimit());
        expenseSettings.setPeriodType(dto.periodType());

    }

    @Transactional
    public void calculateCountQuantityLimit(String email, CountQuantityLimitDto dto, PeriodType periodType, ConfirmPasswordDto confirmPasswordDto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        if (!expenseSettings.isCountQuantityLimitEnabled()) return;

        long countedExpenses = countExpensesInPeriod(user, periodType);
        if (countedExpenses + 1 > dto.numberOfQuantityLimit()) {

            if (expenseSettings.isQuantityLimitEmergencyModeUsed()) {
                throw new StateConflictException("Emergency mode already used. You have already added an expense using emergency mode in this period. You cannot add more expenses until the limit is increased or the period resets.");
            }

            if (!expenseSettings.isQuantityLimitEmergencyModeEnabled()) {
                log.info("Quantity limit exceeded. Current count: {}, Limit: {}", countedExpenses, dto.numberOfQuantityLimit());
                throw new StateConflictException("Quantity Limit Exceeded, you have already added " + countedExpenses + " expenses");
            }

            log.info("The user's expense exceeds the limit, but emergency mode is enabled");
            if (confirmPasswordDto == null) {
                throw new MissingRequirementException("Emergency mode password confirmation required to continue");
            }

            passwordConfirmationService.confirmPassword(email, confirmPasswordDto);
            expenseSettings.setQuantityLimitEmergencyModeEnabled(false);
            expenseSettings.setQuantityLimitEmergencyModeUsed(true);
            log.info("User confirmed password. Expense added");
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

}
