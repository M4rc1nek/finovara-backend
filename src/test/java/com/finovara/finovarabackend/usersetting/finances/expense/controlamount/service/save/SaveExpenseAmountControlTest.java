package com.finovara.finovarabackend.usersetting.finances.expense.controlamount.service.save;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.controlamount.dto.ControlAmountDto;
import com.finovara.finovarabackend.usersetting.finances.expense.controlamount.service.ControlAmountService;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveExpenseAmountControlTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private SettingsActivityService settingsActivityService;

    @InjectMocks
    private ControlAmountService controlAmountService;

    private User user;
    private ExpenseSettings expenseSettings;

    private final String EMAIL = "test@test.com";

    @BeforeEach
    void setup() {
        user = new User();
        expenseSettings = new ExpenseSettings();
        user.setExpenseSettings(expenseSettings);
    }

    @Test
    void shouldEnableExpenseAmountControl() {
        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);

        ControlAmountDto dto = new ControlAmountDto(true, BigDecimal.valueOf(100));

        controlAmountService.saveExpenseAmountControl(EMAIL, dto);

        assertTrue(expenseSettings.isAmountThresholdEnabled());
        assertEquals(BigDecimal.valueOf(100), expenseSettings.getBlockedAmount());

        verify(settingsActivityService).createSettingActivity(EMAIL, SettingActivityStatus.ENABLED, SettingType.EXPENSE_CONTROL_AMOUNT);
    }

    @Test
    void shouldDisableExpenseAmountControl() {
        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);

        ControlAmountDto dto = new ControlAmountDto(false, BigDecimal.valueOf(50));

        controlAmountService.saveExpenseAmountControl(EMAIL, dto);

        assertFalse(expenseSettings.isAmountThresholdEnabled());
        assertEquals(BigDecimal.valueOf(50), expenseSettings.getBlockedAmount());

        verify(settingsActivityService).createSettingActivity(EMAIL, SettingActivityStatus.DISABLED, SettingType.EXPENSE_CONTROL_AMOUNT);
    }

    @Test
    void shouldSetBlockedAmountToZeroIfNull() {
        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);

        ControlAmountDto dto = new ControlAmountDto(true, null);

        controlAmountService.saveExpenseAmountControl(EMAIL, dto);

        assertEquals(BigDecimal.ZERO, expenseSettings.getBlockedAmount());
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenThrow(new InvalidInputException("User not found"));

        ControlAmountDto dto = new ControlAmountDto(true, BigDecimal.valueOf(10));

        assertThrows(InvalidInputException.class, () -> controlAmountService.saveExpenseAmountControl(EMAIL, dto));
    }
}