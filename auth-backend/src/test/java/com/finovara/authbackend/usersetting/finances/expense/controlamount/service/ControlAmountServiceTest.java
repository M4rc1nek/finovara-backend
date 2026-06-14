package com.finovara.authbackend.usersetting.finances.expense.controlamount.service;

import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.event.activity.settings.SettingsActivityEvent;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.authbackend.user.model.User;
import com.finovara.authbackend.usersetting.finances.expense.controlamount.dto.ControlAmountDto;
import com.finovara.authbackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.authbackend.util.user.service.UserManagerService;
import org.springframework.kafka.core.KafkaTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlAmountServiceTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ControlAmountService controlAmountService;

    private User user;
    private ExpenseSettings expenseSettings;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        user = new User();
        expenseSettings = new ExpenseSettings();
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

            ArgumentCaptor<SettingsActivityEvent> eventCaptor = ArgumentCaptor.forClass(SettingsActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.settings"), eventCaptor.capture());
            assertEquals(SettingActivityStatus.ENABLED, eventCaptor.getValue().status());
        }

        @Test
        void shouldDisableControl() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

            ControlAmountDto dto = new ControlAmountDto(false, BigDecimal.valueOf(50));

            controlAmountService.saveExpenseAmountControl(USER_ID, dto);

            assertFalse(expenseSettings.isAmountThresholdEnabled());
            assertEquals(BigDecimal.valueOf(50), expenseSettings.getBlockedAmount());

            ArgumentCaptor<SettingsActivityEvent> eventCaptor = ArgumentCaptor.forClass(SettingsActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.settings"), eventCaptor.capture());
            assertEquals(SettingActivityStatus.DISABLED, eventCaptor.getValue().status());
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