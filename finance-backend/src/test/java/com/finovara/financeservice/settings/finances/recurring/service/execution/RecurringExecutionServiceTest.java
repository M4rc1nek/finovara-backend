package com.finovara.financeservice.settings.finances.recurring.service.execution;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.mainaccount.activity.model.PiggyBankActivityType;
import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.contracts.util.model.RecurringType;
import com.finovara.contracts.util.model.RevenueCategory;
import com.finovara.financeservice.expense.dto.ExpenseDto;
import com.finovara.financeservice.expense.dto.ExpenseRequestDto;
import com.finovara.financeservice.expense.service.ExpenseService;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.piggybank.service.PiggyBankTransactionService;
import com.finovara.financeservice.revenue.dto.RevenueDto;
import com.finovara.financeservice.revenue.service.RevenueService;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.service.validator.ExpenseSettingsValidator;
import com.finovara.financeservice.settings.finances.recurring.service.validator.RecurringRevenueValidator;
import com.finovara.financeservice.settings.finances.recurring.service.validator.RecurringSavingsValidator;
import com.finovara.financeservice.util.limit.manager.LimitManagerService;
import com.finovara.financeservice.util.settings.SettingsManager;
import com.finovara.financeservice.util.transaction.TransactionOrigin;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringExecutionServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PIGGY_BANK_ID = 10L;
    private static final BigDecimal REVENUE_AMOUNT = BigDecimal.valueOf(500);
    private static final BigDecimal EXPENSE_AMOUNT = BigDecimal.valueOf(200);
    private static final BigDecimal SAVINGS_AMOUNT = BigDecimal.valueOf(300);
    private static final List<Limit> LIMITS = List.of();

    @Mock
    private SettingsManager settingsManager;

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
    private WalletManagerService walletManagerService;

    @Mock
    private LimitManagerService limitManagerService;

    @Mock
    private RecurringSettings recurringSettings;

    @Mock
    private ExpenseSettings expenseSettings;

    @Mock
    private Wallet wallet;

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
            verifyNoInteractions(settingsManager, revenueService, expenseService, piggyBankTransactionService, expenseSettingsValidator, recurringRevenueValidator, recurringSavingsValidator, walletManagerService, limitManagerService);
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
            when(recurringSettings.getUserId()).thenReturn(USER_ID);
            when(recurringSettings.getRevenueCategory()).thenReturn(null);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verifyNoInteractions(recurringRevenueValidator, revenueService);
        }

        @Nested
        class WithValidRevenueSettings {

            @BeforeEach
            void setUp() {
                when(recurringSettings.getUserId()).thenReturn(USER_ID);
                when(recurringSettings.getRevenueCategory()).thenReturn(RevenueCategory.SALARY);
            }

            @Test
            void shouldReturnExecutedWhenRevenueIsAddedSuccessfully() {
                when(recurringSettings.getAmount()).thenReturn(REVENUE_AMOUNT);

                RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

                assertEquals(RecurringExecutionResult.EXECUTED, result);
                verify(recurringRevenueValidator).validate(recurringSettings);
                verify(revenueService).addRevenue(any(RevenueDto.class), eq(USER_ID), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));
            }

            @Test
            void shouldValidateSettingsBeforeAddingRevenue() {
                when(recurringSettings.getAmount()).thenReturn(REVENUE_AMOUNT);

                recurringExecutionService.execute(recurringSettings, executionDate);

                InOrder order = inOrder(recurringRevenueValidator, revenueService);

                order.verify(recurringRevenueValidator).validate(recurringSettings);
                order.verify(revenueService).addRevenue(any(RevenueDto.class), eq(USER_ID), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));
            }

            @Test
            void shouldBuildRevenueDtoWithCorrectFieldsWhenSettingsAreValid() {
                when(recurringSettings.getAmount()).thenReturn(REVENUE_AMOUNT);

                recurringExecutionService.execute(recurringSettings, executionDate);

                ArgumentCaptor<RevenueDto> captor = ArgumentCaptor.forClass(RevenueDto.class);

                verify(revenueService).addRevenue(captor.capture(), eq(USER_ID), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));

                RevenueDto revenueDto = captor.getValue();

                assertNull(revenueDto.id());
                assertEquals(USER_ID, revenueDto.userId());
                assertEquals(REVENUE_AMOUNT, revenueDto.amount());
                assertEquals(RevenueCategory.SALARY, revenueDto.category());
                assertEquals(executionDate, revenueDto.createdAt());
                assertNull(revenueDto.authorizationCode());
                assertNull(revenueDto.riskVerificationSourceEventId());
            }

            @Test
            void shouldBuildRevenueDtoWithNullAmountWhenAmountIsNull() {
                when(recurringSettings.getAmount()).thenReturn(null);

                recurringExecutionService.execute(recurringSettings, executionDate);

                ArgumentCaptor<RevenueDto> captor = ArgumentCaptor.forClass(RevenueDto.class);

                verify(revenueService).addRevenue(captor.capture(), eq(USER_ID), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));

                assertNull(captor.getValue().amount());
            }

            @Test
            void shouldReturnSkippedWhenValidatorThrowsInvalidInputException() {
                doThrow(new InvalidInputException("invalid settings")).when(recurringRevenueValidator).validate(recurringSettings);

                RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

                assertEquals(RecurringExecutionResult.SKIPPED, result);
                verify(recurringRevenueValidator).validate(recurringSettings);
                verifyNoInteractions(revenueService);
            }

            @Test
            void shouldReturnSkippedWhenRevenueServiceThrowsInvalidInputException() {
                when(recurringSettings.getAmount()).thenReturn(REVENUE_AMOUNT);

                doThrow(new InvalidInputException("invalid revenue")).when(revenueService).addRevenue(any(RevenueDto.class), eq(USER_ID), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));

                RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

                assertEquals(RecurringExecutionResult.SKIPPED, result);
            }

            @Test
            void shouldThrowExceptionWhenValidatorThrowsUnexpectedException() {
                doThrow(new IllegalStateException("unexpected")).when(recurringRevenueValidator).validate(recurringSettings);

                assertThrows(IllegalStateException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));

                verifyNoInteractions(revenueService);
            }

            @Test
            void shouldThrowExceptionWhenRevenueServiceThrowsUnexpectedException() {
                when(recurringSettings.getAmount()).thenReturn(REVENUE_AMOUNT);

                doThrow(new IllegalStateException("unexpected")).when(revenueService).addRevenue(any(RevenueDto.class), eq(USER_ID), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));

                assertThrows(IllegalStateException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));
            }
        }
    }

    @Nested
    class ExecuteExpense {

        @BeforeEach
        void setUp() {
            when(recurringSettings.getType()).thenReturn(RecurringType.EXPENSE);
        }

        @Test
        void shouldReturnExecutedWhenUserIdIsNull() {
            when(recurringSettings.getUserId()).thenReturn(null);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verifyNoInteractions(settingsManager, walletManagerService, limitManagerService, expenseSettingsValidator, expenseService);
        }

        @Test
        void shouldReturnExecutedWhenExpenseCategoryIsNull() {
            when(recurringSettings.getUserId()).thenReturn(USER_ID);
            when(recurringSettings.getExpenseCategory()).thenReturn(null);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verifyNoInteractions(settingsManager, walletManagerService, limitManagerService, expenseSettingsValidator, expenseService);
        }

        @Test
        void shouldReturnExecutedWhenExpenseSettingsDoNotExist() {
            when(recurringSettings.getUserId()).thenReturn(USER_ID);
            when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
            when(settingsManager.getExpenseSettingsByUserId(USER_ID)).thenReturn(null);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verify(settingsManager).getExpenseSettingsByUserId(USER_ID);
            verifyNoInteractions(walletManagerService, limitManagerService, expenseSettingsValidator, expenseService);
        }

        @Nested
        class WithExpenseSettings {

            @BeforeEach
            void setUp() {
                when(recurringSettings.getUserId()).thenReturn(USER_ID);
                when(recurringSettings.getExpenseCategory()).thenReturn(ExpenseCategory.FOOD);
                when(settingsManager.getExpenseSettingsByUserId(USER_ID)).thenReturn(expenseSettings);
            }

            @Test
            void shouldThrowExceptionWhenWalletIsNotFound() {
                doThrow(new RequestedEntityNotFoundException("wallet not found")).when(walletManagerService).getWalletByUserIdOrThrow(USER_ID);

                assertThrows(RequestedEntityNotFoundException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));

                verifyNoInteractions(limitManagerService, expenseSettingsValidator, expenseService);
            }

            @Test
            void shouldThrowExceptionWhenLimitManagerThrowsUnexpectedException() {
                when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);

                doThrow(new IllegalStateException("unexpected")).when(limitManagerService).getLimitsByUserId(USER_ID);

                assertThrows(IllegalStateException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));

                verifyNoInteractions(expenseSettingsValidator, expenseService);
            }

            @Nested
            class WithWalletAndLimits {

                @BeforeEach
                void setUp() {
                    when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
                    when(limitManagerService.getLimitsByUserId(USER_ID)).thenReturn(LIMITS);
                }

                @Test
                void shouldReturnExecutedAndResetSkippedNotificationWhenExpenseIsAddedSuccessfully() {
                    RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

                    assertEquals(RecurringExecutionResult.EXECUTED, result);
                    verify(expenseSettingsValidator).validate(recurringSettings, expenseSettings, wallet, LIMITS);
                    verify(expenseService).addExpense(any(ExpenseRequestDto.class), eq(USER_ID), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));
                    verify(recurringSettings).setSkippedNotificationSent(false);
                }

                @Test
                void shouldLoadWalletAndLimitsBeforeValidation() {
                    recurringExecutionService.execute(recurringSettings, executionDate);

                    InOrder order = inOrder(walletManagerService, limitManagerService, expenseSettingsValidator);

                    order.verify(walletManagerService).getWalletByUserIdOrThrow(USER_ID);
                    order.verify(limitManagerService).getLimitsByUserId(USER_ID);
                    order.verify(expenseSettingsValidator).validate(recurringSettings, expenseSettings, wallet, LIMITS);
                }

                @Test
                void shouldValidateBeforeAddingExpense() {
                    recurringExecutionService.execute(recurringSettings, executionDate);

                    InOrder order = inOrder(expenseSettingsValidator, expenseService);

                    order.verify(expenseSettingsValidator).validate(recurringSettings, expenseSettings, wallet, LIMITS);
                    order.verify(expenseService).addExpense(any(ExpenseRequestDto.class), eq(USER_ID), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));
                }

                @Test
                void shouldBuildExpenseRequestWithCorrectExpenseDtoWhenSettingsAreValid() {
                    when(recurringSettings.getAmount()).thenReturn(EXPENSE_AMOUNT);

                    recurringExecutionService.execute(recurringSettings, executionDate);

                    ArgumentCaptor<ExpenseRequestDto> captor = ArgumentCaptor.forClass(ExpenseRequestDto.class);

                    verify(expenseService).addExpense(captor.capture(), eq(USER_ID), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));

                    ExpenseDto expenseDto = captor.getValue().expenseDto();

                    assertNull(expenseDto.id());
                    assertEquals(USER_ID, expenseDto.userId());
                    assertEquals(EXPENSE_AMOUNT, expenseDto.amount());
                    assertEquals(ExpenseCategory.FOOD, expenseDto.category());
                    assertEquals(executionDate, expenseDto.createdAt());
                }

                @Test
                void shouldBuildExpenseRequestWithNullAuthorizationData() {
                    recurringExecutionService.execute(recurringSettings, executionDate);

                    ArgumentCaptor<ExpenseRequestDto> captor = ArgumentCaptor.forClass(ExpenseRequestDto.class);

                    verify(expenseService).addExpense(captor.capture(), eq(USER_ID), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));

                    ExpenseRequestDto requestDto = captor.getValue();

                    assertNull(requestDto.confirmPasswordDto().password());
                    assertNull(requestDto.confirmAuthorizationCodeDto().code());
                    assertNull(requestDto.riskVerificationSourceEventId());
                }

                @Test
                void shouldUseExpenseSettingsPeriodTypeWhenExpenseSettingsPeriodTypeIsNotNull() {
                    when(expenseSettings.getPeriodType()).thenReturn(PeriodType.MONTHLY);

                    recurringExecutionService.execute(recurringSettings, executionDate);

                    ArgumentCaptor<ExpenseRequestDto> captor = ArgumentCaptor.forClass(ExpenseRequestDto.class);

                    verify(expenseService).addExpense(captor.capture(), eq(USER_ID), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));

                    assertEquals(PeriodType.MONTHLY, captor.getValue().countQuantityLimitDto().periodType());
                    verify(recurringSettings, never()).getPeriodType();
                }

                @Test
                void shouldUseRecurringSettingsPeriodTypeWhenExpenseSettingsPeriodTypeIsNull() {
                    when(expenseSettings.getPeriodType()).thenReturn(null);
                    when(recurringSettings.getPeriodType()).thenReturn(PeriodType.MONTHLY);

                    recurringExecutionService.execute(recurringSettings, executionDate);

                    ArgumentCaptor<ExpenseRequestDto> captor = ArgumentCaptor.forClass(ExpenseRequestDto.class);

                    verify(expenseService).addExpense(captor.capture(), eq(USER_ID), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));

                    assertEquals(PeriodType.MONTHLY, captor.getValue().countQuantityLimitDto().periodType());
                }

                @Test
                void shouldUseNullPeriodTypeWhenBothPeriodTypesAreNull() {
                    when(expenseSettings.getPeriodType()).thenReturn(null);
                    when(recurringSettings.getPeriodType()).thenReturn(null);

                    recurringExecutionService.execute(recurringSettings, executionDate);

                    ArgumentCaptor<ExpenseRequestDto> captor = ArgumentCaptor.forClass(ExpenseRequestDto.class);

                    verify(expenseService).addExpense(captor.capture(), eq(USER_ID), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));

                    assertNull(captor.getValue().countQuantityLimitDto().periodType());
                }

                @Test
                void shouldReturnSkippedWhenValidatorThrowsInvalidInputException() {
                    doThrow(new InvalidInputException("invalid settings")).when(expenseSettingsValidator).validate(recurringSettings, expenseSettings, wallet, LIMITS);

                    RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

                    assertEquals(RecurringExecutionResult.SKIPPED, result);
                    verifyNoInteractions(expenseService);
                    verify(recurringSettings, never()).setSkippedNotificationSent(false);
                }

                @Test
                void shouldReturnSkippedWhenExpenseServiceThrowsInvalidInputException() {
                    doThrow(new InvalidInputException("invalid expense")).when(expenseService).addExpense(any(ExpenseRequestDto.class), eq(USER_ID), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));

                    RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

                    assertEquals(RecurringExecutionResult.SKIPPED, result);
                    verify(recurringSettings, never()).setSkippedNotificationSent(false);
                }

                @Test
                void shouldThrowExceptionWhenValidatorThrowsUnexpectedException() {
                    doThrow(new IllegalStateException("unexpected")).when(expenseSettingsValidator).validate(recurringSettings, expenseSettings, wallet, LIMITS);

                    assertThrows(IllegalStateException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));

                    verifyNoInteractions(expenseService);
                    verify(recurringSettings, never()).setSkippedNotificationSent(false);
                }

                @Test
                void shouldThrowExceptionWhenExpenseServiceThrowsUnexpectedException() {
                    doThrow(new IllegalStateException("unexpected")).when(expenseService).addExpense(any(ExpenseRequestDto.class), eq(USER_ID), isNull(), eq(TransactionOrigin.RECURRING_SYSTEM));

                    assertThrows(IllegalStateException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));

                    verify(recurringSettings, never()).setSkippedNotificationSent(false);
                }
            }
        }
    }

    @Nested
    class ExecuteSavings {

        @BeforeEach
        void setUp() {
            when(recurringSettings.getType()).thenReturn(RecurringType.SAVINGS);
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
            when(recurringSettings.getUserId()).thenReturn(USER_ID);
            when(recurringSettings.getPiggyBankId()).thenReturn(null);

            RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

            assertEquals(RecurringExecutionResult.EXECUTED, result);
            verifyNoInteractions(walletManagerService, recurringSavingsValidator, piggyBankTransactionService);
        }

        @Nested
        class WithPiggyBank {

            @BeforeEach
            void setUp() {
                when(recurringSettings.getUserId()).thenReturn(USER_ID);
                when(recurringSettings.getPiggyBankId()).thenReturn(PIGGY_BANK_ID);
            }

            @Test
            void shouldThrowExceptionWhenWalletIsNotFound() {
                doThrow(new RequestedEntityNotFoundException("wallet not found")).when(walletManagerService).getWalletByUserIdOrThrow(USER_ID);

                assertThrows(RequestedEntityNotFoundException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));

                verifyNoInteractions(recurringSavingsValidator, piggyBankTransactionService);
                verify(recurringSettings, never()).setEnable(false);
                verify(recurringSettings, never()).setNextExecutionDate(null);
            }

            @Nested
            class WithWallet {

                @BeforeEach
                void setUp() {
                    when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
                }

                @Test
                void shouldReturnExecutedAndResetSkippedNotificationWhenSavingsAreAddedSuccessfully() {
                    when(recurringSettings.getAmount()).thenReturn(SAVINGS_AMOUNT);

                    RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

                    assertEquals(RecurringExecutionResult.EXECUTED, result);
                    verify(recurringSavingsValidator).validate(recurringSettings, wallet);
                    verify(piggyBankTransactionService).addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, SAVINGS_AMOUNT, PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, null, null, null, TransactionOrigin.RECURRING_SYSTEM);
                    verify(recurringSettings).setSkippedNotificationSent(false);
                }

                @Test
                void shouldValidateSavingsBeforeAddingBalance() {
                    when(recurringSettings.getAmount()).thenReturn(SAVINGS_AMOUNT);

                    recurringExecutionService.execute(recurringSettings, executionDate);

                    InOrder order = inOrder(recurringSavingsValidator, piggyBankTransactionService);

                    order.verify(recurringSavingsValidator).validate(recurringSettings, wallet);
                    order.verify(piggyBankTransactionService).addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, SAVINGS_AMOUNT, PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, null, null, null, TransactionOrigin.RECURRING_SYSTEM);
                }

                @Test
                void shouldPassNullAmountWhenAmountIsNull() {
                    when(recurringSettings.getAmount()).thenReturn(null);

                    recurringExecutionService.execute(recurringSettings, executionDate);

                    verify(piggyBankTransactionService).addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, null, PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, null, null, null, TransactionOrigin.RECURRING_SYSTEM);
                }

                @Test
                void shouldNotDisableRecurringSettingsWhenSavingsAreAddedSuccessfully() {
                    when(recurringSettings.getAmount()).thenReturn(SAVINGS_AMOUNT);

                    recurringExecutionService.execute(recurringSettings, executionDate);

                    verify(recurringSettings, never()).setEnable(false);
                    verify(recurringSettings, never()).setNextExecutionDate(null);
                }

                @Test
                void shouldReturnSkippedWhenValidatorThrowsInvalidInputException() {
                    doThrow(new InvalidInputException("invalid settings")).when(recurringSavingsValidator).validate(recurringSettings, wallet);

                    RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

                    assertEquals(RecurringExecutionResult.SKIPPED, result);
                    verifyNoInteractions(piggyBankTransactionService);
                    verify(recurringSettings, never()).setSkippedNotificationSent(false);
                    verify(recurringSettings, never()).setEnable(false);
                }

                @Test
                void shouldReturnSkippedWhenPiggyBankTransactionThrowsInvalidInputException() {
                    when(recurringSettings.getAmount()).thenReturn(SAVINGS_AMOUNT);

                    doThrow(new InvalidInputException("invalid transaction")).when(piggyBankTransactionService).addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, SAVINGS_AMOUNT, PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, null, null, null, TransactionOrigin.RECURRING_SYSTEM);

                    RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

                    assertEquals(RecurringExecutionResult.SKIPPED, result);
                    verify(recurringSettings, never()).setSkippedNotificationSent(false);
                    verify(recurringSettings, never()).setEnable(false);
                }

                @Test
                void shouldDisableRecurringSettingsWhenPiggyBankIsNotFoundDuringTransaction() {
                    when(recurringSettings.getAmount()).thenReturn(SAVINGS_AMOUNT);

                    doThrow(new RequestedEntityNotFoundException("piggy bank not found")).when(piggyBankTransactionService).addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, SAVINGS_AMOUNT, PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, null, null, null, TransactionOrigin.RECURRING_SYSTEM);

                    RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

                    assertEquals(RecurringExecutionResult.EXECUTED, result);
                    verify(recurringSettings).setEnable(false);
                    verify(recurringSettings).setNextExecutionDate(null);
                    verify(recurringSettings, never()).setSkippedNotificationSent(false);
                }

                @Test
                void shouldDisableRecurringSettingsWhenPiggyBankIsNotFoundDuringValidation() {
                    doThrow(new RequestedEntityNotFoundException("piggy bank not found")).when(recurringSavingsValidator).validate(recurringSettings, wallet);

                    RecurringExecutionResult result = recurringExecutionService.execute(recurringSettings, executionDate);

                    assertEquals(RecurringExecutionResult.EXECUTED, result);
                    verify(recurringSettings).setEnable(false);
                    verify(recurringSettings).setNextExecutionDate(null);
                    verifyNoInteractions(piggyBankTransactionService);
                    verify(recurringSettings, never()).setSkippedNotificationSent(false);
                }

                @Test
                void shouldThrowExceptionWhenValidatorThrowsUnexpectedException() {
                    doThrow(new IllegalStateException("unexpected")).when(recurringSavingsValidator).validate(recurringSettings, wallet);

                    assertThrows(IllegalStateException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));

                    verifyNoInteractions(piggyBankTransactionService);
                    verify(recurringSettings, never()).setSkippedNotificationSent(false);
                    verify(recurringSettings, never()).setEnable(false);
                }

                @Test
                void shouldThrowExceptionWhenPiggyBankTransactionThrowsUnexpectedException() {
                    when(recurringSettings.getAmount()).thenReturn(SAVINGS_AMOUNT);

                    doThrow(new IllegalStateException("unexpected")).when(piggyBankTransactionService).addBalanceToPiggyBank(USER_ID, PIGGY_BANK_ID, SAVINGS_AMOUNT, PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, null, null, null, TransactionOrigin.RECURRING_SYSTEM);

                    assertThrows(IllegalStateException.class, () -> recurringExecutionService.execute(recurringSettings, executionDate));

                    verify(recurringSettings, never()).setSkippedNotificationSent(false);
                    verify(recurringSettings, never()).setEnable(false);
                }
            }
        }
    }
}