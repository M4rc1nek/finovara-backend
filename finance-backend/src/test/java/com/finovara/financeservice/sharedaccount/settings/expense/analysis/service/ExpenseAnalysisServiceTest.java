package com.finovara.financeservice.sharedaccount.settings.expense.analysis.service;

import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import com.finovara.financeservice.exception.conflict.ConfirmationRequiredException;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.sharedaccount.expense.model.SharedExpense;
import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettings;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettingsRepository;
import com.finovara.financeservice.sharedaccount.settings.expense.analysis.dto.ExpenseAnalysisDto;
import com.finovara.financeservice.sharedaccount.settings.expense.analysis.dto.ExpenseAnalysisMode;
import com.finovara.financeservice.util.settings.ExpenseAnomalyDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseAnalysisServiceTest {

    @Mock
    private SharedAccountSettingsRepository sharedAccountSettingsRepository;

    @Mock
    private SharedExpenseRepository sharedExpenseRepository;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private ExpenseAnomalyDetector expenseAnomalyDetector;

    @InjectMocks
    private ExpenseAnalysisService expenseAnalysisService;

    private SharedAccountSettings sharedAccountSettings;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        sharedAccountSettings = new SharedAccountSettings();
        when(sharedAccountSettingsRepository.findByUserId(USER_ID)).thenReturn(sharedAccountSettings);
    }

    @Nested
    class SaveExpenseAnalysis {

        @Test
        void shouldEnableExpenseAnalysis() {
            ExpenseAnalysisDto dto = new ExpenseAnalysisDto(true);

            expenseAnalysisService.saveExpenseAnalysis(USER_ID, dto);

            assertTrue(sharedAccountSettings.isExpenseAnalysisEnabled());
        }

        @Test
        void shouldDisableExpenseAnalysis() {
            ExpenseAnalysisDto dto = new ExpenseAnalysisDto(false);

            expenseAnalysisService.saveExpenseAnalysis(USER_ID, dto);

            assertFalse(sharedAccountSettings.isExpenseAnalysisEnabled());
        }
    }

    @Nested
    class GetExpenseAnalysis {

        @Test
        void shouldReturnEnabledTrue() {
            sharedAccountSettings.setExpenseAnalysisEnabled(true);

            ExpenseAnalysisDto result = expenseAnalysisService.getExpenseAnalysis(USER_ID);

            assertTrue(result.expenseAnalysisEnabled());
        }

        @Test
        void shouldReturnEnabledFalse() {
            sharedAccountSettings.setExpenseAnalysisEnabled(false);

            ExpenseAnalysisDto result = expenseAnalysisService.getExpenseAnalysis(USER_ID);

            assertFalse(result.expenseAnalysisEnabled());
        }
    }

    @Nested
    class HandleExpenseAnalysis {

        @Test
        void shouldDoNothingWhenExpenseAnalysisDisabled() {
            sharedAccountSettings.setExpenseAnalysisEnabled(false);

            expenseAnalysisService.handleExpenseAnalysis(USER_ID, null, BigDecimal.valueOf(100), ExpenseAnalysisMode.ADD);

            verifyNoInteractions(sharedExpenseRepository, expenseAnomalyDetector, authBackendClient);
        }

        @Test
        void shouldDoNothingWhenNotTenthExpenseInAddMode() {
            sharedAccountSettings.setExpenseAnalysisEnabled(true);
            when(sharedExpenseRepository.countExpensesByUserId(USER_ID)).thenReturn(3L);

            expenseAnalysisService.handleExpenseAnalysis(USER_ID, null, BigDecimal.valueOf(100), ExpenseAnalysisMode.ADD);

            verify(sharedExpenseRepository, never()).findTenLastByUserId(any(), any());
            verifyNoInteractions(expenseAnomalyDetector);
        }

        @Test
        void shouldDoNothingWhenNotTenthExpenseInEditMode() {
            sharedAccountSettings.setExpenseAnalysisEnabled(true);
            when(sharedExpenseRepository.countExpensesByUserId(USER_ID)).thenReturn(5L);

            expenseAnalysisService.handleExpenseAnalysis(USER_ID, null, BigDecimal.valueOf(100), ExpenseAnalysisMode.EDIT);

            verify(sharedExpenseRepository, never()).findTenLastByUserId(any(), any());
            verifyNoInteractions(expenseAnomalyDetector);
        }

        @Test
        void shouldFetchLastExpensesWhenAddModeReachesTenthExpense() {
            sharedAccountSettings.setExpenseAnalysisEnabled(true);
            when(sharedExpenseRepository.countExpensesByUserId(USER_ID)).thenReturn(9L);

            List<SharedExpense> lastExpenses = buildExpenses(BigDecimal.valueOf(100));
            when(sharedExpenseRepository.findTenLastByUserId(USER_ID, PageRequest.of(0, 11))).thenReturn(lastExpenses);
            when(expenseAnomalyDetector.calculateAnomalyThreshold(any(), any())).thenReturn(BigDecimal.valueOf(500));

            expenseAnalysisService.handleExpenseAnalysis(USER_ID, null, BigDecimal.valueOf(100), ExpenseAnalysisMode.ADD);

            verify(sharedExpenseRepository).findTenLastByUserId(USER_ID, PageRequest.of(0, 11));
        }

        @Test
        void shouldFetchLastExpensesWhenEditModeAtTenthExpense() {
            sharedAccountSettings.setExpenseAnalysisEnabled(true);
            when(sharedExpenseRepository.countExpensesByUserId(USER_ID)).thenReturn(10L);

            List<SharedExpense> lastExpenses = buildExpenses(BigDecimal.valueOf(100));
            when(sharedExpenseRepository.findTenLastByUserId(USER_ID, PageRequest.of(0, 11))).thenReturn(lastExpenses);
            when(expenseAnomalyDetector.calculateAnomalyThreshold(any(), any())).thenReturn(BigDecimal.valueOf(500));

            expenseAnalysisService.handleExpenseAnalysis(USER_ID, null, BigDecimal.valueOf(100), ExpenseAnalysisMode.EDIT);

            verify(sharedExpenseRepository).findTenLastByUserId(USER_ID, PageRequest.of(0, 11));
        }

        @Test
        void shouldNotRequirePasswordWhenExpenseWithinThreshold() {
            sharedAccountSettings.setExpenseAnalysisEnabled(true);
            when(sharedExpenseRepository.countExpensesByUserId(USER_ID)).thenReturn(9L);
            when(sharedExpenseRepository.findTenLastByUserId(USER_ID, PageRequest.of(0, 11)))
                    .thenReturn(buildExpenses(BigDecimal.valueOf(100)));
            when(expenseAnomalyDetector.calculateAnomalyThreshold(any(), any())).thenReturn(BigDecimal.valueOf(500));

            expenseAnalysisService.handleExpenseAnalysis(USER_ID, null, BigDecimal.valueOf(300), ExpenseAnalysisMode.ADD);

            verify(expenseAnomalyDetector, never()).requirePasswordConfirmation(any(), any(), any());
        }

        @Test
        void shouldThrowExceptionWhenUnusualExpenseWithoutPassword() {
            sharedAccountSettings.setExpenseAnalysisEnabled(true);
            when(sharedExpenseRepository.countExpensesByUserId(USER_ID)).thenReturn(9L);
            when(sharedExpenseRepository.findTenLastByUserId(USER_ID, PageRequest.of(0, 11)))
                    .thenReturn(buildExpenses(BigDecimal.valueOf(100)));
            when(expenseAnomalyDetector.calculateAnomalyThreshold(any(), any())).thenReturn(BigDecimal.valueOf(300));
            doThrow(new ConfirmationRequiredException("Password confirmation required"))
                    .when(expenseAnomalyDetector).requirePasswordConfirmation(USER_ID, null, authBackendClient);

            BigDecimal newExpense = BigDecimal.valueOf(400);

            assertThrows(ConfirmationRequiredException.class,
                    () -> expenseAnalysisService.handleExpenseAnalysis(USER_ID, null, newExpense, ExpenseAnalysisMode.ADD));
        }

        @Test
        void shouldConfirmPasswordWhenUnusualExpenseWithPassword() {
            sharedAccountSettings.setExpenseAnalysisEnabled(true);
            when(sharedExpenseRepository.countExpensesByUserId(USER_ID)).thenReturn(9L);
            when(sharedExpenseRepository.findTenLastByUserId(USER_ID, PageRequest.of(0, 11)))
                    .thenReturn(buildExpenses(BigDecimal.valueOf(100)));
            when(expenseAnomalyDetector.calculateAnomalyThreshold(any(), any())).thenReturn(BigDecimal.valueOf(300));

            BigDecimal newExpense = BigDecimal.valueOf(400);
            ConfirmPasswordDto passwordDto = new ConfirmPasswordDto("password");

            expenseAnalysisService.handleExpenseAnalysis(USER_ID, passwordDto, newExpense, ExpenseAnalysisMode.ADD);

            verify(expenseAnomalyDetector).requirePasswordConfirmation(USER_ID, passwordDto, authBackendClient);
        }

        private List<SharedExpense> buildExpenses(BigDecimal amount) {
            return IntStream.range(0, 4).mapToObj(i -> {
                SharedExpense expense = new SharedExpense();
                expense.setAmount(amount);
                return expense;
            }).toList();
        }
    }
}