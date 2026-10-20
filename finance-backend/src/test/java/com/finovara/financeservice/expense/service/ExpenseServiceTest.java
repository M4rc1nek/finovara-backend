package com.finovara.financeservice.expense.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.contracts.mainaccount.activity.event.expense.ExpenseActivityEvent;
import com.finovara.contracts.mainaccount.transaction.event.limit.LimitStatsEvent;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.securitymonitoring.model.RiskTriggerType;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.user.authorization.dto.ConfirmAuthorizationCodeDto;
import com.finovara.contracts.user.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.financeservice.expense.dto.ExpenseDto;
import com.finovara.financeservice.expense.dto.ExpenseRequestDto;
import com.finovara.financeservice.expense.mapper.ExpenseMapper;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.limit.dto.LimitStatsDto;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.limit.repository.LimitRepository;
import com.finovara.financeservice.limit.service.LimitCalculateService;
import com.finovara.financeservice.riskverification.service.RiskGuardService;
import com.finovara.financeservice.settings.finances.expense.controlamount.service.ControlAmountService;
import com.finovara.financeservice.settings.finances.expense.quantitylimit.dto.CountQuantityLimitDto;
import com.finovara.financeservice.settings.finances.expense.quantitylimit.service.CountQuantityLimitService;
import com.finovara.financeservice.settings.finances.expense.smartscan.dto.SmartScanMode;
import com.finovara.financeservice.settings.finances.expense.smartscan.service.SmartScanService;
import com.finovara.financeservice.settings.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.financeservice.settings.piggybank.roundup.service.RoundUpService;
import com.finovara.financeservice.util.periodbalance.FinancialPeriodService;
import com.finovara.financeservice.util.transaction.TransactionOrigin;
import com.finovara.financeservice.util.transaction.expense.ExpenseManagerService;
import com.finovara.financeservice.wallet.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long EXPENSE_ID = 3L;
    private static final Long SAVED_EXPENSE_ID = 7L;
    private static final String USER_EMAIL = "user@test.com";
    private static final String RISK_SOURCE_EVENT_ID = "risk-source-event-id";
    private static final String DESCRIPTION = "description";
    private static final String OLD_DESCRIPTION = "old description";
    private static final String AUTHORIZATION_CODE = "authCode";
    private static final String LIMIT_STATS_TOPIC = "limit.calculate-stats";
    private static final String OUTBOX_AGGREGATE = "Expense";
    private static final String OUTBOX_EVENT = "expense.created";
    private static final ExpenseCategory CATEGORY = ExpenseCategory.values()[0];
    private static final ExpenseCategory OTHER_CATEGORY = ExpenseCategory.values()[1];
    private static final PeriodType PERIOD_TYPE = PeriodType.MONTHLY;
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(50);
    private static final BigDecimal OLD_AMOUNT = BigDecimal.valueOf(100);
    private static final BigDecimal EDITED_AMOUNT = BigDecimal.valueOf(250);
    private static final BigDecimal DELETED_AMOUNT = BigDecimal.valueOf(80);

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private OutboxService outboxService;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private LimitRepository limitRepository;

    @Mock
    private LimitCalculateService limitCalculateService;

    @Mock
    private WalletService walletService;

    @Mock
    private RoundUpService roundUpService;

    @Mock
    private CountQuantityLimitService countQuantityLimitService;

    @Mock
    private ControlAmountService controlAmountService;

    @Mock
    private SmartScanService smartScanService;

    @Mock
    private ExpenseManagerService expenseManagerService;

    @Mock
    private ExpenseMapper expenseMapper;

    @Mock
    private FinancialPeriodService financialPeriodService;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Mock
    private RiskGuardService riskGuardService;

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private ConfirmPasswordDto confirmPasswordDto;

    @Mock
    private ConfirmAuthorizationCodeDto confirmAuthorizationCodeDto;

    @Mock
    private CountQuantityLimitDto countQuantityLimitDto;

    @Mock
    private LimitStatsDto limitStatsDto;

    @InjectMocks
    private ExpenseService expenseService;

    private ExpenseRequestDto buildRequestDto(BigDecimal amount, ExpenseCategory category) {
        ExpenseDto expenseDto = new ExpenseDto(null, USER_ID, amount, category, LocalDate.now(), DESCRIPTION);

        return new ExpenseRequestDto(expenseDto, confirmPasswordDto, confirmAuthorizationCodeDto, countQuantityLimitDto, RISK_SOURCE_EVENT_ID);
    }

    private Limit buildLimit(ExpenseCategory category, BigDecimal amount) {
        return Limit.builder()
                .periodType(PERIOD_TYPE)
                .category(category)
                .amount(amount)
                .build();
    }

    private Expense buildExpense(Long id, BigDecimal amount, ExpenseCategory category) {
        return Expense.builder()
                .id(id)
                .amount(amount)
                .category(category)
                .createdAt(LocalDate.now())
                .description(OLD_DESCRIPTION)
                .userId(USER_ID)
                .build();
    }

    @Nested
    class AddExpense {

        private ExpenseRequestDto requestDto;

        @BeforeEach
        void setUp() {
            requestDto = buildRequestDto(AMOUNT, CATEGORY);
        }

        @Nested
        class WhenRequestIsRejected {

            @Test
            void shouldThrowExceptionWhenAmountIsLessThanOne() {
                ExpenseRequestDto invalidRequestDto = buildRequestDto(BigDecimal.valueOf(0.5), CATEGORY);

                assertThrows(InvalidInputException.class, () -> expenseService.addExpense(invalidRequestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM));

                verifyNoInteractions(walletService, expenseRepository, smartScanService, outboxService, roundUpService, controlAmountService, kafkaTemplate);
            }

            @Test
            void shouldThrowExceptionWhenAmountIsJustBelowOne() {
                ExpenseRequestDto invalidRequestDto = buildRequestDto(BigDecimal.valueOf(0.99), CATEGORY);

                assertThrows(InvalidInputException.class, () -> expenseService.addExpense(invalidRequestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM));

                verifyNoInteractions(walletService, expenseRepository);
            }

            @Test
            void shouldThrowExceptionWhenAmountIsZero() {
                ExpenseRequestDto invalidRequestDto = buildRequestDto(BigDecimal.ZERO, CATEGORY);

                assertThrows(InvalidInputException.class, () -> expenseService.addExpense(invalidRequestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM));

                verifyNoInteractions(walletService, expenseRepository);
            }

            @Test
            void shouldThrowExceptionWhenAmountIsNegative() {
                ExpenseRequestDto invalidRequestDto = buildRequestDto(BigDecimal.valueOf(-10), CATEGORY);

                assertThrows(InvalidInputException.class, () -> expenseService.addExpense(invalidRequestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM));

                verifyNoInteractions(walletService, expenseRepository);
            }

            @Test
            void shouldThrowExceptionWhenGeneralLimitIsExceeded() {
                ExpenseRequestDto exceedingRequestDto = buildRequestDto(BigDecimal.valueOf(500), CATEGORY);
                Limit generalLimit = buildLimit(null, BigDecimal.valueOf(400));

                when(limitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(generalLimit));
                when(financialPeriodService.getExpensesSum(USER_ID, PERIOD_TYPE, null)).thenReturn(BigDecimal.ZERO);

                assertThrows(MissingRequirementException.class, () -> expenseService.addExpense(exceedingRequestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM));

                verifyNoInteractions(countQuantityLimitService, walletService, expenseRepository, smartScanService);
            }

            @Test
            void shouldThrowExceptionWhenCategoryLimitIsExceeded() {
                ExpenseRequestDto exceedingRequestDto = buildRequestDto(BigDecimal.valueOf(500), CATEGORY);
                Limit categoryLimit = buildLimit(CATEGORY, BigDecimal.valueOf(400));

                when(limitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(categoryLimit));
                when(financialPeriodService.getExpensesSum(USER_ID, PERIOD_TYPE, CATEGORY)).thenReturn(BigDecimal.ZERO);

                assertThrows(MissingRequirementException.class, () -> expenseService.addExpense(exceedingRequestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM));

                verifyNoInteractions(countQuantityLimitService, walletService, expenseRepository, smartScanService);
            }

            @Test
            void shouldThrowExceptionWhenAlreadySpentAmountPlusNewAmountExceedsLimit() {
                ExpenseRequestDto exceedingRequestDto = buildRequestDto(BigDecimal.valueOf(700), CATEGORY);
                Limit generalLimit = buildLimit(null, BigDecimal.valueOf(1000));

                when(limitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(generalLimit));
                when(financialPeriodService.getExpensesSum(USER_ID, PERIOD_TYPE, null)).thenReturn(BigDecimal.valueOf(400));

                assertThrows(MissingRequirementException.class, () -> expenseService.addExpense(exceedingRequestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM));

                verifyNoInteractions(walletService, expenseRepository);
            }

            @Test
            void shouldThrowExceptionWhenSecondLimitIsExceeded() {
                Limit firstLimit = buildLimit(null, BigDecimal.valueOf(1000));
                Limit secondLimit = buildLimit(CATEGORY, BigDecimal.valueOf(60));

                when(limitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(firstLimit, secondLimit));
                when(financialPeriodService.getExpensesSum(USER_ID, PERIOD_TYPE, null)).thenReturn(BigDecimal.ZERO);
                when(financialPeriodService.getExpensesSum(USER_ID, PERIOD_TYPE, CATEGORY)).thenReturn(BigDecimal.valueOf(30));

                assertThrows(MissingRequirementException.class, () -> expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM));

                verifyNoInteractions(walletService, expenseRepository);
            }

            @Test
            void shouldThrowExceptionWhenRiskGuardFails() {
                when(authBackendClient.getUserEmail(USER_ID)).thenReturn(USER_EMAIL);
                doThrow(new IllegalStateException("risk detected")).when(riskGuardService).guard(USER_ID, RiskTriggerType.EXPENSE, AMOUNT, CATEGORY.name(), USER_EMAIL, RISK_SOURCE_EVENT_ID, servletRequest);

                assertThrows(IllegalStateException.class, () -> expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));

                verifyNoInteractions(additionalAuthorizationCodeResolver, limitRepository, walletService, expenseRepository, outboxService);
                verify(authBackendClient, never()).confirmAuthorizationCode(eq(USER_ID), any());
            }

            @Test
            void shouldThrowExceptionWhenAuthorizationCodeConfirmationFails() {
                doThrow(new IllegalStateException("invalid code")).when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

                assertThrows(IllegalStateException.class, () -> expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));

                verifyNoInteractions(limitRepository, walletService, expenseRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenCountQuantityLimitServiceFails() {
                doThrow(new IllegalStateException("quantity limit")).when(countQuantityLimitService).handleExpenseLimitExceeded(USER_ID, countQuantityLimitDto, null, confirmPasswordDto);

                assertThrows(IllegalStateException.class, () -> expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM));

                verifyNoInteractions(smartScanService, walletService, expenseRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenSmartScanServiceFails() {
                doThrow(new IllegalStateException("smart scan")).when(smartScanService).handleSmartScan(USER_ID, confirmPasswordDto, AMOUNT, SmartScanMode.ADD);

                assertThrows(IllegalStateException.class, () -> expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM));

                verifyNoInteractions(walletService, expenseRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenWalletBalanceRemovalFails() {
                doThrow(new IllegalStateException("insufficient funds")).when(walletService).removeBalanceFromWallet(USER_ID, AMOUNT);

                assertThrows(IllegalStateException.class, () -> expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM));

                verifyNoInteractions(expenseRepository, outboxService, roundUpService, controlAmountService, kafkaTemplate);
            }
        }

        @Nested
        class WhenExpenseIsSaved {

            @BeforeEach
            void setUp() {
                when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
                    Expense expense = invocation.getArgument(0);
                    expense.setId(SAVED_EXPENSE_ID);
                    return expense;
                });
            }

            @Test
            void shouldReturnSavedExpenseIdWhenRequestIsValid() {
                Long result = expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL);

                assertEquals(SAVED_EXPENSE_ID, result);
            }

            @Test
            void shouldAddExpenseWhenAmountEqualsOne() {
                ExpenseRequestDto boundaryRequestDto = buildRequestDto(BigDecimal.ONE, CATEGORY);

                Long result = expenseService.addExpense(boundaryRequestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM);

                assertEquals(SAVED_EXPENSE_ID, result);
                verify(walletService).removeBalanceFromWallet(USER_ID, BigDecimal.ONE);
            }

            @Test
            void shouldSaveExpenseWithCorrectFieldsWhenAddingExpense() {
                expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM);

                ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);

                verify(expenseRepository).save(captor.capture());

                Expense savedExpense = captor.getValue();

                assertEquals(AMOUNT, savedExpense.getAmount());
                assertEquals(CATEGORY, savedExpense.getCategory());
                assertEquals(DESCRIPTION, savedExpense.getDescription());
                assertEquals(USER_ID, savedExpense.getUserId());
                assertEquals(LocalDate.now(), savedExpense.getCreatedAt());
            }

            @Test
            void shouldRemoveBalanceFromWalletWhenAddingExpense() {
                expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM);

                verify(walletService).removeBalanceFromWallet(USER_ID, AMOUNT);
            }

            @Test
            void shouldPassPeriodTypeFromCountQuantityLimitDtoWhenHandlingQuantityLimit() {
                when(countQuantityLimitDto.periodType()).thenReturn(PERIOD_TYPE);

                expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM);

                verify(countQuantityLimitService).handleExpenseLimitExceeded(USER_ID, countQuantityLimitDto, PERIOD_TYPE, confirmPasswordDto);
            }

            @Test
            void shouldCallSmartScanServiceWithAddModeWhenAddingExpense() {
                expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM);

                verify(smartScanService).handleSmartScan(USER_ID, confirmPasswordDto, AMOUNT, SmartScanMode.ADD);
            }

            @Test
            void shouldHandleRoundUpWithApplyModeWhenAddingExpense() {
                expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM);

                verify(roundUpService).handleExpenseForRoundUp(USER_ID, SAVED_EXPENSE_ID, PiggyBankAutomationMode.APPLY);
            }

            @Test
            void shouldCallControlAmountServiceWhenAddingExpense() {
                expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM);

                verify(controlAmountService).handleExpenseAmountControl(USER_ID, AMOUNT);
            }

            @Test
            void shouldSaveOutboxEventWhenAddingExpense() {
                expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM);

                verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(SAVED_EXPENSE_ID.toString()), eq(OUTBOX_EVENT), any(ExpenseActivityEvent.class));
            }

            @Test
            void shouldSaveExpenseAfterWalletBalanceIsRemoved() {
                expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM);

                InOrder order = inOrder(walletService, expenseRepository, outboxService);

                order.verify(walletService).removeBalanceFromWallet(USER_ID, AMOUNT);
                order.verify(expenseRepository).save(any(Expense.class));
                order.verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(SAVED_EXPENSE_ID.toString()), eq(OUTBOX_EVENT), any(ExpenseActivityEvent.class));
            }

            @Test
            void shouldConfirmAuthorizationCodeWhenOriginIsUserManual() {
                expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL);

                verify(additionalAuthorizationCodeResolver).resolve(confirmAuthorizationCodeDto);
                verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
            }

            @Test
            void shouldGuardAgainstRiskWhenOriginIsUserManual() {
                when(authBackendClient.getUserEmail(USER_ID)).thenReturn(USER_EMAIL);

                expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL);

                verify(riskGuardService).guard(USER_ID, RiskTriggerType.EXPENSE, AMOUNT, CATEGORY.name(), USER_EMAIL, RISK_SOURCE_EVENT_ID, servletRequest);
            }

            @Test
            void shouldGuardAgainstRiskBeforeConfirmingAuthorizationCodeWhenOriginIsUserManual() {
                expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL);

                InOrder order = inOrder(riskGuardService, authBackendClient);

                order.verify(riskGuardService).guard(USER_ID, RiskTriggerType.EXPENSE, AMOUNT, CATEGORY.name(), null, RISK_SOURCE_EVENT_ID, servletRequest);
                order.verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
            }

            @Test
            void shouldSkipRiskGuardAndAuthorizationWhenOriginIsRecurringSystem() {
                expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM);

                verifyNoInteractions(riskGuardService, authBackendClient, additionalAuthorizationCodeResolver);
            }

            @Test
            void shouldNotPublishLimitStatsEventsWhenNoLimitsExist() {
                expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM);

                verifyNoInteractions(kafkaTemplate, limitCalculateService, financialPeriodService);
            }

            @Test
            void shouldLoadLimitsForValidationAndForStatsPublishing() {
                expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM);

                verify(limitRepository, times(2)).findAllByUserId(USER_ID);
            }

            @Test
            void shouldPublishLimitStatsEventForEachLimitWhenAddingExpense() {
                Limit generalLimit = buildLimit(null, BigDecimal.valueOf(1000));
                Limit categoryLimit = buildLimit(CATEGORY, BigDecimal.valueOf(2000));

                when(limitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(generalLimit, categoryLimit));
                when(financialPeriodService.getExpensesSum(USER_ID, PERIOD_TYPE, null)).thenReturn(BigDecimal.ZERO);
                when(financialPeriodService.getExpensesSum(USER_ID, PERIOD_TYPE, CATEGORY)).thenReturn(BigDecimal.ZERO);
                when(limitCalculateService.calculateLimitStats(any(Limit.class), eq(USER_ID), any(LocalDate.class))).thenReturn(limitStatsDto);

                expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM);

                verify(kafkaTemplate, times(2)).send(eq(LIMIT_STATS_TOPIC), any(LimitStatsEvent.class));
            }

            @Test
            void shouldAddExpenseWhenTotalEqualsLimitAmount() {
                Limit generalLimit = buildLimit(null, BigDecimal.valueOf(100));

                when(limitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(generalLimit));
                when(financialPeriodService.getExpensesSum(USER_ID, PERIOD_TYPE, null)).thenReturn(AMOUNT);
                when(limitCalculateService.calculateLimitStats(eq(generalLimit), eq(USER_ID), any(LocalDate.class))).thenReturn(limitStatsDto);

                Long result = expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM);

                assertEquals(SAVED_EXPENSE_ID, result);
                verify(kafkaTemplate).send(eq(LIMIT_STATS_TOPIC), any(LimitStatsEvent.class));
            }

            @Test
            void shouldSkipLimitValidationWhenLimitCategoryDiffersFromExpenseCategory() {
                Limit otherCategoryLimit = buildLimit(OTHER_CATEGORY, BigDecimal.ONE);

                when(limitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(otherCategoryLimit));
                when(limitCalculateService.calculateLimitStats(eq(otherCategoryLimit), eq(USER_ID), any(LocalDate.class))).thenReturn(limitStatsDto);

                Long result = expenseService.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.RECURRING_SYSTEM);

                assertEquals(SAVED_EXPENSE_ID, result);
                verifyNoInteractions(financialPeriodService);
            }
        }
    }

    @Nested
    class EditExpense {

        @Test
        void shouldThrowExceptionWhenExpenseIsNotFound() {
            ExpenseRequestDto requestDto = buildRequestDto(EDITED_AMOUNT, CATEGORY);

            doThrow(new RequestedEntityNotFoundException("Expense not found")).when(expenseManagerService).getExpenseByUserIdOrThrow(EXPENSE_ID, USER_ID);

            assertThrows(RequestedEntityNotFoundException.class, () -> expenseService.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest));

            verifyNoInteractions(authBackendClient, riskGuardService, limitRepository, walletService, roundUpService, expenseRepository, outboxService);
        }

        @Nested
        class WhenExpenseExists {

            private Expense existingExpense;
            private ExpenseRequestDto requestDto;

            @BeforeEach
            void setUp() {
                existingExpense = buildExpense(EXPENSE_ID, OLD_AMOUNT, CATEGORY);
                requestDto = buildRequestDto(EDITED_AMOUNT, CATEGORY);

                when(expenseManagerService.getExpenseByUserIdOrThrow(EXPENSE_ID, USER_ID)).thenReturn(existingExpense);
            }

            @Test
            void shouldReturnExpenseIdWhenEditIsSuccessful() {
                Long result = expenseService.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest);

                assertEquals(EXPENSE_ID, result);
            }

            @Test
            void shouldThrowExceptionWhenAuthorizationCodeConfirmationFails() {
                doThrow(new IllegalStateException("invalid code")).when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

                assertThrows(IllegalStateException.class, () -> expenseService.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest));

                verifyNoInteractions(riskGuardService, limitRepository, walletService, expenseRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenRiskGuardFails() {
                when(authBackendClient.getUserEmail(USER_ID)).thenReturn(USER_EMAIL);
                doThrow(new IllegalStateException("risk detected")).when(riskGuardService).guard(USER_ID, RiskTriggerType.EXPENSE, EDITED_AMOUNT, CATEGORY.name(), USER_EMAIL, RISK_SOURCE_EVENT_ID, servletRequest);

                assertThrows(IllegalStateException.class, () -> expenseService.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest));

                verifyNoInteractions(limitRepository, walletService, expenseRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenGeneralLimitIsExceeded() {
                ExpenseRequestDto exceedingRequestDto = buildRequestDto(BigDecimal.valueOf(900), CATEGORY);
                Limit generalLimit = buildLimit(null, BigDecimal.valueOf(400));

                when(limitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(generalLimit));
                when(financialPeriodService.getExpensesSum(USER_ID, PERIOD_TYPE, null)).thenReturn(BigDecimal.ZERO);

                assertThrows(MissingRequirementException.class, () -> expenseService.editExpense(exceedingRequestDto, USER_ID, EXPENSE_ID, servletRequest));

                verifyNoInteractions(walletService, roundUpService, outboxService);
                verify(expenseRepository, never()).save(any(Expense.class));
            }

            @Test
            void shouldThrowExceptionWhenCategoryLimitIsExceeded() {
                ExpenseRequestDto exceedingRequestDto = buildRequestDto(BigDecimal.valueOf(900), CATEGORY);
                Limit categoryLimit = buildLimit(CATEGORY, BigDecimal.valueOf(400));

                when(limitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(categoryLimit));
                when(financialPeriodService.getExpensesSum(USER_ID, PERIOD_TYPE, CATEGORY)).thenReturn(BigDecimal.ZERO);

                assertThrows(MissingRequirementException.class, () -> expenseService.editExpense(exceedingRequestDto, USER_ID, EXPENSE_ID, servletRequest));

                verifyNoInteractions(walletService, roundUpService, outboxService);
                verify(expenseRepository, never()).save(any(Expense.class));
            }

            @Test
            void shouldThrowExceptionWhenOldAmountDoesNotCountTowardsLimitOfNewCategory() {
                ExpenseRequestDto categoryRequestDto = buildRequestDto(OLD_AMOUNT, CATEGORY);
                Limit categoryLimit = buildLimit(CATEGORY, BigDecimal.valueOf(400));

                existingExpense.setCategory(OTHER_CATEGORY);

                when(limitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(categoryLimit));
                when(financialPeriodService.getExpensesSum(USER_ID, PERIOD_TYPE, CATEGORY)).thenReturn(BigDecimal.valueOf(350));

                assertThrows(MissingRequirementException.class, () -> expenseService.editExpense(categoryRequestDto, USER_ID, EXPENSE_ID, servletRequest));

                verifyNoInteractions(walletService);
            }

            @Test
            void shouldEditExpenseWhenOldAmountIsSubtractedAndTotalEqualsLimit() {
                ExpenseRequestDto boundaryRequestDto = buildRequestDto(BigDecimal.valueOf(200), CATEGORY);
                Limit generalLimit = buildLimit(null, BigDecimal.valueOf(400));

                when(limitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(generalLimit));
                when(financialPeriodService.getExpensesSum(USER_ID, PERIOD_TYPE, null)).thenReturn(BigDecimal.valueOf(300));
                when(limitCalculateService.calculateLimitStats(eq(generalLimit), eq(USER_ID), any(LocalDate.class))).thenReturn(limitStatsDto);

                Long result = expenseService.editExpense(boundaryRequestDto, USER_ID, EXPENSE_ID, servletRequest);

                assertEquals(EXPENSE_ID, result);
                verify(expenseRepository).save(existingExpense);
            }

            @Test
            void shouldSkipLimitValidationWhenNewCategoryDiffersFromLimitCategory() {
                ExpenseRequestDto otherCategoryRequestDto = buildRequestDto(EDITED_AMOUNT, OTHER_CATEGORY);
                Limit categoryLimit = buildLimit(CATEGORY, BigDecimal.ONE);

                when(limitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(categoryLimit));
                when(limitCalculateService.calculateLimitStats(eq(categoryLimit), eq(USER_ID), any(LocalDate.class))).thenReturn(limitStatsDto);

                Long result = expenseService.editExpense(otherCategoryRequestDto, USER_ID, EXPENSE_ID, servletRequest);

                assertEquals(EXPENSE_ID, result);
                verifyNoInteractions(financialPeriodService);
            }

            @Test
            void shouldConfirmAuthorizationCodeWhenEditingExpense() {
                expenseService.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest);

                verify(additionalAuthorizationCodeResolver).resolve(confirmAuthorizationCodeDto);
                verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
            }

            @Test
            void shouldGuardAgainstRiskWhenEditingExpense() {
                when(authBackendClient.getUserEmail(USER_ID)).thenReturn(USER_EMAIL);

                expenseService.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest);

                verify(riskGuardService).guard(USER_ID, RiskTriggerType.EXPENSE, EDITED_AMOUNT, CATEGORY.name(), USER_EMAIL, RISK_SOURCE_EVENT_ID, servletRequest);
            }

            @Test
            void shouldConfirmAuthorizationCodeBeforeGuardingAgainstRisk() {
                expenseService.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest);

                InOrder order = inOrder(authBackendClient, riskGuardService);

                order.verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
                order.verify(riskGuardService).guard(USER_ID, RiskTriggerType.EXPENSE, EDITED_AMOUNT, CATEGORY.name(), null, RISK_SOURCE_EVENT_ID, servletRequest);
            }

            @Test
            void shouldAdjustWalletBalanceWhenEditingExpense() {
                expenseService.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest);

                verify(walletService).addBalanceToWallet(USER_ID, OLD_AMOUNT);
                verify(walletService).removeBalanceFromWallet(USER_ID, EDITED_AMOUNT);
            }

            @Test
            void shouldReturnOldAmountToWalletBeforeRemovingNewAmount() {
                expenseService.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest);

                InOrder order = inOrder(walletService);

                order.verify(walletService).addBalanceToWallet(USER_ID, OLD_AMOUNT);
                order.verify(walletService).removeBalanceFromWallet(USER_ID, EDITED_AMOUNT);
            }

            @Test
            void shouldRollbackThenApplyRoundUpWhenEditingExpense() {
                expenseService.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest);

                InOrder order = inOrder(roundUpService);

                order.verify(roundUpService).handleExpenseForRoundUp(USER_ID, EXPENSE_ID, PiggyBankAutomationMode.ROLLBACK);
                order.verify(roundUpService).handleExpenseForRoundUp(USER_ID, EXPENSE_ID, PiggyBankAutomationMode.APPLY);
            }

            @Test
            void shouldUpdateExpenseFieldsWhenEditingExpense() {
                ExpenseRequestDto otherCategoryRequestDto = buildRequestDto(EDITED_AMOUNT, OTHER_CATEGORY);

                expenseService.editExpense(otherCategoryRequestDto, USER_ID, EXPENSE_ID, servletRequest);

                assertEquals(EDITED_AMOUNT, existingExpense.getAmount());
                assertEquals(OTHER_CATEGORY, existingExpense.getCategory());
                assertEquals(DESCRIPTION, existingExpense.getDescription());
            }

            @Test
            void shouldSaveUpdatedExpenseWhenEditingExpense() {
                expenseService.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest);

                verify(expenseRepository).save(existingExpense);
            }

            @Test
            void shouldSaveOutboxEventWhenEditingExpense() {
                expenseService.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest);

                verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(EXPENSE_ID.toString()), eq(OUTBOX_EVENT), any(ExpenseActivityEvent.class));
            }

            @Test
            void shouldCallSmartScanServiceWithEditModeWhenEditingExpense() {
                expenseService.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest);

                verify(smartScanService).handleSmartScan(USER_ID, confirmPasswordDto, EDITED_AMOUNT, SmartScanMode.EDIT);
            }

            @Test
            void shouldCallControlAmountServiceWhenEditingExpense() {
                expenseService.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest);

                verify(controlAmountService).handleExpenseAmountControl(USER_ID, EDITED_AMOUNT);
            }

            @Test
            void shouldNotCallCountQuantityLimitServiceWhenEditingExpense() {
                expenseService.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest);

                verifyNoInteractions(countQuantityLimitService);
            }

            @Test
            void shouldNotPublishLimitStatsEventsWhenNoLimitsExist() {
                expenseService.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest);

                verifyNoInteractions(kafkaTemplate, limitCalculateService);
            }

            @Test
            void shouldPublishLimitStatsEventsWhenEditingExpense() {
                Limit generalLimit = buildLimit(null, BigDecimal.valueOf(1000));

                when(limitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(generalLimit));
                when(financialPeriodService.getExpensesSum(USER_ID, PERIOD_TYPE, null)).thenReturn(BigDecimal.ZERO);
                when(limitCalculateService.calculateLimitStats(eq(generalLimit), eq(USER_ID), any(LocalDate.class))).thenReturn(limitStatsDto);

                expenseService.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest);

                verify(kafkaTemplate).send(eq(LIMIT_STATS_TOPIC), any(LimitStatsEvent.class));
            }
        }
    }

    @Nested
    class GetExpense {

        @Test
        void shouldReturnMappedExpenseDtosWhenExpensesExist() {
            Expense firstExpense = buildExpense(1L, BigDecimal.valueOf(50), CATEGORY);
            Expense secondExpense = buildExpense(2L, BigDecimal.valueOf(75), OTHER_CATEGORY);
            ExpenseDto firstDto = new ExpenseDto(1L, USER_ID, BigDecimal.valueOf(50), CATEGORY, LocalDate.now(), "first");
            ExpenseDto secondDto = new ExpenseDto(2L, USER_ID, BigDecimal.valueOf(75), OTHER_CATEGORY, LocalDate.now(), "second");

            when(expenseRepository.findAllByUserId(USER_ID)).thenReturn(List.of(firstExpense, secondExpense));
            when(expenseMapper.mapExpenseToDto(firstExpense)).thenReturn(firstDto);
            when(expenseMapper.mapExpenseToDto(secondExpense)).thenReturn(secondDto);

            List<ExpenseDto> result = expenseService.getExpense(USER_ID);

            assertEquals(List.of(firstDto, secondDto), result);
        }

        @Test
        void shouldMapEachExpenseWhenExpensesExist() {
            Expense firstExpense = buildExpense(1L, BigDecimal.valueOf(50), CATEGORY);
            Expense secondExpense = buildExpense(2L, BigDecimal.valueOf(75), OTHER_CATEGORY);

            when(expenseRepository.findAllByUserId(USER_ID)).thenReturn(List.of(firstExpense, secondExpense));

            expenseService.getExpense(USER_ID);

            verify(expenseMapper).mapExpenseToDto(firstExpense);
            verify(expenseMapper).mapExpenseToDto(secondExpense);
        }

        @Test
        void shouldReturnEmptyListWhenNoExpensesExist() {
            when(expenseRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

            List<ExpenseDto> result = expenseService.getExpense(USER_ID);

            assertTrue(result.isEmpty());
            verifyNoInteractions(expenseMapper);
        }
    }

    @Nested
    class DeleteExpense {

        @Test
        void shouldThrowExceptionWhenExpenseIsNotFound() {
            doThrow(new RequestedEntityNotFoundException("Expense not found")).when(expenseManagerService).getExpenseByUserIdOrThrow(EXPENSE_ID, USER_ID);

            assertThrows(RequestedEntityNotFoundException.class, () -> expenseService.deleteExpense(EXPENSE_ID, USER_ID, AUTHORIZATION_CODE));

            verifyNoInteractions(authBackendClient, roundUpService, walletService, outboxService, expenseRepository, limitRepository);
        }

        @Nested
        class WhenExpenseExists {

            private Expense expense;

            @BeforeEach
            void setUp() {
                expense = buildExpense(EXPENSE_ID, DELETED_AMOUNT, CATEGORY);

                when(expenseManagerService.getExpenseByUserIdOrThrow(EXPENSE_ID, USER_ID)).thenReturn(expense);
            }

            @Test
            void shouldThrowExceptionWhenAuthorizationCodeConfirmationFails() {
                doThrow(new IllegalStateException("invalid code")).when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

                assertThrows(IllegalStateException.class, () -> expenseService.deleteExpense(EXPENSE_ID, USER_ID, AUTHORIZATION_CODE));

                verifyNoInteractions(roundUpService, walletService, outboxService, expenseRepository, limitRepository);
            }

            @Test
            void shouldDeleteExpenseWhenExpenseExists() {
                expenseService.deleteExpense(EXPENSE_ID, USER_ID, AUTHORIZATION_CODE);

                verify(expenseRepository).delete(expense);
            }

            @Test
            void shouldConfirmAuthorizationCodeWhenDeletingExpense() {
                expenseService.deleteExpense(EXPENSE_ID, USER_ID, AUTHORIZATION_CODE);

                verify(additionalAuthorizationCodeResolver).resolve(AUTHORIZATION_CODE);
                verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
            }

            @Test
            void shouldRollbackRoundUpWhenDeletingExpense() {
                expenseService.deleteExpense(EXPENSE_ID, USER_ID, AUTHORIZATION_CODE);

                verify(roundUpService).handleExpenseForRoundUp(USER_ID, EXPENSE_ID, PiggyBankAutomationMode.ROLLBACK);
            }

            @Test
            void shouldAddBalanceBackToWalletWhenDeletingExpense() {
                expenseService.deleteExpense(EXPENSE_ID, USER_ID, AUTHORIZATION_CODE);

                verify(walletService).addBalanceToWallet(USER_ID, DELETED_AMOUNT);
            }

            @Test
            void shouldSaveOutboxEventWhenDeletingExpense() {
                expenseService.deleteExpense(EXPENSE_ID, USER_ID, AUTHORIZATION_CODE);

                verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(EXPENSE_ID.toString()), eq(OUTBOX_EVENT), any(ExpenseActivityEvent.class));
            }

            @Test
            void shouldPerformDeletionStepsInOrderWhenDeletingExpense() {
                expenseService.deleteExpense(EXPENSE_ID, USER_ID, AUTHORIZATION_CODE);

                InOrder order = inOrder(authBackendClient, roundUpService, walletService, outboxService, expenseRepository);

                order.verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
                order.verify(roundUpService).handleExpenseForRoundUp(USER_ID, EXPENSE_ID, PiggyBankAutomationMode.ROLLBACK);
                order.verify(walletService).addBalanceToWallet(USER_ID, DELETED_AMOUNT);
                order.verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(EXPENSE_ID.toString()), eq(OUTBOX_EVENT), any(ExpenseActivityEvent.class));
                order.verify(expenseRepository).delete(expense);
            }

            @Test
            void shouldNotPublishLimitStatsEventsWhenNoLimitsExist() {
                expenseService.deleteExpense(EXPENSE_ID, USER_ID, AUTHORIZATION_CODE);

                verify(limitRepository).findAllByUserId(USER_ID);
                verifyNoInteractions(kafkaTemplate, limitCalculateService);
            }

            @Test
            void shouldPublishLimitStatsEventsWhenDeletingExpense() {
                Limit generalLimit = buildLimit(null, BigDecimal.valueOf(500));

                when(limitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(generalLimit));
                when(limitCalculateService.calculateLimitStats(eq(generalLimit), eq(USER_ID), any(LocalDate.class))).thenReturn(limitStatsDto);

                expenseService.deleteExpense(EXPENSE_ID, USER_ID, AUTHORIZATION_CODE);

                verify(kafkaTemplate).send(eq(LIMIT_STATS_TOPIC), any(LimitStatsEvent.class));
            }

            @Test
            void shouldPublishLimitStatsEventForEachLimitWhenDeletingExpense() {
                Limit firstLimit = buildLimit(null, BigDecimal.valueOf(500));
                Limit secondLimit = buildLimit(CATEGORY, BigDecimal.valueOf(300));

                when(limitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(firstLimit, secondLimit));
                when(limitCalculateService.calculateLimitStats(any(Limit.class), eq(USER_ID), any(LocalDate.class))).thenReturn(limitStatsDto);

                expenseService.deleteExpense(EXPENSE_ID, USER_ID, AUTHORIZATION_CODE);

                verify(kafkaTemplate, times(2)).send(eq(LIMIT_STATS_TOPIC), any(LimitStatsEvent.class));
            }
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteAllExpensesWhenUserIdIsProvided() {
            expenseService.deleteByUserId(USER_ID);

            verify(expenseRepository).deleteByUserId(USER_ID);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFailsOnDeleteByUserId() {
            doThrow(new IllegalStateException("database error")).when(expenseRepository).deleteByUserId(USER_ID);

            assertThrows(IllegalStateException.class, () -> expenseService.deleteByUserId(USER_ID));
        }
    }
}