package com.finovara.finovarabackend.usersetting.finances.expense.countlimit.save;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.exception.conflict.StateConflictException;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.service.CountQuantityLimitService;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaveCountQuantityLimitTest {

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
        when(expenseRepository.countExpensesByUserAssignedIdAndCreatedAtBetween(anyLong(), any(), any()))
                .thenReturn(3L);

        CountQuantityLimitDto dto = new CountQuantityLimitDto(true, PeriodType.DAILY, 5);

        countQuantityLimitService.saveCountQuantityLimit(EMAIL, dto);

        verify(settingsActivityService).createSettingActivity(EMAIL, SettingActivityStatus.ENABLED, SettingType.EXPENSE_COUNT_LIMIT);
        assertTrue(expenseSettings.isCountQuantityLimitEnabled());
        assertEquals(5, expenseSettings.getNumberOfQuantityLimit());
    }

    @Test
    void shouldThrowWhenLimitLessThanAlreadyCountedExpenses() {
        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
        when(expenseRepository.countExpensesByUserAssignedIdAndCreatedAtBetween(anyLong(), any(), any()))
                .thenReturn(5L);

        CountQuantityLimitDto dto = new CountQuantityLimitDto(true, PeriodType.DAILY, 3);

        assertThrows(StateConflictException.class, () -> {
            countQuantityLimitService.saveCountQuantityLimit(EMAIL, dto);
        });
    }

    @Test
    void shouldDisableCountQuantityLimit() {
        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);

        CountQuantityLimitDto dto = new CountQuantityLimitDto(false, PeriodType.DAILY, 5);
        expenseSettings.setQuantityLimitEmergencyModeUsed(true);

        countQuantityLimitService.saveCountQuantityLimit(EMAIL, dto);

        verify(settingsActivityService).createSettingActivity(EMAIL, SettingActivityStatus.DISABLED, SettingType.EXPENSE_COUNT_LIMIT);
        assertFalse(expenseSettings.isCountQuantityLimitEnabled());
        assertFalse(expenseSettings.isQuantityLimitEmergencyModeUsed());
        verify(expenseRepository, never())
                .countExpensesByUserAssignedIdAndCreatedAtBetween(anyLong(), any(), any());
    }

    @Test
    void shouldResetEmergencyModeWhenStrategyChanges() {
        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
        when(expenseRepository.countExpensesByUserAssignedIdAndCreatedAtBetween(anyLong(), any(), any()))
                .thenReturn(2L);

        expenseSettings.setPeriodType(PeriodType.WEEKLY);
        expenseSettings.setQuantityLimitEmergencyModeUsed(true);

        CountQuantityLimitDto dto = new CountQuantityLimitDto(true, PeriodType.DAILY, 5);

        countQuantityLimitService.saveCountQuantityLimit(EMAIL, dto);

        assertFalse(expenseSettings.isQuantityLimitEmergencyModeUsed());
        assertEquals(PeriodType.DAILY, expenseSettings.getPeriodType());
        assertEquals(5, expenseSettings.getNumberOfQuantityLimit());
    }
}