package com.finovara.finovarabackend.usersetting.finances.expense.controlamount.service;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.controlamount.dto.ControlAmountDto;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
class ControlAmountServiceTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private SettingsActivityService settingsActivityService;

    @InjectMocks
    private ControlAmountService controlAmountService;

    private User user;
    private ExpenseSettings expenseSettings;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        user = new User();
        expenseSettings = new ExpenseSettings();
        user.setExpenseSettings(expenseSettings);
    }

    @Nested
    class GetExpenseControlAmount {

        @Test
        void shouldReturnCorrectDtoWhenEnabled() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

            expenseSettings.setAmountThresholdEnabled(true);
            expenseSettings.setBlockedAmount(BigDecimal.valueOf(200));

            ControlAmountDto result = controlAmountService.getExpenseAmountControl(USER_ID);

            assertTrue(result.expenseAmountThresholdEnabled());
            assertEquals(BigDecimal.valueOf(200), result.blockedAmount());
        }

        @Test
        void shouldReturnCorrectDtoWhenDisabled() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

            expenseSettings.setAmountThresholdEnabled(false);
            expenseSettings.setBlockedAmount(BigDecimal.valueOf(50));

            ControlAmountDto result = controlAmountService.getExpenseAmountControl(USER_ID);

            assertFalse(result.expenseAmountThresholdEnabled());
            assertEquals(BigDecimal.valueOf(50), result.blockedAmount());
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenThrow(new InvalidInputException("User not found"));

            assertThrows(InvalidInputException.class, () -> controlAmountService.getExpenseAmountControl(USER_ID));
        }
    }

    @Nested
    class HandleExpenseControlAmount {

        @Test
        void shouldThrowWhenAmountExceedsLimit() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

            expenseSettings.setAmountThresholdEnabled(true);
            expenseSettings.setBlockedAmount(BigDecimal.valueOf(100));

            assertThrows(InvalidInputException.class, () -> controlAmountService.handleExpenseAmountControl(USER_ID, BigDecimal.valueOf(150)));
        }

        @Test
        void shouldAllowEqualAmount() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

            expenseSettings.setAmountThresholdEnabled(true);
            expenseSettings.setBlockedAmount(BigDecimal.valueOf(100));

            assertDoesNotThrow(() -> controlAmountService.handleExpenseAmountControl(USER_ID, BigDecimal.valueOf(100)));
        }

        @Test
        void shouldAllowWhenDisabled() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

            expenseSettings.setAmountThresholdEnabled(false);
            expenseSettings.setBlockedAmount(BigDecimal.valueOf(50));

            assertDoesNotThrow(() -> controlAmountService.handleExpenseAmountControl(USER_ID, BigDecimal.valueOf(1000)));
        }

        @Test
        void shouldTreatNullBlockedAmountAsZero() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

            expenseSettings.setAmountThresholdEnabled(true);
            expenseSettings.setBlockedAmount(null);

            assertThrows(InvalidInputException.class, () -> controlAmountService.handleExpenseAmountControl(USER_ID, BigDecimal.valueOf(10)));
        }
    }

    @Nested
    class SaveExpenseControlAmount {
        @Test
        void shouldEnableControl() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

            ControlAmountDto dto = new ControlAmountDto(true, BigDecimal.valueOf(100));

            controlAmountService.saveExpenseAmountControl(USER_ID, dto);

            assertTrue(expenseSettings.isAmountThresholdEnabled());
            assertEquals(BigDecimal.valueOf(100), expenseSettings.getBlockedAmount());

            verify(settingsActivityService).createSettingActivity(USER_ID, SettingActivityStatus.ENABLED, SettingType.EXPENSE_CONTROL_AMOUNT);
        }

        @Test
        void shouldDisableControl() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

            ControlAmountDto dto = new ControlAmountDto(false, BigDecimal.valueOf(50));

            controlAmountService.saveExpenseAmountControl(USER_ID, dto);

            assertFalse(expenseSettings.isAmountThresholdEnabled());
            assertEquals(BigDecimal.valueOf(50), expenseSettings.getBlockedAmount());

            verify(settingsActivityService).createSettingActivity(USER_ID, SettingActivityStatus.DISABLED, SettingType.EXPENSE_CONTROL_AMOUNT);
        }

        @Test
        void shouldSetZeroWhenNullAmount() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

            ControlAmountDto dto = new ControlAmountDto(true, null);

            controlAmountService.saveExpenseAmountControl(USER_ID, dto);

            assertEquals(BigDecimal.ZERO, expenseSettings.getBlockedAmount());
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenThrow(new InvalidInputException("User not found"));

            ControlAmountDto dto = new ControlAmountDto(true, BigDecimal.valueOf(10));

            assertThrows(InvalidInputException.class, () -> controlAmountService.saveExpenseAmountControl(USER_ID, dto));
        }
    }
}