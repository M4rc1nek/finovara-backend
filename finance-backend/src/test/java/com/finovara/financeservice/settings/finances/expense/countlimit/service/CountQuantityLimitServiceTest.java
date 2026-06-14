package com.finovara.authbackend.settings.finances.expense.countlimit.service;

import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.event.activity.settings.SettingsActivityEvent;
import com.finovara.contracts.exception.conflict.StateConflictException;
import com.finovara.authbackend.expense.repository.ExpenseRepository;
import com.finovara.authbackend.settings.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.authbackend.settings.finances.expense.countlimit.validator.CountQuantityLimitValidator;
import com.finovara.authbackend.settings.finances.expense.model.ExpenseSettings;
import com.finovara.authbackend.settings.finances.expense.repository.ExpenseSettingsRepository;
import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import com.finovara.authbackend.util.confirmationpassword.service.PasswordValidator;
import com.finovara.contracts.model.PeriodType;
import org.springframework.kafka.core.KafkaTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CountQuantityLimitServiceTest {

    @Mock
    private ExpenseSettingsRepository expenseSettingsRepository;

    @Mock
    private CountQuantityLimitValidator countQuantityLimitValidator;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private PasswordValidator passwordValidator;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private CountQuantityLimitService countQuantityLimitService;

    private ExpenseSettings expenseSettings;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setup() {
        expenseSettings = new ExpenseSettings();
        when(expenseSettingsRepository.findByUserIdOrThrow(USER_ID)).thenReturn(expenseSettings);
    }

    @Nested
    class SaveCountQuantityLimit {

        @Test
        void shouldEnableLimitSuccessfully() {
            when(expenseRepository.countExpensesByUserIdAndCreatedAtBetween(anyLong(), any(), any())).thenReturn(2L);

            CountQuantityLimitDto dto = new CountQuantityLimitDto(true, PeriodType.DAILY, 5);

            countQuantityLimitService.saveCountQuantityLimit(USER_ID, dto);

            assertTrue(expenseSettings.isCountQuantityLimitEnabled());
            assertEquals(5, expenseSettings.getNumberOfQuantityLimit());

            ArgumentCaptor<SettingsActivityEvent> eventCaptor = ArgumentCaptor.forClass(SettingsActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.settings"), eventCaptor.capture());
            assertEquals(SettingActivityStatus.ENABLED, eventCaptor.getValue().status());
        }
        @Test
        void shouldThrowWhenLimitExceeded() {
            when(expenseRepository.countExpensesByUserIdAndCreatedAtBetween(anyLong(), any(), any())).thenReturn(5L);

            CountQuantityLimitDto dto = new CountQuantityLimitDto(true, PeriodType.DAILY, 3);

            assertThrows(StateConflictException.class, () -> countQuantityLimitService.saveCountQuantityLimit(USER_ID, dto));
        }

        @Test
        void shouldDisableLimit() {
            expenseSettings.setQuantityLimitEmergencyModeUsed(true);

            CountQuantityLimitDto dto = new CountQuantityLimitDto(false, PeriodType.DAILY, 5);

            countQuantityLimitService.saveCountQuantityLimit(USER_ID, dto);

            assertFalse(expenseSettings.isCountQuantityLimitEnabled());
            assertFalse(expenseSettings.isQuantityLimitEmergencyModeUsed());

            ArgumentCaptor<SettingsActivityEvent> eventCaptor = ArgumentCaptor.forClass(SettingsActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.settings"), eventCaptor.capture());
            assertEquals(SettingActivityStatus.DISABLED, eventCaptor.getValue().status());
        }

    }
    @Nested
    class HandleExpenseLimit {
        @Test
        void shouldDoNothingWhenLimitDisabled() {
            expenseSettings.setCountQuantityLimitEnabled(false);

            countQuantityLimitService.handleExpenseLimitExceeded(USER_ID, new CountQuantityLimitDto(true,
                    PeriodType.DAILY, 5), PeriodType.DAILY, null);

            verifyNoInteractions(passwordValidator);
        }

        @Test
        void shouldUseEmergencyModeWhenPasswordProvided() {
            expenseSettings.setCountQuantityLimitEnabled(true);
            expenseSettings.setQuantityLimitEmergencyModeEnabled(true);

            when(expenseRepository.countExpensesByUserIdAndCreatedAtBetween(eq(USER_ID), any(), any())).thenReturn(5L);

            CountQuantityLimitDto dto = new CountQuantityLimitDto(true, PeriodType.DAILY, 5);
            ConfirmPasswordDto confirmPasswordDto = new ConfirmPasswordDto("password");

            countQuantityLimitService.handleExpenseLimitExceeded(USER_ID, dto, PeriodType.DAILY, confirmPasswordDto);

            verify(passwordValidator).validatePassword(USER_ID, confirmPasswordDto);

            assertFalse(expenseSettings.isQuantityLimitEmergencyModeEnabled());
            assertTrue(expenseSettings.isQuantityLimitEmergencyModeUsed());
        }

    }
    @Nested
    class GetCountQuantityLimit {
        @Test
        void shouldReturnEnabledLimit() {
            expenseSettings.setCountQuantityLimitEnabled(true);
            expenseSettings.setPeriodType(PeriodType.DAILY);
            expenseSettings.setNumberOfQuantityLimit(5);

            CountQuantityLimitDto dto = countQuantityLimitService.getCountQuantityLimit(USER_ID);

            assertTrue(dto.expenseCountLimitEnabled());
            assertEquals(PeriodType.DAILY, dto.periodType());
            assertEquals(5, dto.numberOfQuantityLimit());
        }

        @Test
        void shouldReturnDisabledLimit() {
            expenseSettings.setCountQuantityLimitEnabled(false);
            expenseSettings.setPeriodType(PeriodType.WEEKLY);
            expenseSettings.setNumberOfQuantityLimit(10);

            CountQuantityLimitDto dto = countQuantityLimitService.getCountQuantityLimit(USER_ID);

            assertFalse(dto.expenseCountLimitEnabled());
            assertEquals(PeriodType.WEEKLY, dto.periodType());
            assertEquals(10, dto.numberOfQuantityLimit());
        }
    }
}
