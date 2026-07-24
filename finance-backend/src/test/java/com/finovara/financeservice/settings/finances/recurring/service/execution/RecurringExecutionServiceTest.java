package com.finovara.financeservice.settings.finances.recurring.service.execution;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.financeservice.expense.dto.ExpenseDto;
import com.finovara.financeservice.expense.dto.ExpenseRequestDto;
import com.finovara.financeservice.expense.service.ExpenseService;
import com.finovara.financeservice.piggybank.service.PiggyBankTransactionService;
import com.finovara.financeservice.revenue.dto.RevenueDto;
import com.finovara.financeservice.revenue.service.RevenueService;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.expense.repository.ExpenseSettingsRepository;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringType;
import com.finovara.financeservice.settings.finances.recurring.service.validator.RecurringExpenseValidator;
import com.finovara.financeservice.settings.finances.recurring.service.validator.RecurringRevenueValidator;
import com.finovara.financeservice.settings.finances.recurring.service.validator.RecurringSavingsValidator;
import com.finovara.financeservice.util.limit.manager.LimitManagerService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringExecutionServiceTest {

    @Mock
    private RevenueService revenueService;

    @Mock
    private ExpenseService expenseService;

    @Mock
    private PiggyBankTransactionService piggyBankTransactionService;

    @Mock
    private RecurringExpenseValidator recurringExpenseValidator;

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
                recurringExpenseValidator,
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
        void shouldDoNothingWhenTypeIsNull() {
            when(recurringSettings.getType()).thenReturn(null);

            recurringExecutionService.execute(recurringSettings, executionDate);

            verifyNoInteractions(revenueService, expenseService, piggyBankTransactionService,
                    recurringExpenseValidator, recurringRevenueValidator, recurringSavingsValidator);
        }

        @Test
        void shouldDelegateToCreateRevenueWhenTypeIsRevenue() {
            when(recurringSettings.getType()).thenReturn(RecurringType.REVENUE);
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getRevenueCategory()).thenReturn(RevenueCategory.SALARY);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(100));

            recurringExecutionService.execute(recurringSettings, executionDate);

            verify(recurringRevenueValidator, times(1)).validate(recurringSettings);
            verify(revenueService, times(1)).addRevenue(any(RevenueDto.class), eq(1L));
        }

        @Test
        void shouldDelegateToCreateExpenseWhenTypeIsExpense() {
            when(recurringSettings.getType()).thenReturn(RecurringType.EXPENSE);
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(null);

            recurringExecutionService.execute(recurringSettings, executionDate);

            verify(expenseSettingsRepository, times(1)).findByUserId(1L);
            verifyNoInteractions(walletManagerService, limitManagerService, expenseService);
        }

        @Test
        void shouldDelegateToCreateSavingsWhenTypeIsSavings() {
            when(recurringSettings.getType()).thenReturn(RecurringType.SAVINGS);
            when(recurringSettings.getUserId()).thenReturn(null);

            recurringExecutionService.execute(recurringSettings, executionDate);

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
        void shouldReturnWhenUserIdIsNull() {
            when(recurringSettings.getUserId()).thenReturn(null);

            recurringExecutionService.execute(recurringSettings, executionDate);

            verifyNoInteractions(recurringRevenueValidator, revenueService);
        }

        @Test
        void shouldReturnWhenRevenueCategoryIsNull() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getRevenueCategory()).thenReturn(null);

            recurringExecutionService.execute(recurringSettings, executionDate);

            verifyNoInteractions(recurringRevenueValidator, revenueService);
        }

        @Test
        void shouldValidateAndAddRevenueWhenSettingsAreValid() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getRevenueCategory()).thenReturn(RevenueCategory.SALARY);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(500));

            recurringExecutionService.execute(recurringSettings, executionDate);

            verify(recurringRevenueValidator, times(1)).validate(recurringSettings);
            verify(revenueService, times(1)).addRevenue(any(RevenueDto.class), eq(1L));
        }

        @Test
        void shouldBuildRevenueDtoWithCorrectFieldsWhenSettingsAreValid() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getRevenueCategory()).thenReturn(RevenueCategory.SALARY);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(500));

            recurringExecutionService.execute(recurringSettings, executionDate);

            ArgumentCaptor<RevenueDto> captor = ArgumentCaptor.forClass(RevenueDto.class);
            verify(revenueService).addRevenue(captor.capture(), eq(1L));
            RevenueDto capturedDto = captor.getValue();
            assertThat(capturedDto.userId()).isEqualTo(1L);
            assertThat(capturedDto.amount()).isEqualByComparingTo(BigDecimal.valueOf(500));
            assertThat(capturedDto.category()).isEqualTo(RevenueCategory.SALARY);
            assertThat(capturedDto.createdAt()).isEqualTo(executionDate);
        }

        @Test
        void shouldThrowExceptionWhenValidatorRejectsSettings() {
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
        void shouldReturnWhenUserIdIsNull() {
            when(recurringSettings.getUserId()).thenReturn(null);

            recurringExecutionService.execute(recurringSettings, executionDate);

            verifyNoInteractions(expenseSettingsRepository, walletManagerService, limitManagerService, expenseService);
        }

        @Test
        void shouldReturnWhenExpenseCategoryIsNull() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(null);

            recurringExecutionService.execute(recurringSettings, executionDate);

            verifyNoInteractions(expenseSettingsRepository, walletManagerService, limitManagerService, expenseService);
        }

        @Test
        void shouldReturnWhenExpenseSettingsNotFound() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(null);

            recurringExecutionService.execute(recurringSettings, executionDate);

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

            recurringExecutionService.execute(recurringSettings, executionDate);

            verify(recurringExpenseValidator, times(1)).validate(recurringSettings, expenseSettings, wallet, List.of());
            verify(expenseService, times(1)).addExpense(any(ExpenseRequestDto.class), eq(1L));
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
            verify(expenseService).addExpense(captor.capture(), eq(1L));
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
            verify(expenseService).addExpense(captor.capture(), eq(1L));
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
            verify(expenseService).addExpense(captor.capture(), eq(1L));
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
        void shouldThrowExceptionWhenValidatorRejectsSettings() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(expenseSettings);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);
            when(limitManagerService.getLimitsByUserId(1L)).thenReturn(List.of());
            doThrow(new IllegalStateException("invalid settings")).when(recurringExpenseValidator)
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
        void shouldReturnWhenUserIdIsNull() {
            when(recurringSettings.getUserId()).thenReturn(null);

            recurringExecutionService.execute(recurringSettings, executionDate);

            verifyNoInteractions(walletManagerService, recurringSavingsValidator, piggyBankTransactionService);
        }

        @Test
        void shouldReturnWhenPiggyBankIdIsNull() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getPiggyBankId()).thenReturn(null);

            recurringExecutionService.execute(recurringSettings, executionDate);

            verifyNoInteractions(walletManagerService, recurringSavingsValidator, piggyBankTransactionService);
        }

        @Test
        void shouldValidateAndAddBalanceWhenSettingsAreValid() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getPiggyBankId()).thenReturn(10L);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(300));
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);

            recurringExecutionService.execute(recurringSettings, executionDate);

            verify(recurringSavingsValidator, times(1)).validate(recurringSettings, wallet);
            verify(piggyBankTransactionService, times(1))
                    .addBalanceToPiggyBank(1L, 10L, BigDecimal.valueOf(300), PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING);
        }

        @Test
        void shouldDisableSettingsWhenPiggyBankNotFound() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getPiggyBankId()).thenReturn(10L);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(300));
            when(recurringSettings.getId()).thenReturn(99L);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);
            doThrow(new RequestedEntityNotFoundException("piggy bank not found"))
                    .when(piggyBankTransactionService)
                    .addBalanceToPiggyBank(1L, 10L, BigDecimal.valueOf(300), PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING);

            recurringExecutionService.execute(recurringSettings, executionDate);

            verify(recurringSettings, times(1)).setEnable(false);
            verify(recurringSettings, times(1)).setNextExecutionDate(null);
        }

        @Test
        void shouldNotThrowExceptionWhenPiggyBankNotFound() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getPiggyBankId()).thenReturn(10L);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(300));
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);
            doThrow(new RequestedEntityNotFoundException("piggy bank not found"))
                    .when(piggyBankTransactionService)
                    .addBalanceToPiggyBank(1L, 10L, BigDecimal.valueOf(300), PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING);

            recurringExecutionService.execute(recurringSettings, executionDate);

            verify(piggyBankTransactionService, times(1))
                    .addBalanceToPiggyBank(1L, 10L, BigDecimal.valueOf(300), PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING);
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
        void shouldThrowExceptionWhenValidatorRejectsSettings() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getPiggyBankId()).thenReturn(10L);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);
            doThrow(new IllegalStateException("invalid settings")).when(recurringSavingsValidator).validate(recurringSettings, wallet);

            assertThrows(IllegalStateException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));

            verifyNoInteractions(piggyBankTransactionService);
        }
    }
}