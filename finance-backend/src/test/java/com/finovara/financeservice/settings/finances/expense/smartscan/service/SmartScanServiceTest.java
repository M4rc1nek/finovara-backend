package com.finovara.financeservice.settings.finances.expense.smartscan.service;

import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.event.activity.settings.SettingsActivityEvent;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.expense.repository.ExpenseSettingsRepository;
import com.finovara.financeservice.settings.finances.expense.smartscan.dto.SmartScanDto;
import com.finovara.financeservice.settings.finances.expense.smartscan.dto.SmartScanMode;
import com.finovara.financeservice.settings.finances.expense.smartscan.exception.conflict.SmartScanConfirmationRequiredException;
import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import org.springframework.kafka.core.KafkaTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartScanServiceTest {

    @Mock
    private ExpenseSettingsRepository expenseSettingsRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private PasswordValidator passwordValidator;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private SmartScanService smartScanService;

    private ExpenseSettings expenseSettings;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setup() {
        expenseSettings = new ExpenseSettings();
        when(expenseSettingsRepository.findByUserIdOrThrow(USER_ID)).thenReturn(expenseSettings);
    }

    @Nested
    class SaveSmartScan {
        @Test
        void shouldEnableSmartScan() {
            SmartScanDto dto = new SmartScanDto(true);

            smartScanService.saveSmartScan(USER_ID, dto);

            assertTrue(expenseSettings.isSmartScanEnabled());

            ArgumentCaptor<SettingsActivityEvent> eventCaptor = ArgumentCaptor.forClass(SettingsActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.settings"), eventCaptor.capture());
            assertEquals(SettingActivityStatus.ENABLED, eventCaptor.getValue().status());
        }

        @Test
        void shouldDisableSmartScan() {
            SmartScanDto dto = new SmartScanDto(false);

            smartScanService.saveSmartScan(USER_ID, dto);

            assertFalse(expenseSettings.isSmartScanEnabled());

            ArgumentCaptor<SettingsActivityEvent> eventCaptor = ArgumentCaptor.forClass(SettingsActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.settings"), eventCaptor.capture());
            assertEquals(SettingActivityStatus.DISABLED, eventCaptor.getValue().status());
        }
    }

    @Nested
    class GetSmartScan {
        @Test
        void shouldReturnEnabledTrue() {
            expenseSettings.setSmartScanEnabled(true);

            SmartScanDto result = smartScanService.getSmartScan(USER_ID);

            assertTrue(result.smartScanEnabled());
        }

        @Test
        void shouldReturnEnabledFalse() {
            expenseSettings.setSmartScanEnabled(false);

            SmartScanDto result = smartScanService.getSmartScan(USER_ID);

            assertFalse(result.smartScanEnabled());
        }
    }

    @Nested
    class HandleSmartScan {
        @Test
        void shouldDoNothingWhenSmartScanDisabled() {
            expenseSettings.setSmartScanEnabled(false);

            smartScanService.handleSmartScan(USER_ID, null, BigDecimal.valueOf(100), SmartScanMode.ADD);

            verifyNoInteractions(passwordValidator, expenseRepository);
        }

        @Test
        void shouldDoNothingWhenNotFifthExpense() {
            expenseSettings.setSmartScanEnabled(true);

            when(expenseRepository.countExpensesByUserId(USER_ID)).thenReturn(3L);

            smartScanService.handleSmartScan(USER_ID, null, BigDecimal.valueOf(100), SmartScanMode.ADD);

            verifyNoInteractions(passwordValidator);
        }

        @Test
        void shouldThrowExceptionWhenUnusualExpenseWithoutPassword() {
            expenseSettings.setSmartScanEnabled(true);

            when(expenseRepository.countExpensesByUserId(USER_ID)).thenReturn(4L);

            List<Expense> lastExpenses = IntStream.range(0, 4).mapToObj(i -> {
                Expense expense = new Expense();
                expense.setAmount(BigDecimal.valueOf(100));
                return expense;
            }).toList();

            when(expenseRepository.findFiveLastByUserId(USER_ID, PageRequest.of(0, 4))).thenReturn(lastExpenses);

            BigDecimal newExpense = BigDecimal.valueOf(400);

            assertThrows(SmartScanConfirmationRequiredException.class, () -> smartScanService.handleSmartScan(USER_ID, null, newExpense, SmartScanMode.ADD));

            verifyNoInteractions(passwordValidator);
        }

        @Test
        void shouldConfirmPasswordWhenUnusualExpenseWithPassword() {
            expenseSettings.setSmartScanEnabled(true);

            when(expenseRepository.countExpensesByUserId(USER_ID)).thenReturn(4L);

            List<Expense> lastExpenses = IntStream.range(0, 4).mapToObj(i -> {
                Expense expense = new Expense();
                expense.setAmount(BigDecimal.valueOf(100));
                return expense;
            }).toList();

            when(expenseRepository.findFiveLastByUserId(USER_ID, PageRequest.of(0, 4))).thenReturn(lastExpenses);

            BigDecimal newExpense = BigDecimal.valueOf(400);
            ConfirmPasswordDto passwordDto = new ConfirmPasswordDto("password");

            smartScanService.handleSmartScan(USER_ID, passwordDto, newExpense, SmartScanMode.ADD);

            verify(passwordValidator).validatePassword(USER_ID, passwordDto);
        }
    }
}