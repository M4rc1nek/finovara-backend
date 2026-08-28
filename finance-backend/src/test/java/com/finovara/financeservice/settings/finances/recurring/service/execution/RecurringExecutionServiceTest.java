package com.finovara.financeservice.settings.finances.recurring.service.execution;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.RecurringType;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.financeservice.expense.dto.ExpenseDto;
import com.finovara.financeservice.expense.dto.ExpenseRequestDto;
import com.finovara.financeservice.expense.service.ExpenseService;
import com.finovara.financeservice.piggybank.service.PiggyBankTransactionService;
import com.finovara.financeservice.revenue.dto.RevenueDto;
import com.finovara.financeservice.revenue.service.RevenueService;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.expense.repository.ExpenseSettingsRepository;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.service.validator.ExpenseSettingsValidator;
import com.finovara.financeservice.settings.finances.recurring.service.validator.RecurringRevenueValidator;
import com.finovara.financeservice.settings.finances.recurring.service.validator.RecurringSavingsValidator;
import com.finovara.financeservice.util.limit.manager.LimitManagerService;
import com.finovara.financeservice.util.transaction.TransactionOrigin;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringExecutionServiceTest {

    @Mock
    private RevenueService revenueService;

    @Mock
    private ExpenseService expenseService;

    @Mock
    private PiggyBankTransactionService piggyBankTransactionService;

    @Mock
    private ExpenseSettingsValidator expenseSettingsValidator;

    @Mock
    private RecurringRevenueValidator recurringRevenueValidator;

    @Mock
    private RecurringSavingsValidator recurringSavingsValidator;

    @Mock
    private ExpenseSettingsRepository expenseSettingsRepository;

    @Mock
    private WalletManagerService walletManagerService;

    @Mock
    private LimitManagerService limitManagerService;

    private RecurringExecutionService recurringExecutionService;

    private RecurringSettings recurringSettings;

    private LocalDate executionDate;

    @BeforeEach
    void setUp() {
        recurringExecutionService = new RecurringExecutionService(
                revenueService,
                expenseService,
                piggyBankTransactionService,
                expenseSettingsValidator,
                recurringRevenueValidator,
                recurringSavingsValidator,
                expenseSettingsRepository,
                walletManagerService,
                limitManagerService
        );

        recurringSettings = mock(RecurringSettings.class);
        executionDate = LocalDate.of(2026, 1, 1);
    }

    @Nested
    class ExecuteTests {

        @Test
        void shouldReturnExecutedAndDoNothingWhenTypeIsNull() {
            when(recurringSettings.getType()).thenReturn(null);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertThat(result).isEqualTo(RecurringExecutionResult.EXECUTED);
            verifyNoInteractions(revenueService, expenseService, piggyBankTransactionService,
                    expenseSettingsValidator, recurringRevenueValidator, recurringSavingsValidator);
        }

        @Test
        void shouldDelegateToCreateRevenueWhenTypeIsRevenue() {
            when(recurringSettings.getType()).thenReturn(RecurringType.REVENUE);
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getRevenueCategory()).thenReturn(RevenueCategory.SALARY);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(100));

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertThat(result).isEqualTo(RecurringExecutionResult.EXECUTED);
            verify(recurringRevenueValidator).validate(recurringSettings);
            verify(revenueService).addRevenue(any(RevenueDto.class), eq(1L), eq(TransactionOrigin.RECURRING_SYSTEM));
        }

        @Test
        void shouldDelegateToCreateExpenseWhenTypeIsExpense() {
            when(recurringSettings.getType()).thenReturn(RecurringType.EXPENSE);
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(null);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertThat(result).isEqualTo(RecurringExecutionResult.EXECUTED);
            verify(expenseSettingsRepository).findByUserId(1L);
            verifyNoInteractions(walletManagerService, limitManagerService, expenseService);
        }

        @Test
        void shouldDelegateToCreateSavingsWhenTypeIsSavings() {
            when(recurringSettings.getType()).thenReturn(RecurringType.SAVINGS);
            when(recurringSettings.getUserId()).thenReturn(null);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertThat(result).isEqualTo(RecurringExecutionResult.EXECUTED);
            verifyNoInteractions(walletManagerService, recurringSavingsValidator, piggyBankTransactionService);
        }
    }

    @Nested
    class CreateRevenueTests {

        @BeforeEach
        void setUp() {
            when(recurringSettings.getType()).thenReturn(RecurringType.REVENUE);
        }

        @Test
        void shouldReturnExecutedWhenUserIdIsNull() {
            when(recurringSettings.getUserId()).thenReturn(null);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertThat(result).isEqualTo(RecurringExecutionResult.EXECUTED);
            verifyNoInteractions(recurringRevenueValidator, revenueService);
        }

        @Test
        void shouldReturnExecutedWhenRevenueCategoryIsNull() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getRevenueCategory()).thenReturn(null);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertThat(result).isEqualTo(RecurringExecutionResult.EXECUTED);
            verifyNoInteractions(recurringRevenueValidator, revenueService);
        }

        @Test
        void shouldValidateAndAddRevenueWhenSettingsAreValid() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getRevenueCategory()).thenReturn(RevenueCategory.SALARY);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(500));

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertThat(result).isEqualTo(RecurringExecutionResult.EXECUTED);
            verify(recurringRevenueValidator).validate(recurringSettings);
            verify(revenueService).addRevenue(any(RevenueDto.class), eq(1L), eq(TransactionOrigin.RECURRING_SYSTEM));
        }

        @Test
        void shouldBuildRevenueDtoWithCorrectFieldsWhenSettingsAreValid() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getRevenueCategory()).thenReturn(RevenueCategory.SALARY);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(500));

            recurringExecutionService.execute(recurringSettings, executionDate);

            ArgumentCaptor<RevenueDto> captor = ArgumentCaptor.forClass(RevenueDto.class);
            verify(revenueService).addRevenue(captor.capture(), eq(1L), eq(TransactionOrigin.RECURRING_SYSTEM));

            RevenueDto capturedDto = captor.getValue();
            assertThat(capturedDto.userId()).isEqualTo(1L);
            assertThat(capturedDto.amount()).isEqualByComparingTo(BigDecimal.valueOf(500));
            assertThat(capturedDto.category()).isEqualTo(RevenueCategory.SALARY);
            assertThat(capturedDto.createdAt()).isEqualTo(executionDate);
        }

        @Test
        void shouldReturnSkippedWhenValidatorRejectsSettingsWithInvalidInput() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getRevenueCategory()).thenReturn(RevenueCategory.SALARY);

            doThrow(new InvalidInputException("invalid settings")).when(recurringRevenueValidator).validate(recurringSettings);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertThat(result).isEqualTo(RecurringExecutionResult.SKIPPED);
            verifyNoInteractions(revenueService);
        }

        @Test
        void shouldPropagateExceptionWhenValidatorThrowsUnhandledException() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getRevenueCategory()).thenReturn(RevenueCategory.SALARY);

            doThrow(new IllegalStateException("invalid settings")).when(recurringRevenueValidator).validate(recurringSettings);

            assertThrows(IllegalStateException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));

            verifyNoInteractions(revenueService);
        }
    }

    @Nested
    class CreateExpenseTests {

        private ExpenseSettings expenseSettings;

        private Wallet wallet;

        @BeforeEach
        void setUp() {
            when(recurringSettings.getType()).thenReturn(RecurringType.EXPENSE);

            expenseSettings = mock(ExpenseSettings.class);
            wallet = mock(Wallet.class);
        }

        @Test
        void shouldReturnExecutedWhenUserIdIsNull() {
            when(recurringSettings.getUserId()).thenReturn(null);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertThat(result).isEqualTo(RecurringExecutionResult.EXECUTED);
            verifyNoInteractions(expenseSettingsRepository, walletManagerService, limitManagerService, expenseService);
        }

        @Test
        void shouldReturnExecutedWhenExpenseCategoryIsNull() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(null);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertThat(result).isEqualTo(RecurringExecutionResult.EXECUTED);
            verifyNoInteractions(expenseSettingsRepository, walletManagerService, limitManagerService, expenseService);
        }

        @Test
        void shouldReturnExecutedWhenExpenseSettingsNotFound() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(null);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertThat(result).isEqualTo(RecurringExecutionResult.EXECUTED);
            verifyNoInteractions(walletManagerService, limitManagerService, expenseService);
        }

        @Test
        void shouldValidateAndAddExpenseWhenSettingsAreValid() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(200));
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(expenseSettings);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);
            when(limitManagerService.getLimitsByUserId(1L)).thenReturn(List.of());
            when(expenseSettings.getPeriodType()).thenReturn(PeriodType.MONTHLY);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertThat(result).isEqualTo(RecurringExecutionResult.EXECUTED);
            verify(expenseSettingsValidator).validate(recurringSettings, expenseSettings, wallet, List.of());
            verify(expenseService).addExpense(any(ExpenseRequestDto.class), eq(1L), eq(TransactionOrigin.RECURRING_SYSTEM));
            verify(recurringSettings).setSkippedNotificationSent(false);
        }

        @Test
        void shouldUseExpenseSettingsPeriodTypeWhenPresent() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(200));
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(expenseSettings);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);
            when(limitManagerService.getLimitsByUserId(1L)).thenReturn(List.of());
            when(expenseSettings.getPeriodType()).thenReturn(PeriodType.WEEKLY);
            when(expenseSettings.isCountQuantityLimitEnabled()).thenReturn(true);
            when(expenseSettings.getNumberOfQuantityLimit()).thenReturn(5);

            recurringExecutionService.execute(recurringSettings, executionDate);

            ArgumentCaptor<ExpenseRequestDto> captor = ArgumentCaptor.forClass(ExpenseRequestDto.class);
            verify(expenseService).addExpense(captor.capture(), eq(1L), eq(TransactionOrigin.RECURRING_SYSTEM));
            assertThat(captor.getValue().countQuantityLimitDto().periodType()).isEqualTo(PeriodType.WEEKLY);
        }

        @Test
        void shouldUseSettingsPeriodTypeWhenExpenseSettingsPeriodTypeIsNull() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(200));
            when(recurringSettings.getPeriodType()).thenReturn(PeriodType.MONTHLY);
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(expenseSettings);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);
            when(limitManagerService.getLimitsByUserId(1L)).thenReturn(List.of());
            when(expenseSettings.getPeriodType()).thenReturn(null);
            when(expenseSettings.isCountQuantityLimitEnabled()).thenReturn(false);
            when(expenseSettings.getNumberOfQuantityLimit()).thenReturn(0);

            recurringExecutionService.execute(recurringSettings, executionDate);

            ArgumentCaptor<ExpenseRequestDto> captor = ArgumentCaptor.forClass(ExpenseRequestDto.class);
            verify(expenseService).addExpense(captor.capture(), eq(1L), eq(TransactionOrigin.RECURRING_SYSTEM));
            assertThat(captor.getValue().countQuantityLimitDto().periodType()).isEqualTo(PeriodType.MONTHLY);
        }

        @Test
        void shouldBuildExpenseDtoWithCorrectFieldsWhenSettingsAreValid() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(200));
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(expenseSettings);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);
            when(limitManagerService.getLimitsByUserId(1L)).thenReturn(List.of());
            when(expenseSettings.getPeriodType()).thenReturn(PeriodType.MONTHLY);

            recurringExecutionService.execute(recurringSettings, executionDate);

            ArgumentCaptor<ExpenseRequestDto> captor = ArgumentCaptor.forClass(ExpenseRequestDto.class);
            verify(expenseService).addExpense(captor.capture(), eq(1L), eq(TransactionOrigin.RECURRING_SYSTEM));

            ExpenseDto capturedExpenseDto = captor.getValue().expenseDto();
            assertThat(capturedExpenseDto.userId()).isEqualTo(1L);
            assertThat(capturedExpenseDto.amount()).isEqualByComparingTo(BigDecimal.valueOf(200));
            assertThat(capturedExpenseDto.category()).isEqualTo(ExpenseCategory.FOOD);
            assertThat(capturedExpenseDto.createdAt()).isEqualTo(executionDate);
        }

        @Test
        void shouldThrowExceptionWhenWalletNotFound() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(expenseSettings);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenThrow(new RequestedEntityNotFoundException("wallet not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));

            verifyNoInteractions(expenseService);
        }



        @Test
        void shouldPropagateExceptionWhenValidatorThrowsUnhandledException() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(expenseSettings);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);
            when(limitManagerService.getLimitsByUserId(1L)).thenReturn(List.of());

            doThrow(new IllegalStateException("invalid settings")).when(expenseSettingsValidator)
                    .validate(recurringSettings, expenseSettings, wallet, List.of());

            assertThrows(IllegalStateException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));

            verifyNoInteractions(expenseService);
        }
    }

    @Nested
    class CreateSavingsTests {

        private Wallet wallet;

        @BeforeEach
        void setUp() {
            when(recurringSettings.getType()).thenReturn(RecurringType.SAVINGS);
            wallet = mock(Wallet.class);
        }

        @Test
        void shouldReturnExecutedWhenUserIdIsNull() {
            when(recurringSettings.getUserId()).thenReturn(null);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertThat(result).isEqualTo(RecurringExecutionResult.EXECUTED);
            verifyNoInteractions(walletManagerService, recurringSavingsValidator, piggyBankTransactionService);
        }

        @Test
        void shouldReturnExecutedWhenPiggyBankIdIsNull() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getPiggyBankId()).thenReturn(null);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertThat(result).isEqualTo(RecurringExecutionResult.EXECUTED);
            verifyNoInteractions(walletManagerService, recurringSavingsValidator, piggyBankTransactionService);
        }

        @Test
        void shouldValidateAndAddBalanceWhenSettingsAreValid() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getPiggyBankId()).thenReturn(10L);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(300));
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertThat(result).isEqualTo(RecurringExecutionResult.EXECUTED);
            verify(recurringSavingsValidator).validate(recurringSettings, wallet);
            verify(piggyBankTransactionService).addBalanceToPiggyBank(1L, 10L, BigDecimal.valueOf(300),
                    PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, null, TransactionOrigin.RECURRING_SYSTEM);
            verify(recurringSettings).setSkippedNotificationSent(false);
        }

        @Test
        void shouldDisableSettingsAndReturnExecutedWhenPiggyBankNotFound() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getPiggyBankId()).thenReturn(10L);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(300));
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);

            doThrow(new RequestedEntityNotFoundException("piggy bank not found")).when(piggyBankTransactionService)
                    .addBalanceToPiggyBank(1L, 10L, BigDecimal.valueOf(300),
                            PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, null, TransactionOrigin.RECURRING_SYSTEM);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertThat(result).isEqualTo(RecurringExecutionResult.EXECUTED);
            verify(recurringSettings).setEnable(false);
            verify(recurringSettings).setNextExecutionDate(null);
        }

        @Test
        void shouldThrowExceptionWhenWalletNotFound() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getPiggyBankId()).thenReturn(10L);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenThrow(new RequestedEntityNotFoundException("wallet not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));

            verifyNoInteractions(recurringSavingsValidator, piggyBankTransactionService);
        }


        @Test
        void shouldPropagateExceptionWhenValidatorThrowsUnhandledException() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getPiggyBankId()).thenReturn(10L);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);

            doThrow(new IllegalStateException("invalid settings")).when(recurringSavingsValidator).validate(recurringSettings, wallet);

            assertThrows(IllegalStateException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));

            verifyNoInteractions(piggyBankTransactionService);
        }
    }
}