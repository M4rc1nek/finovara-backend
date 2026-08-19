package com.finovara.financeservice.settings.finances.expense.smartscan.service;

import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.activity.event.settings.SettingsActivityEvent;
import com.finovara.financeservice.exception.conflict.ConfirmationRequiredException;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.expense.repository.ExpenseSettingsRepository;
import com.finovara.financeservice.settings.finances.expense.smartscan.dto.SmartScanDto;
import com.finovara.financeservice.settings.finances.expense.smartscan.dto.SmartScanMode;
import com.finovara.financeservice.util.settings.ExpenseAnomalyDetector;
import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartScanServiceTest {

    @Mock
    private ExpenseSettingsRepository expenseSettingsRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ExpenseAnomalyDetector expenseAnomalyDetector;

    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @InjectMocks
    private SmartScanService smartScanService;

    private ExpenseSettings expenseSettings;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setup() {
        expenseSettings = new ExpenseSettings();
        when(expenseSettingsRepository.findByUserId(USER_ID)).thenReturn(expenseSettings);
    }

    @Nested
    class SaveSmartScan {

        @Test
        void shouldEnableSmartScan() {
            SmartScanDto dto = new SmartScanDto(true, null);

            smartScanService.saveSmartScan(USER_ID, dto);

            assertTrue(expenseSettings.isSmartScanEnabled());

            ArgumentCaptor<SettingsActivityEvent> eventCaptor = ArgumentCaptor.forClass(SettingsActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.settings"), eventCaptor.capture());
            assertEquals(SettingActivityStatus.ENABLED, eventCaptor.getValue().status());
        }

        @Test
        void shouldDisableSmartScan() {
            SmartScanDto dto = new SmartScanDto(false, null);

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

            verifyNoInteractions(authBackendClient, expenseRepository, expenseAnomalyDetector);
        }

        @Test
        void shouldDoNothingWhenNotFifthExpense() {
            expenseSettings.setSmartScanEnabled(true);

            when(expenseRepository.countExpensesByUserId(USER_ID)).thenReturn(3L);

            smartScanService.handleSmartScan(USER_ID, null, BigDecimal.valueOf(100), SmartScanMode.ADD);

            verify(expenseRepository, never()).findFiveLastByUserId(any(), any());
            verifyNoInteractions(authBackendClient, expenseAnomalyDetector);
        }

        @Test
        void shouldNotRequirePasswordWhenExpenseWithinThreshold() {
            expenseSettings.setSmartScanEnabled(true);

            when(expenseRepository.countExpensesByUserId(USER_ID)).thenReturn(4L);

            List<Expense> lastExpenses = buildExpenses(BigDecimal.valueOf(100));
            when(expenseRepository.findFiveLastByUserId(USER_ID, PageRequest.of(0, 4))).thenReturn(lastExpenses);
            when(expenseAnomalyDetector.calculateAnomalyThreshold(any(), any())).thenReturn(BigDecimal.valueOf(500));

            smartScanService.handleSmartScan(USER_ID, null, BigDecimal.valueOf(300), SmartScanMode.ADD);

            verify(expenseAnomalyDetector, never()).requirePasswordConfirmation(any(), any(), any());
        }

        @Test
        void shouldThrowExceptionWhenUnusualExpenseWithoutPassword() {
            expenseSettings.setSmartScanEnabled(true);

            when(expenseRepository.countExpensesByUserId(USER_ID)).thenReturn(4L);

            List<Expense> lastExpenses = buildExpenses(BigDecimal.valueOf(100));
            when(expenseRepository.findFiveLastByUserId(USER_ID, PageRequest.of(0, 4))).thenReturn(lastExpenses);
            when(expenseAnomalyDetector.calculateAnomalyThreshold(any(), any())).thenReturn(BigDecimal.valueOf(300));
            doThrow(new ConfirmationRequiredException("Password confirmation required"))
                    .when(expenseAnomalyDetector).requirePasswordConfirmation(USER_ID, null, authBackendClient);

            BigDecimal newExpense = BigDecimal.valueOf(400);

            assertThrows(ConfirmationRequiredException.class,
                    () -> smartScanService.handleSmartScan(USER_ID, null, newExpense, SmartScanMode.ADD));
        }

        @Test
        void shouldConfirmPasswordWhenUnusualExpenseWithPassword() {
            expenseSettings.setSmartScanEnabled(true);

            when(expenseRepository.countExpensesByUserId(USER_ID)).thenReturn(4L);

            List<Expense> lastExpenses = buildExpenses(BigDecimal.valueOf(100));
            when(expenseRepository.findFiveLastByUserId(USER_ID, PageRequest.of(0, 4))).thenReturn(lastExpenses);
            when(expenseAnomalyDetector.calculateAnomalyThreshold(any(), any())).thenReturn(BigDecimal.valueOf(300));

            BigDecimal newExpense = BigDecimal.valueOf(400);
            ConfirmPasswordDto passwordDto = new ConfirmPasswordDto("password");

            smartScanService.handleSmartScan(USER_ID, passwordDto, newExpense, SmartScanMode.ADD);

            verify(expenseAnomalyDetector).requirePasswordConfirmation(USER_ID, passwordDto, authBackendClient);
        }

        @Test
        void shouldDoNothingWhenEditModeNotAtScanInterval() {
            expenseSettings.setSmartScanEnabled(true);

            when(expenseRepository.countExpensesByUserId(USER_ID)).thenReturn(6L);

            smartScanService.handleSmartScan(USER_ID, null, BigDecimal.valueOf(100), SmartScanMode.EDIT);

            verify(expenseRepository, never()).findFiveLastByUserId(any(), any());
            verifyNoInteractions(authBackendClient, expenseAnomalyDetector);
        }

        private List<Expense> buildExpenses(BigDecimal amount) {
            return IntStream.range(0, 4).mapToObj(i -> {
                Expense expense = new Expense();
                expense.setAmount(amount);
                return expense;
            }).toList();
        }
    }
}