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
import com.finovara.financeservice.limit.model.Limit;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

    @Mock
    private RecurringSettings recurringSettings;

    @InjectMocks
    private RecurringExecutionService recurringExecutionService;

    private LocalDate executionDate;

    @BeforeEach
    void setUp() {
        executionDate = LocalDate.of(2026, 1, 1);
    }

    @Nested
    class Execute {

        @Test
        void shouldReturnExecutedWhenTypeIsNull() {
            when(recurringSettings.getType()).thenReturn(null);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verifyNoInteractions(revenueService, expenseService, piggyBankTransactionService, expenseSettingsValidator, recurringRevenueValidator, recurringSavingsValidator, expenseSettingsRepository, walletManagerService, limitManagerService);
        }

        @Test
        void shouldExecuteRevenueWhenTypeIsRevenue() {
            when(recurringSettings.getType()).thenReturn(RecurringType.REVENUE);
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getRevenueCategory()).thenReturn(RevenueCategory.SALARY);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(500));

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verify(recurringRevenueValidator).validate(recurringSettings);
            verify(revenueService).addRevenue(any(RevenueDto.class), eq(1L), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));
        }

        @Test
        void shouldExecuteExpenseWhenTypeIsExpense() {
            ExpenseSettings expenseSettings = mock(ExpenseSettings.class);
            Wallet wallet = mock(Wallet.class);
            List<Limit> limits = List.of();

            when(recurringSettings.getType()).thenReturn(RecurringType.EXPENSE);
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(expenseSettings);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);
            when(limitManagerService.getLimitsByUserId(1L)).thenReturn(limits);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verify(expenseSettingsValidator).validate(recurringSettings, expenseSettings, wallet, limits);
            verify(expenseService).addExpense(any(ExpenseRequestDto.class), eq(1L), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));
        }

        @Test
        void shouldExecuteSavingsWhenTypeIsSavings() {
            Wallet wallet = mock(Wallet.class);

            when(recurringSettings.getType()).thenReturn(RecurringType.SAVINGS);
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getPiggyBankId()).thenReturn(10L);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(300));
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verify(recurringSavingsValidator).validate(recurringSettings, wallet);
            verify(piggyBankTransactionService).addBalanceToPiggyBank(1L, 10L, BigDecimal.valueOf(300), PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, null, null, null, TransactionOrigin.RECURRING_SYSTEM);
        }
    }

    @Nested
    class ExecuteRevenue {

        @BeforeEach
        void setUp() {
            when(recurringSettings.getType()).thenReturn(RecurringType.REVENUE);
        }

        @Test
        void shouldReturnExecutedWhenUserIdIsNull() {
            when(recurringSettings.getUserId()).thenReturn(null);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verifyNoInteractions(recurringRevenueValidator, revenueService);
        }

        @Test
        void shouldReturnExecutedWhenRevenueCategoryIsNull() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getRevenueCategory()).thenReturn(null);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verifyNoInteractions(recurringRevenueValidator, revenueService);
        }

        @Test
        void shouldValidateSettingsBeforeAddingRevenue() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getRevenueCategory()).thenReturn(RevenueCategory.SALARY);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(500));

            recurringExecutionService.execute(recurringSettings, executionDate);

            org.mockito.InOrder order = inOrder(recurringRevenueValidator, revenueService);

            order.verify(recurringRevenueValidator).validate(recurringSettings);
            order.verify(revenueService).addRevenue(any(RevenueDto.class), eq(1L), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));
        }

        @Test
        void shouldBuildRevenueDtoWithCorrectFields() {
            BigDecimal amount = BigDecimal.valueOf(500);

            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getRevenueCategory()).thenReturn(RevenueCategory.SALARY);
            when(recurringSettings.getAmount()).thenReturn(amount);

            recurringExecutionService.execute(recurringSettings, executionDate);

            ArgumentCaptor<RevenueDto> captor = ArgumentCaptor.forClass(RevenueDto.class);

            verify(revenueService).addRevenue(captor.capture(), eq(1L), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));

            RevenueDto revenueDto = captor.getValue();

            assertNull(revenueDto.id());
            assertEquals(1L, revenueDto.userId());
            assertEquals(amount, revenueDto.amount());
            assertEquals(RevenueCategory.SALARY, revenueDto.category());
            assertEquals(executionDate, revenueDto.createdAt());
            assertNull(revenueDto.authorizationCode());
            assertNull(revenueDto.riskVerificationSourceEventId());
        }

        @Test
        void shouldReturnSkippedWhenValidatorThrowsInvalidInputException() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getRevenueCategory()).thenReturn(RevenueCategory.SALARY);

            doThrow(new InvalidInputException("invalid settings")).when(recurringRevenueValidator).validate(recurringSettings);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.SKIPPED, result);
            verify(recurringRevenueValidator).validate(recurringSettings);
            verifyNoInteractions(revenueService);
        }

        @Test
        void shouldReturnSkippedWhenRevenueServiceThrowsInvalidInputException() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getRevenueCategory()).thenReturn(RevenueCategory.SALARY);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(500));

            doThrow(new InvalidInputException("invalid revenue")).when(revenueService).addRevenue(any(RevenueDto.class), eq(1L), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.SKIPPED, result);
        }

        @Test
        void shouldThrowExceptionWhenValidatorThrowsUnexpectedException() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getRevenueCategory()).thenReturn(RevenueCategory.SALARY);

            doThrow(new IllegalStateException("unexpected")).when(recurringRevenueValidator).validate(recurringSettings);

            assertThrows(IllegalStateException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));

            verifyNoInteractions(revenueService);
        }

        @Test
        void shouldThrowExceptionWhenRevenueServiceThrowsUnexpectedException() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getRevenueCategory()).thenReturn(RevenueCategory.SALARY);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(500));

            doThrow(new IllegalStateException("unexpected")).when(revenueService).addRevenue(any(RevenueDto.class), eq(1L), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));

            assertThrows(IllegalStateException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));
        }
    }

    @Nested
    class ExecuteExpense {

        private ExpenseSettings expenseSettings;
        private Wallet wallet;
        private List<Limit> limits;

        @BeforeEach
        void setUp() {
            when(recurringSettings.getType()).thenReturn(RecurringType.EXPENSE);

            expenseSettings = mock(ExpenseSettings.class);
            wallet = mock(Wallet.class);
            limits = List.of();
        }

        @Test
        void shouldReturnExecutedWhenUserIdIsNull() {
            when(recurringSettings.getUserId()).thenReturn(null);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verifyNoInteractions(expenseSettingsRepository, walletManagerService, limitManagerService, expenseSettingsValidator, expenseService);
        }

        @Test
        void shouldReturnExecutedWhenExpenseCategoryIsNull() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(null);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verifyNoInteractions(expenseSettingsRepository, walletManagerService, limitManagerService, expenseSettingsValidator, expenseService);
        }

        @Test
        void shouldReturnExecutedWhenExpenseSettingsDoNotExist() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(null);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verify(expenseSettingsRepository).findByUserId(1L);
            verifyNoInteractions(walletManagerService, limitManagerService, expenseSettingsValidator, expenseService);
        }

        @Test
        void shouldLoadWalletAndLimitsBeforeValidation() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(expenseSettings);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);
            when(limitManagerService.getLimitsByUserId(1L)).thenReturn(limits);

            recurringExecutionService.execute(recurringSettings, executionDate);

            org.mockito.InOrder order = inOrder(walletManagerService, limitManagerService, expenseSettingsValidator);

            order.verify(walletManagerService).getWalletByUserIdOrThrow(1L);
            order.verify(limitManagerService).getLimitsByUserId(1L);
            order.verify(expenseSettingsValidator).validate(recurringSettings, expenseSettings, wallet, limits);
        }

        @Test
        void shouldValidateAndAddExpenseWhenSettingsAreValid() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(200));
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(expenseSettings);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);
            when(limitManagerService.getLimitsByUserId(1L)).thenReturn(limits);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verify(expenseSettingsValidator).validate(recurringSettings, expenseSettings, wallet, limits);
            verify(expenseService).addExpense(any(ExpenseRequestDto.class), eq(1L), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));
            verify(recurringSettings).setSkippedNotificationSent(false);
        }

        @Test
        void shouldBuildExpenseRequestWithCorrectExpenseDto() {
            BigDecimal amount = BigDecimal.valueOf(200);

            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(recurringSettings.getAmount()).thenReturn(amount);
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(expenseSettings);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);
            when(limitManagerService.getLimitsByUserId(1L)).thenReturn(limits);

            recurringExecutionService.execute(recurringSettings, executionDate);

            ArgumentCaptor<ExpenseRequestDto> captor = ArgumentCaptor.forClass(ExpenseRequestDto.class);

            verify(expenseService).addExpense(captor.capture(), eq(1L), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));

            ExpenseDto expenseDto = captor.getValue().expenseDto();

            assertNull(expenseDto.id());
            assertEquals(1L, expenseDto.userId());
            assertEquals(amount, expenseDto.amount());
            assertEquals(ExpenseCategory.FOOD, expenseDto.category());
            assertEquals(executionDate, expenseDto.createdAt());
        }

        @Test
        void shouldBuildExpenseRequestWithNullAuthorizationData() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(expenseSettings);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);
            when(limitManagerService.getLimitsByUserId(1L)).thenReturn(limits);

            recurringExecutionService.execute(recurringSettings, executionDate);

            ArgumentCaptor<ExpenseRequestDto> captor = ArgumentCaptor.forClass(ExpenseRequestDto.class);

            verify(expenseService).addExpense(captor.capture(), eq(1L), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));

            ExpenseRequestDto requestDto = captor.getValue();

            assertNull(requestDto.confirmPasswordDto().password());
            assertNull(requestDto.confirmAuthorizationCodeDto().code());
            assertNull(requestDto.riskVerificationSourceEventId());
        }

        @Test
        void shouldUseRecurringSettingsPeriodTypeWhenExpenseSettingsPeriodTypeIsNull() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(recurringSettings.getPeriodType()).thenReturn(PeriodType.MONTHLY);
            when(expenseSettings.getPeriodType()).thenReturn(null);
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(expenseSettings);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);
            when(limitManagerService.getLimitsByUserId(1L)).thenReturn(limits);

            recurringExecutionService.execute(recurringSettings, executionDate);

            ArgumentCaptor<ExpenseRequestDto> captor = ArgumentCaptor.forClass(ExpenseRequestDto.class);

            verify(expenseService).addExpense(captor.capture(), eq(1L), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));

            assertEquals(PeriodType.MONTHLY, captor.getValue().countQuantityLimitDto().periodType());
        }

        @Test
        void shouldUseNullPeriodTypeWhenBothPeriodTypesAreNull() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(recurringSettings.getPeriodType()).thenReturn(null);
            when(expenseSettings.getPeriodType()).thenReturn(null);
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(expenseSettings);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);
            when(limitManagerService.getLimitsByUserId(1L)).thenReturn(limits);

            recurringExecutionService.execute(recurringSettings, executionDate);

            ArgumentCaptor<ExpenseRequestDto> captor = ArgumentCaptor.forClass(ExpenseRequestDto.class);

            verify(expenseService).addExpense(captor.capture(), eq(1L), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));

            assertNull(captor.getValue().countQuantityLimitDto().periodType());
        }

        @Test
        void shouldReturnSkippedWhenValidatorThrowsInvalidInputException() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(expenseSettings);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);
            when(limitManagerService.getLimitsByUserId(1L)).thenReturn(limits);

            doThrow(new InvalidInputException("invalid settings")).when(expenseSettingsValidator).validate(recurringSettings, expenseSettings, wallet, limits);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.SKIPPED, result);
            verifyNoInteractions(expenseService);
            verify(recurringSettings, never()).setSkippedNotificationSent(false);
        }

        @Test
        void shouldReturnSkippedWhenExpenseServiceThrowsInvalidInputException() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(expenseSettings);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);
            when(limitManagerService.getLimitsByUserId(1L)).thenReturn(limits);

            doThrow(new InvalidInputException("invalid expense")).when(expenseService).addExpense(any(ExpenseRequestDto.class), eq(1L), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.SKIPPED, result);
            verify(recurringSettings, never()).setSkippedNotificationSent(false);
        }

        @Test
        void shouldThrowExceptionWhenWalletIsNotFound() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(expenseSettings);

            doThrow(new RequestedEntityNotFoundException("wallet not found")).when(walletManagerService).getWalletByUserIdOrThrow(1L);

            assertThrows(RequestedEntityNotFoundException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));

            verifyNoInteractions(limitManagerService, expenseSettingsValidator, expenseService);
        }

        @Test
        void shouldThrowExceptionWhenLimitManagerThrowsUnexpectedException() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(expenseSettings);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);

            doThrow(new IllegalStateException("unexpected")).when(limitManagerService).getLimitsByUserId(1L);

            assertThrows(IllegalStateException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));

            verifyNoInteractions(expenseSettingsValidator, expenseService);
        }

        @Test
        void shouldThrowExceptionWhenValidatorThrowsUnexpectedException() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(expenseSettings);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);
            when(limitManagerService.getLimitsByUserId(1L)).thenReturn(limits);

            doThrow(new IllegalStateException("unexpected")).when(expenseSettingsValidator).validate(recurringSettings, expenseSettings, wallet, limits);

            assertThrows(IllegalStateException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));

            verifyNoInteractions(expenseService);
        }

        @Test
        void shouldThrowExceptionWhenExpenseServiceThrowsUnexpectedException() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(expenseSettingsRepository.findByUserId(1L)).thenReturn(expenseSettings);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);
            when(limitManagerService.getLimitsByUserId(1L)).thenReturn(limits);

            doThrow(new IllegalStateException("unexpected")).when(expenseService).addExpense(any(ExpenseRequestDto.class), eq(1L), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));

            assertThrows(IllegalStateException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));

            verify(recurringSettings, never()).setSkippedNotificationSent(false);
        }
    }

    @Nested
    class ExecuteSavings {

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

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verifyNoInteractions(walletManagerService, recurringSavingsValidator, piggyBankTransactionService);
        }

        @Test
        void shouldReturnExecutedWhenPiggyBankIdIsNull() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getPiggyBankId()).thenReturn(null);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verifyNoInteractions(walletManagerService, recurringSavingsValidator, piggyBankTransactionService);
        }

        @Test
        void shouldValidateAndExecuteSavingsWhenSettingsAreValid() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getPiggyBankId()).thenReturn(10L);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(300));
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verify(recurringSavingsValidator).validate(recurringSettings, wallet);
            verify(piggyBankTransactionService).addBalanceToPiggyBank(1L, 10L, BigDecimal.valueOf(300), PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, null, null, null, TransactionOrigin.RECURRING_SYSTEM);
            verify(recurringSettings).setSkippedNotificationSent(false);
        }

        @Test
        void shouldValidateSavingsBeforeAddingBalance() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getPiggyBankId()).thenReturn(10L);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(300));
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);

            recurringExecutionService.execute(recurringSettings, executionDate);

            org.mockito.InOrder order = inOrder(recurringSavingsValidator, piggyBankTransactionService);

            order.verify(recurringSavingsValidator).validate(recurringSettings, wallet);
            order.verify(piggyBankTransactionService).addBalanceToPiggyBank(1L, 10L, BigDecimal.valueOf(300), PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, null, null, null, TransactionOrigin.RECURRING_SYSTEM);
        }

        @Test
        void shouldReturnSkippedWhenValidatorThrowsInvalidInputException() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getPiggyBankId()).thenReturn(10L);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);

            doThrow(new InvalidInputException("invalid settings")).when(recurringSavingsValidator).validate(recurringSettings, wallet);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.SKIPPED, result);
            verifyNoInteractions(piggyBankTransactionService);
            verify(recurringSettings, never()).setSkippedNotificationSent(false);
        }

        @Test
        void shouldReturnSkippedWhenPiggyBankTransactionThrowsInvalidInputException() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getPiggyBankId()).thenReturn(10L);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(300));
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);

            doThrow(new InvalidInputException("invalid transaction")).when(piggyBankTransactionService).addBalanceToPiggyBank(1L, 10L, BigDecimal.valueOf(300), PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, null, null, null, TransactionOrigin.RECURRING_SYSTEM);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.SKIPPED, result);
            verify(recurringSettings, never()).setSkippedNotificationSent(false);
        }

        @Test
        void shouldDisableRecurringSettingsWhenPiggyBankIsNotFound() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getPiggyBankId()).thenReturn(10L);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(300));
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);

            doThrow(new RequestedEntityNotFoundException("piggy bank not found")).when(piggyBankTransactionService).addBalanceToPiggyBank(1L, 10L, BigDecimal.valueOf(300), PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, null, null, null, TransactionOrigin.RECURRING_SYSTEM);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verify(recurringSettings).setEnable(false);
            verify(recurringSettings).setNextExecutionDate(null);
            verify(recurringSettings, never()).setSkippedNotificationSent(false);
        }

        @Test
        void shouldDisableRecurringSettingsWhenPiggyBankIsNotFoundDuringValidation() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getPiggyBankId()).thenReturn(10L);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);

            doThrow(new RequestedEntityNotFoundException("piggy bank not found")).when(recurringSavingsValidator).validate(recurringSettings, wallet);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verify(recurringSettings).setEnable(false);
            verify(recurringSettings).setNextExecutionDate(null);
            verifyNoInteractions(piggyBankTransactionService);
            verify(recurringSettings, never()).setSkippedNotificationSent(false);
        }

        @Test
        void shouldThrowExceptionWhenWalletIsNotFound() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getPiggyBankId()).thenReturn(10L);

            doThrow(new RequestedEntityNotFoundException("wallet not found")).when(walletManagerService).getWalletByUserIdOrThrow(1L);

            assertThrows(RequestedEntityNotFoundException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));

            verifyNoInteractions(recurringSavingsValidator, piggyBankTransactionService);
            verify(recurringSettings, never()).setEnable(false);
            verify(recurringSettings, never()).setNextExecutionDate(null);
        }

        @Test
        void shouldThrowExceptionWhenValidatorThrowsUnexpectedException() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getPiggyBankId()).thenReturn(10L);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);

            doThrow(new IllegalStateException("unexpected")).when(recurringSavingsValidator).validate(recurringSettings, wallet);

            assertThrows(IllegalStateException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));

            verifyNoInteractions(piggyBankTransactionService);
            verify(recurringSettings, never()).setSkippedNotificationSent(false);
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankTransactionThrowsUnexpectedException() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getPiggyBankId()).thenReturn(10L);
            when(recurringSettings.getAmount()).thenReturn(BigDecimal.valueOf(300));
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);

            doThrow(new IllegalStateException("unexpected")).when(piggyBankTransactionService).addBalanceToPiggyBank(1L, 10L, BigDecimal.valueOf(300), PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, null, null, null, TransactionOrigin.RECURRING_SYSTEM);

            assertThrows(IllegalStateException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));

            verify(recurringSettings, never()).setSkippedNotificationSent(false);
        }

        @Test
        void shouldPassNullAmountWhenAmountIsNull() {
            when(recurringSettings.getUserId()).thenReturn(1L);
            when(recurringSettings.getPiggyBankId()).thenReturn(10L);
            when(recurringSettings.getAmount()).thenReturn(null);
            when(walletManagerService.getWalletByUserIdOrThrow(1L)).thenReturn(wallet);

            recurringExecutionService.execute(recurringSettings, executionDate);

            verify(piggyBankTransactionService).addBalanceToPiggyBank(1L, 10L, null, PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, null, null, null, TransactionOrigin.RECURRING_SYSTEM);
        }
    }
}
