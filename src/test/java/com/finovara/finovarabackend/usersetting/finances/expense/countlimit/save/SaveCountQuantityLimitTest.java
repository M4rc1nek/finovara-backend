package com.finovara.finovarabackend.usersetting.finances.expense.countlimit.save;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.exception.conflict.StateConflictException;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.model.CountQuantityLimitStrategy;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.service.CountQuantityLimitService;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.util.service.time.SpentInPeriodService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveCountQuantityLimitTest {

    @Mock
    private SpentInPeriodService spentInPeriodService;

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private SettingsActivityService settingsActivityService;

    @InjectMocks
    private CountQuantityLimitService countQuantityLimitService;

    private User user;
    private ExpenseSettings expenseSettings;

    private final String EMAIL = "test@test.com";

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);
        expenseSettings = new ExpenseSettings();
        user.setExpenseSettings(expenseSettings);
    }

    @Test
    void shouldEnableCountQuantityLimitWhenLimitIsSufficient() {
        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
        when(spentInPeriodService.today()).thenReturn(LocalDate.now());
        when(expenseRepository.countExpensesByUserAssignedIdAndCreatedAtBetween(anyLong(), any(), any()))
                .thenReturn(3L);

        CountQuantityLimitDto dto = new CountQuantityLimitDto(true, CountQuantityLimitStrategy.DAILY, 5);

        countQuantityLimitService.saveCountQuantityLimit(EMAIL, dto);

        verify(settingsActivityService).createSettingActivity(EMAIL, SettingActivityStatus.ENABLED, SettingType.EXPENSE_COUNT_LIMIT);
        assert expenseSettings.isExpenseCountQuantityLimitEnabled();
        assert expenseSettings.getNumberOfQuantityLimit() == 5;
    }

    @Test
    void shouldThrowWhenLimitLessThanAlreadyCountedExpenses() {
        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
        when(spentInPeriodService.today()).thenReturn(LocalDate.now());
        when(expenseRepository.countExpensesByUserAssignedIdAndCreatedAtBetween(anyLong(), any(), any()))
                .thenReturn(5L);

        CountQuantityLimitDto dto = new CountQuantityLimitDto(true, CountQuantityLimitStrategy.DAILY, 3);

        try {
            countQuantityLimitService.saveCountQuantityLimit(EMAIL, dto);
            assert false; // fail jeśli wyjątek nie został rzucony
        } catch (StateConflictException e) {
            // test przechodzi
        }
    }

    @Test
    void shouldDisableCountQuantityLimit() {
        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);

        CountQuantityLimitDto dto = new CountQuantityLimitDto(false, CountQuantityLimitStrategy.DAILY, 5);
        expenseSettings.setExpenseQuantityLimitEmergencyModeUsed(true);

        countQuantityLimitService.saveCountQuantityLimit(EMAIL, dto);

        verify(settingsActivityService).createSettingActivity(EMAIL, SettingActivityStatus.DISABLED, SettingType.EXPENSE_COUNT_LIMIT);
        assert !expenseSettings.isExpenseCountQuantityLimitEnabled();
        assert !expenseSettings.isExpenseQuantityLimitEmergencyModeUsed();
    }

    @Test
    void shouldResetEmergencyModeWhenStrategyChanges() {
        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
        when(spentInPeriodService.today()).thenReturn(LocalDate.now());
        when(expenseRepository.countExpensesByUserAssignedIdAndCreatedAtBetween(anyLong(), any(), any()))
                .thenReturn(2L);

        expenseSettings.setCountQuantityLimitStrategy(CountQuantityLimitStrategy.WEEKLY);
        expenseSettings.setExpenseQuantityLimitEmergencyModeUsed(true);

        CountQuantityLimitDto dto = new CountQuantityLimitDto(true, CountQuantityLimitStrategy.DAILY, 5);

        countQuantityLimitService.saveCountQuantityLimit(EMAIL, dto);

        assert !expenseSettings.isExpenseQuantityLimitEmergencyModeUsed();
        assert expenseSettings.getCountQuantityLimitStrategy() == CountQuantityLimitStrategy.DAILY;
        assert expenseSettings.getNumberOfQuantityLimit() == 5;
    }
}