package com.finovara.financeservice.settings.finances.recurring.service.execution;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.mainaccount.activity.model.PiggyBankActivityType;
import com.finovara.contracts.user.authorization.dto.ConfirmAuthorizationCodeDto;
import com.finovara.contracts.user.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.RecurringType;
import com.finovara.financeservice.expense.dto.ExpenseDto;
import com.finovara.financeservice.expense.dto.ExpenseRequestDto;
import com.finovara.financeservice.expense.service.ExpenseService;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.piggybank.repository.PiggyBankRepository;
import com.finovara.financeservice.piggybank.service.PiggyBankTransactionService;
import com.finovara.financeservice.revenue.dto.RevenueDto;
import com.finovara.financeservice.revenue.service.RevenueService;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.expense.quantitylimit.dto.CountQuantityLimitDto;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringDescription;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.service.validator.ExpenseSettingsValidator;
import com.finovara.financeservice.settings.finances.recurring.service.validator.RecurringRevenueValidator;
import com.finovara.financeservice.settings.finances.recurring.service.validator.RecurringSavingsValidator;
import com.finovara.financeservice.util.limit.manager.LimitManagerService;
import com.finovara.financeservice.util.settings.SettingsManager;
import com.finovara.financeservice.util.transaction.TransactionOrigin;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringExecutionServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PIGGY_BANK_ID = 5L;
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(120);
    private static final LocalDate DATE = LocalDate.of(2025, 3, 1);
    private static final PeriodType RECURRING_PERIOD_TYPE = PeriodType.values()[0];
    private static final PeriodType EXPENSE_SETTINGS_PERIOD_TYPE = PeriodType.values()[PeriodType.values().length - 1];

    @Mock
    private SettingsManager settingsManager;

    @Mock
    private RevenueService revenueService;

    @Mock
    private ExpenseService expenseService;

    @Mock
    private PiggyBankTransactionService piggyBankTransactionService;

    @Mock
    private PiggyBankRepository piggyBankRepository;

    @Mock
    private ExpenseSettingsValidator expenseSettingsValidator;

    @Mock
    private RecurringRevenueValidator recurringRevenueValidator;

    @Mock
    private RecurringSavingsValidator recurringSavingsValidator;

    @Mock
    private WalletService walletService;

    @Mock
    private WalletManagerService walletManagerService;

    @Mock
    private LimitManagerService limitManagerService;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private RecurringSettings settings;

    @Mock
    private ExpenseSettings expenseSettings;

    @Mock
    private Wallet wallet;

    @Mock
    private Limit limit;

    @InjectMocks
    private RecurringExecutionService service;

    private List<Limit> limits;
    private RecurringExecutionContext context;

    @BeforeEach
    void setUp() {
        limits = List.of(limit);
        context = new RecurringExecutionContext(wallet, expenseSettings, limits, true);
    }

    @Nested
    class PrepareContext {

        @BeforeEach
        void setUp() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
        }

        @Test
        void shouldLockWalletBeforeLoadingWalletWhenPreparingContext() {
            doReturn(RecurringType.REVENUE).when(settings).getType();

            service.prepareContext(settings);

            InOrder inOrder = inOrder(walletService, walletManagerService);
            inOrder.verify(walletService).lockWalletForUpdate(USER_ID);
            inOrder.verify(walletManagerService).getWalletByUserIdOrThrow(USER_ID);
        }

        @Test
        void shouldReturnContextWithExpenseSettingsAndLimitsWhenTypeIsExpense() {
            doReturn(RecurringType.EXPENSE).when(settings).getType();
            when(settingsManager.getExpenseSettingsByUserId(USER_ID)).thenReturn(expenseSettings);
            when(limitManagerService.getLimitsByUserId(USER_ID)).thenReturn(limits);

            RecurringExecutionContext result = service.prepareContext(settings);

            assertEquals(new RecurringExecutionContext(wallet, expenseSettings, limits, false), result);
        }

        @Test
        void shouldReturnContextWithPiggyBankExistingWhenTypeIsSavingsAndPiggyBankExists() {
            doReturn(RecurringType.SAVINGS).when(settings).getType();
            when(settings.getPiggyBankId()).thenReturn(PIGGY_BANK_ID);
            doReturn(Optional.of(new Object())).when(piggyBankRepository).findByIdAndUserId(PIGGY_BANK_ID, USER_ID);

            RecurringExecutionContext result = service.prepareContext(settings);

            assertEquals(new RecurringExecutionContext(wallet, null, List.of(), true), result);
            verifyNoInteractions(settingsManager, limitManagerService);
        }

        @Test
        void shouldReturnContextWithoutPiggyBankWhenTypeIsSavingsAndPiggyBankDoesNotExist() {
            doReturn(RecurringType.SAVINGS).when(settings).getType();
            when(settings.getPiggyBankId()).thenReturn(PIGGY_BANK_ID);
            doReturn(Optional.empty()).when(piggyBankRepository).findByIdAndUserId(PIGGY_BANK_ID, USER_ID);

            RecurringExecutionContext result = service.prepareContext(settings);

            assertFalse(result.piggyBankExists());
        }

        @Test
        void shouldReturnContextWithoutPiggyBankWhenTypeIsSavingsAndPiggyBankIdIsNull() {
            doReturn(RecurringType.SAVINGS).when(settings).getType();
            when(settings.getPiggyBankId()).thenReturn(null);

            RecurringExecutionContext result = service.prepareContext(settings);

            assertFalse(result.piggyBankExists());
            verifyNoInteractions(piggyBankRepository);
        }

        @Test
        void shouldReturnContextWithWalletOnlyWhenTypeIsRevenue() {
            doReturn(RecurringType.REVENUE).when(settings).getType();

            RecurringExecutionContext result = service.prepareContext(settings);

            assertEquals(new RecurringExecutionContext(wallet, null, List.of(), false), result);
            verifyNoInteractions(settingsManager, limitManagerService, piggyBankRepository);
        }

        @Test
        void shouldThrowExceptionWhenTypeIsNull() {
            doReturn(null).when(settings).getType();

            assertThrows(NullPointerException.class, () -> service.prepareContext(settings));
        }
    }

    @Nested
    class Execute {

        @Test
        void shouldReturnExecutedWithoutInteractionsWhenTypeIsNull() {
            doReturn(null).when(settings).getType();

            RecurringExecutionResult result = service.execute(settings, DATE, context);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verifyNoInteractions(revenueService, expenseService, piggyBankTransactionService);
        }
    }

    @Nested
    class ExecuteRevenue {

        @BeforeEach
        void setUp() {
            doReturn(RecurringType.REVENUE).when(settings).getType();
        }

        @Test
        void shouldReturnExecutedWithoutCreatingRevenueWhenUserIdIsNull() {
            when(settings.getUserId()).thenReturn(null);

            RecurringExecutionResult result = service.execute(settings, DATE, context);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verifyNoInteractions(revenueService, recurringRevenueValidator);
        }

        @Test
        void shouldReturnExecutedWithoutCreatingRevenueWhenRevenueCategoryIsNull() {
            when(settings.getUserId()).thenReturn(USER_ID);
            doReturn(null).when(settings).getRevenueCategory();

            RecurringExecutionResult result = service.execute(settings, DATE, context);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verifyNoInteractions(revenueService, recurringRevenueValidator);
        }

        @Test
        void shouldCreateRevenueAndReturnExecutedWhenSettingsAreValid() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getAmount()).thenReturn(AMOUNT);
            RevenueDto expected = new RevenueDto(null, USER_ID, AMOUNT, settings.getRevenueCategory(), DATE, RecurringDescription.REVENUE.label(), null, null);

            RecurringExecutionResult result = service.execute(settings, DATE, context);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verify(recurringRevenueValidator).validate(settings);
            verify(revenueService).addRevenue(expected, USER_ID, null, TransactionOrigin.RECURRING_SYSTEM);
        }

        @Test
        void shouldValidateBeforeCreatingRevenueWhenSettingsAreValid() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getAmount()).thenReturn(AMOUNT);
            RevenueDto expected = new RevenueDto(null, USER_ID, AMOUNT, settings.getRevenueCategory(), DATE, RecurringDescription.REVENUE.label(), null, null);

            service.execute(settings, DATE, context);

            InOrder inOrder = inOrder(recurringRevenueValidator, revenueService);
            inOrder.verify(recurringRevenueValidator).validate(settings);
            inOrder.verify(revenueService).addRevenue(expected, USER_ID, null, TransactionOrigin.RECURRING_SYSTEM);
        }

        @Test
        void shouldReturnSkippedWhenRevenueValidationFails() {
            when(settings.getUserId()).thenReturn(USER_ID);
            doThrow(new InvalidInputException("invalid")).when(recurringRevenueValidator).validate(settings);

            RecurringExecutionResult result = service.execute(settings, DATE, context);

            assertEquals(RecurringExecutionResult.SKIPPED, result);
            verifyNoInteractions(revenueService);
        }

        @Test
        void shouldReturnSkippedWhenAddingRevenueThrowsInvalidInput() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getAmount()).thenReturn(AMOUNT);
            RevenueDto expected = new RevenueDto(null, USER_ID, AMOUNT, settings.getRevenueCategory(), DATE, RecurringDescription.REVENUE.label(), null, null);
            doThrow(new InvalidInputException("invalid")).when(revenueService).addRevenue(expected, USER_ID, null, TransactionOrigin.RECURRING_SYSTEM);

            RecurringExecutionResult result = service.execute(settings, DATE, context);

            assertEquals(RecurringExecutionResult.SKIPPED, result);
        }

        @Test
        void shouldThrowExceptionWhenRevenueValidationThrowsUnexpectedException() {
            when(settings.getUserId()).thenReturn(USER_ID);
            doThrow(new IllegalStateException("unexpected")).when(recurringRevenueValidator).validate(settings);

            assertThrows(IllegalStateException.class, () -> service.execute(settings, DATE, context));

            verifyNoInteractions(revenueService);
        }
    }

    @Nested
    class ExecuteExpense {

        @BeforeEach
        void setUp() {
            doReturn(RecurringType.EXPENSE).when(settings).getType();
        }

        @Test
        void shouldReturnExecutedWithoutCreatingExpenseWhenUserIdIsNull() {
            when(settings.getUserId()).thenReturn(null);

            RecurringExecutionResult result = service.execute(settings, DATE, context);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verifyNoInteractions(expenseService, expenseSettingsValidator);
        }

        @Test
        void shouldReturnExecutedWithoutCreatingExpenseWhenExpenseCategoryIsNull() {
            when(settings.getUserId()).thenReturn(USER_ID);
            doReturn(null).when(settings).getExpenseCategory();

            RecurringExecutionResult result = service.execute(settings, DATE, context);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verifyNoInteractions(expenseService, expenseSettingsValidator);
        }

        @Test
        void shouldReturnExecutedWithoutCreatingExpenseWhenContextHasNoExpenseSettings() {
            RecurringExecutionContext contextWithoutSettings = new RecurringExecutionContext(wallet, null, List.of(), false);
            when(settings.getUserId()).thenReturn(USER_ID);

            RecurringExecutionResult result = service.execute(settings, DATE, contextWithoutSettings);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verifyNoInteractions(expenseService, expenseSettingsValidator);
        }

        @Test
        void shouldCreateExpenseWithExpenseSettingsPeriodTypeWhenPeriodTypeIsDefinedInExpenseSettings() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getAmount()).thenReturn(AMOUNT);
            when(expenseSettings.getPeriodType()).thenReturn(EXPENSE_SETTINGS_PERIOD_TYPE);
            when(expenseSettings.isCountQuantityLimitEnabled()).thenReturn(true);
            ExpenseRequestDto expected = new ExpenseRequestDto(
                    new ExpenseDto(null, USER_ID, AMOUNT, settings.getExpenseCategory(), DATE, RecurringDescription.EXPENSE.label()),
                    new ConfirmPasswordDto(null),
                    new ConfirmAuthorizationCodeDto(null),
                    new CountQuantityLimitDto(true, EXPENSE_SETTINGS_PERIOD_TYPE, expenseSettings.getNumberOfQuantityLimit(), null),
                    null
            );

            RecurringExecutionResult result = service.execute(settings, DATE, context);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verify(expenseSettingsValidator).validate(settings, expenseSettings, wallet, limits);
            verify(expenseService).addExpense(expected, USER_ID, null, TransactionOrigin.RECURRING_SYSTEM);
        }

        @Test
        void shouldCreateExpenseWithRecurringPeriodTypeWhenExpenseSettingsPeriodTypeIsNull() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getAmount()).thenReturn(AMOUNT);
            doReturn(RECURRING_PERIOD_TYPE).when(settings).getPeriodType();
            when(expenseSettings.getPeriodType()).thenReturn(null);
            when(expenseSettings.isCountQuantityLimitEnabled()).thenReturn(false);
            ExpenseRequestDto expected = new ExpenseRequestDto(
                    new ExpenseDto(null, USER_ID, AMOUNT, settings.getExpenseCategory(), DATE, RecurringDescription.EXPENSE.label()),
                    new ConfirmPasswordDto(null),
                    new ConfirmAuthorizationCodeDto(null),
                    new CountQuantityLimitDto(false, RECURRING_PERIOD_TYPE, expenseSettings.getNumberOfQuantityLimit(), null),
                    null
            );

            RecurringExecutionResult result = service.execute(settings, DATE, context);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verify(expenseService).addExpense(expected, USER_ID, null, TransactionOrigin.RECURRING_SYSTEM);
        }

        @Test
        void shouldReturnSkippedWhenExpenseValidationFails() {
            when(settings.getUserId()).thenReturn(USER_ID);
            doThrow(new InvalidInputException("invalid")).when(expenseSettingsValidator).validate(settings, expenseSettings, wallet, limits);

            RecurringExecutionResult result = service.execute(settings, DATE, context);

            assertEquals(RecurringExecutionResult.SKIPPED, result);
            verifyNoInteractions(expenseService);
        }

        @Test
        void shouldReturnSkippedWhenAddingExpenseThrowsInvalidInput() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getAmount()).thenReturn(AMOUNT);
            when(expenseSettings.getPeriodType()).thenReturn(EXPENSE_SETTINGS_PERIOD_TYPE);
            when(expenseSettings.isCountQuantityLimitEnabled()).thenReturn(true);
            ExpenseRequestDto expected = new ExpenseRequestDto(
                    new ExpenseDto(null, USER_ID, AMOUNT, settings.getExpenseCategory(), DATE, RecurringDescription.EXPENSE.label()),
                    new ConfirmPasswordDto(null),
                    new ConfirmAuthorizationCodeDto(null),
                    new CountQuantityLimitDto(true, EXPENSE_SETTINGS_PERIOD_TYPE, expenseSettings.getNumberOfQuantityLimit(), null),
                    null
            );
            doThrow(new InvalidInputException("invalid")).when(expenseService).addExpense(expected, USER_ID, null, TransactionOrigin.RECURRING_SYSTEM);

            RecurringExecutionResult result = service.execute(settings, DATE, context);

            assertEquals(RecurringExecutionResult.SKIPPED, result);
        }

        @Test
        void shouldThrowExceptionWhenExpenseValidationThrowsUnexpectedException() {
            when(settings.getUserId()).thenReturn(USER_ID);
            doThrow(new IllegalStateException("unexpected")).when(expenseSettingsValidator).validate(settings, expenseSettings, wallet, limits);

            assertThrows(IllegalStateException.class, () -> service.execute(settings, DATE, context));

            verifyNoInteractions(expenseService);
        }
    }

    @Nested
    class ExecuteSavings {

        @BeforeEach
        void setUp() {
            doReturn(RecurringType.SAVINGS).when(settings).getType();
        }

        @Test
        void shouldReturnExecutedWithoutSavingWhenUserIdIsNull() {
            when(settings.getUserId()).thenReturn(null);

            RecurringExecutionResult result = service.execute(settings, DATE, context);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verifyNoInteractions(piggyBankTransactionService, recurringSavingsValidator);
        }

        @Test
        void shouldReturnExecutedWithoutSavingWhenPiggyBankIdIsNull() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getPiggyBankId()).thenReturn(null);

            RecurringExecutionResult result = service.execute(settings, DATE, context);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verifyNoInteractions(piggyBankTransactionService, recurringSavingsValidator);
        }

        @Test
        void shouldDisableSettingsAndReturnExecutedWhenPiggyBankDoesNotExist() {
            RecurringExecutionContext contextWithoutPiggyBank = new RecurringExecutionContext(wallet, null, List.of(), false);
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getPiggyBankId()).thenReturn(PIGGY_BANK_ID);

            RecurringExecutionResult result = service.execute(settings, DATE, contextWithoutPiggyBank);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verify(settings).setEnable(false);
            verify(settings).setNextExecutionDate(null);
            verifyNoInteractions(piggyBankTransactionService, recurringSavingsValidator);
        }

        @Test
        void shouldAddBalanceToPiggyBankAndReturnExecutedWhenSettingsAreValid() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getPiggyBankId()).thenReturn(PIGGY_BANK_ID);
            when(settings.getAmount()).thenReturn(AMOUNT);

            RecurringExecutionResult result = service.execute(settings, DATE, context);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verify(recurringSavingsValidator).validate(settings, wallet);
            verify(piggyBankTransactionService).addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, null, null, null, TransactionOrigin.RECURRING_SYSTEM);
            verify(settings, never()).setEnable(false);
        }

        @Test
        void shouldReturnSkippedWhenSavingsValidationFails() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getPiggyBankId()).thenReturn(PIGGY_BANK_ID);
            doThrow(new InvalidInputException("invalid")).when(recurringSavingsValidator).validate(settings, wallet);

            RecurringExecutionResult result = service.execute(settings, DATE, context);

            assertEquals(RecurringExecutionResult.SKIPPED, result);
            verifyNoInteractions(piggyBankTransactionService);
        }

        @Test
        void shouldReturnSkippedWhenAddingBalanceThrowsInvalidInput() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getPiggyBankId()).thenReturn(PIGGY_BANK_ID);
            when(settings.getAmount()).thenReturn(AMOUNT);
            doThrow(new InvalidInputException("invalid")).when(piggyBankTransactionService)
                    .addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, AMOUNT, PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, null, null, null, TransactionOrigin.RECURRING_SYSTEM);

            RecurringExecutionResult result = service.execute(settings, DATE, context);

            assertEquals(RecurringExecutionResult.SKIPPED, result);
        }

        @Test
        void shouldThrowExceptionWhenSavingsValidationThrowsUnexpectedException() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getPiggyBankId()).thenReturn(PIGGY_BANK_ID);
            doThrow(new IllegalStateException("unexpected")).when(recurringSavingsValidator).validate(settings, wallet);

            assertThrows(IllegalStateException.class, () -> service.execute(settings, DATE, context));

            verifyNoInteractions(piggyBankTransactionService);
        }

        @Test
        void shouldKeepSettingsEnabledWhenPiggyBankExists() {
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getPiggyBankId()).thenReturn(PIGGY_BANK_ID);

            RecurringExecutionResult result = service.execute(settings, DATE, context);

            assertTrue(context.piggyBankExists());
            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verify(settings, never()).setNextExecutionDate(null);
        }
    }
}