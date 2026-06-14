package com.finovara.authbackend.settings.finances.expense.controlamount.service;

import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.event.activity.settings.SettingsActivityEvent;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.authbackend.settings.finances.expense.controlamount.dto.ControlAmountDto;
import com.finovara.authbackend.settings.finances.expense.model.ExpenseSettings;
import com.finovara.authbackend.settings.finances.expense.repository.ExpenseSettingsRepository;
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
    private ExpenseSettingsRepository expenseSettingsRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ControlAmountService controlAmountService;

    private ExpenseSettings expenseSettings;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        expenseSettings = new ExpenseSettings();
        when(expenseSettingsRepository.findByUserIdOrThrow(USER_ID)).thenReturn(expenseSettings);
    }

    @Nested
    class GetExpenseControlAmount {

        @Test
        void shouldReturnCorrectDtoWhenEnabled() {
            expenseSettings.setAmountThresholdEnabled(true);
            expenseSettings.setBlockedAmount(BigDecimal.valueOf(200));

            ControlAmountDto result = controlAmountService.getExpenseAmountControl(USER_ID);

            assertTrue(result.expenseAmountThresholdEnabled());
            assertEquals(BigDecimal.valueOf(200), result.blockedAmount());
        }

        @Test
        void shouldReturnCorrectDtoWhenDisabled() {
            expenseSettings.setAmountThresholdEnabled(false);
            expenseSettings.setBlockedAmount(BigDecimal.valueOf(50));

            ControlAmountDto result = controlAmountService.getExpenseAmountControl(USER_ID);

            assertFalse(result.expenseAmountThresholdEnabled());
            assertEquals(BigDecimal.valueOf(50), result.blockedAmount());
        }

        @Test
        void shouldThrowWhenSettingsNotFound() {
            when(expenseSettingsRepository.findByUserIdOrThrow(USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("Expense settings not found"));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> controlAmountService.getExpenseAmountControl(USER_ID));
        }
    }

    @Nested
    class HandleExpenseControlAmount {

        @Test
        void shouldThrowWhenAmountExceedsLimit() {
            expenseSettings.setAmountThresholdEnabled(true);
            expenseSettings.setBlockedAmount(BigDecimal.valueOf(100));

            assertThrows(InvalidInputException.class,
                    () -> controlAmountService.handleExpenseAmountControl(USER_ID, BigDecimal.valueOf(150)));
        }

        @Test
        void shouldAllowEqualAmount() {
            expenseSettings.setAmountThresholdEnabled(true);
            expenseSettings.setBlockedAmount(BigDecimal.valueOf(100));

            assertDoesNotThrow(() -> controlAmountService.handleExpenseAmountControl(USER_ID, BigDecimal.valueOf(100)));
        }

        @Test
        void shouldAllowWhenDisabled() {
            expenseSettings.setAmountThresholdEnabled(false);
            expenseSettings.setBlockedAmount(BigDecimal.valueOf(50));

            assertDoesNotThrow(() -> controlAmountService.handleExpenseAmountControl(USER_ID, BigDecimal.valueOf(1000)));
        }

        @Test
        void shouldTreatNullBlockedAmountAsZero() {
            expenseSettings.setAmountThresholdEnabled(true);
            expenseSettings.setBlockedAmount(null);

            assertThrows(InvalidInputException.class,
                    () -> controlAmountService.handleExpenseAmountControl(USER_ID, BigDecimal.valueOf(10)));
        }
    }

    @Nested
    class SaveExpenseControlAmount {
        @Test
        void shouldEnableControl() {
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
            ControlAmountDto dto = new ControlAmountDto(true, null);

            controlAmountService.saveExpenseAmountControl(USER_ID, dto);

            assertEquals(BigDecimal.ZERO, expenseSettings.getBlockedAmount());
        }

        @Test
        void shouldThrowWhenSettingsNotFound() {
            when(expenseSettingsRepository.findByUserIdOrThrow(USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("Expense settings not found"));

            ControlAmountDto dto = new ControlAmountDto(true, BigDecimal.valueOf(10));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> controlAmountService.saveExpenseAmountControl(USER_ID, dto));
        }
    }
}
