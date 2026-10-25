package com.finovara.financeservice.expense.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.contracts.mainaccount.activity.event.expense.ExpenseActivityEvent;
import com.finovara.contracts.mainaccount.transaction.event.limit.LimitStatsEvent;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.securitymonitoring.model.RiskTriggerType;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
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
import com.finovara.financeservice.limit.service.LimitSpendingService;
import com.finovara.financeservice.riskverification.service.RiskGuardService;
import com.finovara.financeservice.settings.finances.expense.controlamount.service.ControlAmountService;
import com.finovara.financeservice.settings.finances.expense.quantitylimit.dto.CountQuantityLimitDto;
import com.finovara.financeservice.settings.finances.expense.quantitylimit.service.CountQuantityLimitService;
import com.finovara.financeservice.settings.finances.expense.smartscan.dto.SmartScanMode;
import com.finovara.financeservice.settings.finances.expense.smartscan.service.SmartScanService;
import com.finovara.financeservice.settings.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.financeservice.settings.piggybank.roundup.service.RoundUpService;
import com.finovara.financeservice.util.transaction.TransactionOrigin;
import com.finovara.financeservice.util.transaction.expense.ExpenseManagerService;
import com.finovara.financeservice.wallet.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long EXPENSE_ID = 5L;
    private static final Long LIMIT_ID = 7L;
    private static final Long OTHER_LIMIT_ID = 8L;
    private static final String AUTH_CODE = "123456";
    private static final String EXPENSE_AGGREGATE = "Expense";
    private static final String EXPENSE_TOPIC = "expense.created";
    private static final String LIMIT_AGGREGATE = "Limit";
    private static final String LIMIT_STATS_TOPIC = "limit.calculate-stats";
    private static final int PAGE_SIZE = 10;
    private static final BigDecimal AMOUNT = BigDecimal.TEN;
    private static final BigDecimal LIMIT_AMOUNT = BigDecimal.valueOf(100);
    private static final ExpenseCategory CATEGORY = ExpenseCategory.values()[0];
    private static final ExpenseCategory OTHER_CATEGORY = ExpenseCategory.values()[1];
    private static final PeriodType PERIOD = PeriodType.values()[0];
    private static final TransactionOrigin AUTOMATIC_ORIGIN = Arrays.stream(TransactionOrigin.values())
            .filter(origin -> origin != TransactionOrigin.USER_MANUAL)
            .findFirst()
            .orElseThrow();

    @Mock
    private OutboxService outboxService;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private LimitRepository limitRepository;

    @Mock
    private LimitCalculateService limitCalculateService;

    @Mock
    private LimitSpendingService limitSpendingService;

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
    private AuthBackendClient authBackendClient;

    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Mock
    private RiskGuardService riskGuardService;

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private ExpenseRequestDto requestDto;

    @Mock
    private ExpenseDto expenseDto;

    @Mock
    private CountQuantityLimitDto countQuantityLimitDto;

    @Mock
    private LimitStatsDto statsDto;

    @Mock
    private LimitStatsDto otherStatsDto;

    @Captor
    private ArgumentCaptor<Expense> expenseCaptor;

    @Captor
    private ArgumentCaptor<List<Expense>> expensesCaptor;

    @InjectMocks
    private ExpenseService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pageSize", PAGE_SIZE);
    }

    @Nested
    class AddExpense {

        @Nested
        class AmountValidation {

            @BeforeEach
            void setUp() {
                when(requestDto.expenseDto()).thenReturn(expenseDto);
            }

            @Test
            void shouldThrowExceptionWhenAmountIsNull() {
                assertThrows(InvalidInputException.class, () -> service.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));
                verifyNoInteractions(riskGuardService, walletService, expenseRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenAmountIsZero() {
                when(expenseDto.amount()).thenReturn(BigDecimal.ZERO);

                assertThrows(InvalidInputException.class, () -> service.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));
                verifyNoInteractions(riskGuardService, walletService, expenseRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenAmountIsBelowOne() {
                when(expenseDto.amount()).thenReturn(new BigDecimal("0.99"));

                assertThrows(InvalidInputException.class, () -> service.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));
                verifyNoInteractions(riskGuardService, walletService, expenseRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenAmountIsNegative() {
                when(expenseDto.amount()).thenReturn(BigDecimal.valueOf(-5));

                assertThrows(InvalidInputException.class, () -> service.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));
                verifyNoInteractions(riskGuardService, walletService, expenseRepository, outboxService);
            }
        }

        @Nested
        class ValidAmount {

            private Limit generalLimit;
            private Limit categoryLimit;
            private Expense savedExpense;

            @BeforeEach
            void setUp() {
                when(requestDto.expenseDto()).thenReturn(expenseDto);
                when(expenseDto.amount()).thenReturn(AMOUNT);
                when(expenseDto.category()).thenReturn(CATEGORY);
                generalLimit = Limit.builder().id(LIMIT_ID).periodType(PERIOD).category(null).amount(LIMIT_AMOUNT).build();
                categoryLimit = Limit.builder().id(OTHER_LIMIT_ID).periodType(PERIOD).category(CATEGORY).amount(LIMIT_AMOUNT).build();
                savedExpense = Expense.builder().id(EXPENSE_ID).amount(AMOUNT).category(CATEGORY).userId(USER_ID).build();
            }

            @Test
            void shouldThrowExceptionWhenGeneralLimitExceeded() {
                List<Limit> limits = List.of(generalLimit);
                when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(limits);
                when(limitSpendingService.calculateSpent(eq(USER_ID), eq(limits), any(LocalDate.class)))
                        .thenReturn(Map.of(LIMIT_ID, BigDecimal.valueOf(91)));

                assertThrows(MissingRequirementException.class, () -> service.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));
                verify(expenseRepository, never()).save(any(Expense.class));
                verify(walletService, never()).removeBalanceFromWallet(any(), any());
                verifyNoInteractions(outboxService, countQuantityLimitService);
            }

            @Test
            void shouldThrowExceptionWhenCategoryLimitExceeded() {
                List<Limit> limits = List.of(categoryLimit);
                when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(limits);
                when(limitSpendingService.calculateSpent(eq(USER_ID), eq(limits), any(LocalDate.class)))
                        .thenReturn(Map.of(OTHER_LIMIT_ID, BigDecimal.valueOf(100)));

                assertThrows(MissingRequirementException.class, () -> service.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));
                verify(expenseRepository, never()).save(any(Expense.class));
                verifyNoInteractions(outboxService, countQuantityLimitService);
            }

            @Test
            void shouldThrowExceptionWhenRiskGuardRejectsExpense() {
                doThrow(new IllegalStateException()).when(riskGuardService)
                        .guard(any(), any(), any(), any(), any(), any(), any());

                assertThrows(IllegalStateException.class, () -> service.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));
                verify(authBackendClient, never()).confirmAuthorizationCode(any(), any());
                verifyNoInteractions(walletService, expenseRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenAuthorizationCodeIsInvalid() {
                doThrow(new IllegalStateException()).when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

                assertThrows(IllegalStateException.class, () -> service.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));
                verifyNoInteractions(walletService, expenseRepository, outboxService);
            }

            @Nested
            class AfterLimitValidation {

                @BeforeEach
                void setUp() {
                    when(requestDto.countQuantityLimitDto()).thenReturn(countQuantityLimitDto);
                    when(countQuantityLimitDto.periodType()).thenReturn(PERIOD);
                }

                @Test
                void shouldSaveExpenseAndReturnIdWhenOriginIsUserManual() {
                    when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of());
                    when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

                    Long result = service.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL);

                    assertEquals(EXPENSE_ID, result);
                    verify(expenseRepository).save(expenseCaptor.capture());
                    assertEquals(AMOUNT, expenseCaptor.getValue().getAmount());
                    assertEquals(CATEGORY, expenseCaptor.getValue().getCategory());
                    assertEquals(USER_ID, expenseCaptor.getValue().getUserId());
                    assertEquals(LocalDate.now(), expenseCaptor.getValue().getCreatedAt());
                }

                @Test
                void shouldGuardRiskAndConfirmAuthorizationWhenOriginIsUserManual() {
                    when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of());
                    when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

                    service.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL);

                    verify(riskGuardService).guard(eq(USER_ID), eq(RiskTriggerType.EXPENSE), eq(AMOUNT), eq(CATEGORY.name()), any(), any(), eq(servletRequest));
                    verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
                }

                @Test
                void shouldSkipRiskGuardAndAuthorizationWhenOriginIsNotUserManual() {
                    when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of());
                    when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

                    Long result = service.addExpense(requestDto, USER_ID, servletRequest, AUTOMATIC_ORIGIN);

                    assertEquals(EXPENSE_ID, result);
                    verifyNoInteractions(riskGuardService, authBackendClient, additionalAuthorizationCodeResolver);
                }

                @Test
                void shouldApplyAllSideEffectsWhenExpenseAdded() {
                    when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of());
                    when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

                    service.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL);

                    verify(walletService).lockWalletForUpdate(USER_ID);
                    verify(walletService).removeBalanceFromWallet(USER_ID, AMOUNT);
                    verify(countQuantityLimitService).handleExpenseLimitExceeded(eq(USER_ID), eq(countQuantityLimitDto), eq(PERIOD), any());
                    verify(smartScanService).handleSmartScan(eq(USER_ID), any(), eq(AMOUNT), eq(SmartScanMode.ADD));
                    verify(outboxService).save(eq(EXPENSE_AGGREGATE), eq(EXPENSE_ID.toString()), eq(EXPENSE_TOPIC), any(ExpenseActivityEvent.class));
                    verify(roundUpService).handleExpenseForRoundUp(USER_ID, EXPENSE_ID, PiggyBankAutomationMode.APPLY);
                    verify(controlAmountService).handleExpenseAmountControl(USER_ID, AMOUNT);
                }

                @Test
                void shouldLockWalletBeforeValidatingLimitsAndRemovingBalance() {
                    when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of());
                    when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

                    service.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL);

                    InOrder inOrder = inOrder(walletService, limitRepository, expenseRepository);
                    inOrder.verify(walletService).lockWalletForUpdate(USER_ID);
                    inOrder.verify(limitRepository).findAllActiveByUserId(USER_ID);
                    inOrder.verify(walletService).removeBalanceFromWallet(USER_ID, AMOUNT);
                    inOrder.verify(expenseRepository).save(any(Expense.class));
                }

                @Test
                void shouldNotCalculateLimitStatsWhenNoActiveLimits() {
                    when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of());
                    when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

                    service.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL);

                    verifyNoInteractions(limitSpendingService, limitCalculateService);
                }

                @Test
                void shouldPublishLimitStatsEventForEachLimitWhenLimitsNotExceeded() {
                    List<Limit> limits = List.of(generalLimit, categoryLimit);
                    when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(limits);
                    when(limitSpendingService.calculateSpent(eq(USER_ID), eq(limits), any(LocalDate.class)))
                            .thenReturn(Map.of(LIMIT_ID, BigDecimal.valueOf(20), OTHER_LIMIT_ID, BigDecimal.valueOf(30)));
                    when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);
                    when(limitCalculateService.calculateLimitStats(eq(limits), eq(USER_ID), any(LocalDate.class))).thenReturn(List.of(statsDto, otherStatsDto));
                    when(statsDto.limitId()).thenReturn(LIMIT_ID);
                    when(statsDto.percentage()).thenReturn(BigDecimal.valueOf(30));
                    when(statsDto.periodType()).thenReturn(PERIOD);
                    when(otherStatsDto.limitId()).thenReturn(OTHER_LIMIT_ID);
                    when(otherStatsDto.percentage()).thenReturn(BigDecimal.valueOf(40));
                    when(otherStatsDto.periodType()).thenReturn(PERIOD);

                    service.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL);

                    verify(outboxService).save(eq(LIMIT_AGGREGATE), eq(LIMIT_ID.toString()), eq(LIMIT_STATS_TOPIC), any(LimitStatsEvent.class));
                    verify(outboxService).save(eq(LIMIT_AGGREGATE), eq(OTHER_LIMIT_ID.toString()), eq(LIMIT_STATS_TOPIC), any(LimitStatsEvent.class));
                }

                @Test
                void shouldSaveExpenseWhenTotalEqualsLimitAmount() {
                    List<Limit> limits = List.of(generalLimit);
                    when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(limits);
                    when(limitSpendingService.calculateSpent(eq(USER_ID), eq(limits), any(LocalDate.class)))
                            .thenReturn(Map.of(LIMIT_ID, BigDecimal.valueOf(90)));
                    when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);
                    when(limitCalculateService.calculateLimitStats(eq(limits), eq(USER_ID), any(LocalDate.class))).thenReturn(List.of());

                    Long result = service.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL);

                    assertEquals(EXPENSE_ID, result);
                    verify(expenseRepository).save(any(Expense.class));
                }

                @Test
                void shouldIgnoreLimitWhenLimitCategoryDoesNotMatchExpenseCategory() {
                    Limit otherCategoryLimit = Limit.builder().id(LIMIT_ID).periodType(PERIOD).category(OTHER_CATEGORY).amount(LIMIT_AMOUNT).build();
                    List<Limit> limits = List.of(otherCategoryLimit);
                    when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(limits);
                    when(limitSpendingService.calculateSpent(eq(USER_ID), eq(limits), any(LocalDate.class)))
                            .thenReturn(Map.of(LIMIT_ID, BigDecimal.valueOf(1000)));
                    when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);
                    when(limitCalculateService.calculateLimitStats(eq(limits), eq(USER_ID), any(LocalDate.class))).thenReturn(List.of());

                    Long result = service.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL);

                    assertEquals(EXPENSE_ID, result);
                }

                @Test
                void shouldThrowExceptionWhenSmartScanRejectsExpense() {
                    when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of());
                    doThrow(new IllegalStateException()).when(smartScanService)
                            .handleSmartScan(eq(USER_ID), any(), eq(AMOUNT), eq(SmartScanMode.ADD));

                    assertThrows(IllegalStateException.class, () -> service.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));
                    verify(walletService, never()).removeBalanceFromWallet(any(), any());
                    verifyNoInteractions(expenseRepository, outboxService);
                }

                @Test
                void shouldThrowExceptionWhenWalletBalanceIsInsufficient() {
                    when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of());
                    doThrow(new IllegalStateException()).when(walletService).removeBalanceFromWallet(USER_ID, AMOUNT);

                    assertThrows(IllegalStateException.class, () -> service.addExpense(requestDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));
                    verifyNoInteractions(expenseRepository, outboxService);
                }
            }
        }
    }

    @Nested
    class EditExpense {

        @BeforeEach
        void setUp() {
            when(requestDto.expenseDto()).thenReturn(expenseDto);
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNull() {
            assertThrows(InvalidInputException.class, () -> service.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest));
            verifyNoInteractions(authBackendClient, riskGuardService, walletService, expenseManagerService);
        }

        @Test
        void shouldThrowExceptionWhenAmountIsBelowOne() {
            when(expenseDto.amount()).thenReturn(new BigDecimal("0.5"));

            assertThrows(InvalidInputException.class, () -> service.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest));
            verifyNoInteractions(authBackendClient, riskGuardService, walletService, expenseManagerService);
        }

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeIsInvalid() {
            when(expenseDto.amount()).thenReturn(AMOUNT);
            doThrow(new IllegalStateException()).when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class, () -> service.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest));
            verifyNoInteractions(riskGuardService, walletService, expenseManagerService, outboxService);
        }

        @Nested
        class Authorized {

            private Expense existing;
            private Limit generalLimit;
            private Limit categoryLimit;

            @BeforeEach
            void setUp() {
                when(expenseDto.amount()).thenReturn(AMOUNT);
                when(expenseDto.category()).thenReturn(CATEGORY);
                existing = Expense.builder().id(EXPENSE_ID).amount(BigDecimal.valueOf(5)).category(CATEGORY).createdAt(LocalDate.now()).userId(USER_ID).build();
                generalLimit = Limit.builder().id(LIMIT_ID).periodType(PERIOD).category(null).amount(LIMIT_AMOUNT).build();
                categoryLimit = Limit.builder().id(OTHER_LIMIT_ID).periodType(PERIOD).category(CATEGORY).amount(LIMIT_AMOUNT).build();
            }

            @Test
            void shouldUpdateExpenseAndReturnIdWhenAmountIncreased() {
                when(expenseManagerService.getExpenseByUserIdOrThrow(EXPENSE_ID, USER_ID)).thenReturn(existing);
                when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of());

                Long result = service.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest);

                assertEquals(EXPENSE_ID, result);
                assertEquals(AMOUNT, existing.getAmount());
                assertEquals(CATEGORY, existing.getCategory());
                verify(walletService).removeBalanceFromWallet(USER_ID, BigDecimal.valueOf(5));
                verify(walletService, never()).addBalanceToWallet(any(), any());
            }

            @Test
            void shouldAddBalanceToWalletWhenAmountDecreased() {
                existing.setAmount(BigDecimal.valueOf(25));
                when(expenseManagerService.getExpenseByUserIdOrThrow(EXPENSE_ID, USER_ID)).thenReturn(existing);
                when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of());

                service.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest);

                verify(walletService).addBalanceToWallet(USER_ID, BigDecimal.valueOf(15));
                verify(walletService, never()).removeBalanceFromWallet(any(), any());
            }

            @Test
            void shouldNotChangeWalletBalanceWhenAmountUnchanged() {
                existing.setAmount(AMOUNT);
                when(expenseManagerService.getExpenseByUserIdOrThrow(EXPENSE_ID, USER_ID)).thenReturn(existing);
                when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of());

                service.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest);

                verify(walletService, never()).addBalanceToWallet(any(), any());
                verify(walletService, never()).removeBalanceFromWallet(any(), any());
            }

            @Test
            void shouldGuardRiskAndLockWalletWhenExpenseEdited() {
                when(expenseManagerService.getExpenseByUserIdOrThrow(EXPENSE_ID, USER_ID)).thenReturn(existing);
                when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of());

                service.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest);

                verify(riskGuardService).guard(eq(USER_ID), eq(RiskTriggerType.EXPENSE), eq(AMOUNT), eq(CATEGORY.name()), any(), any(), eq(servletRequest));
                verify(walletService).lockWalletForUpdate(USER_ID);
            }

            @Test
            void shouldApplyAllSideEffectsWhenExpenseEdited() {
                when(expenseManagerService.getExpenseByUserIdOrThrow(EXPENSE_ID, USER_ID)).thenReturn(existing);
                when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of());

                service.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest);

                verify(outboxService).save(eq(EXPENSE_AGGREGATE), eq(EXPENSE_ID.toString()), eq(EXPENSE_TOPIC), any(ExpenseActivityEvent.class));
                verify(smartScanService).handleSmartScan(eq(USER_ID), any(), eq(AMOUNT), eq(SmartScanMode.EDIT));
                verify(controlAmountService).handleExpenseAmountControl(USER_ID, AMOUNT);
            }

            @Test
            void shouldRollbackRoundUpBeforeApplyingItAgain() {
                when(expenseManagerService.getExpenseByUserIdOrThrow(EXPENSE_ID, USER_ID)).thenReturn(existing);
                when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of());

                service.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest);

                InOrder inOrder = inOrder(roundUpService);
                inOrder.verify(roundUpService).handleExpenseForRoundUp(USER_ID, EXPENSE_ID, PiggyBankAutomationMode.ROLLBACK);
                inOrder.verify(roundUpService).handleExpenseForRoundUp(USER_ID, EXPENSE_ID, PiggyBankAutomationMode.APPLY);
            }

            @Test
            void shouldNotChangeExpenseWhenGeneralLimitExceeded() {
                List<Limit> limits = List.of(generalLimit);
                when(expenseManagerService.getExpenseByUserIdOrThrow(EXPENSE_ID, USER_ID)).thenReturn(existing);
                when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(limits);
                when(limitSpendingService.calculateSpent(eq(USER_ID), eq(limits), any(LocalDate.class)))
                        .thenReturn(Map.of(LIMIT_ID, BigDecimal.valueOf(100)));

                assertThrows(MissingRequirementException.class, () -> service.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest));
                assertEquals(BigDecimal.valueOf(5), existing.getAmount());
                verify(walletService, never()).removeBalanceFromWallet(any(), any());
                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldSucceedWhenTotalAfterReplacingOldAmountEqualsLimit() {
                List<Limit> limits = List.of(generalLimit);
                when(expenseManagerService.getExpenseByUserIdOrThrow(EXPENSE_ID, USER_ID)).thenReturn(existing);
                when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(limits);
                when(limitSpendingService.calculateSpent(eq(USER_ID), eq(limits), any(LocalDate.class)))
                        .thenReturn(Map.of(LIMIT_ID, BigDecimal.valueOf(95)));
                when(limitCalculateService.calculateLimitStats(eq(limits), eq(USER_ID), any(LocalDate.class))).thenReturn(List.of());

                Long result = service.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest);

                assertEquals(EXPENSE_ID, result);
                assertEquals(AMOUNT, existing.getAmount());
            }

            @Test
            void shouldNotSubtractOldAmountWhenOldCategoryDoesNotMatchLimit() {
                existing.setCategory(OTHER_CATEGORY);
                List<Limit> limits = List.of(categoryLimit);
                when(expenseManagerService.getExpenseByUserIdOrThrow(EXPENSE_ID, USER_ID)).thenReturn(existing);
                when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(limits);
                when(limitSpendingService.calculateSpent(eq(USER_ID), eq(limits), any(LocalDate.class)))
                        .thenReturn(Map.of(OTHER_LIMIT_ID, BigDecimal.valueOf(95)));

                assertThrows(MissingRequirementException.class, () -> service.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest));
                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldIgnoreLimitWhenExpenseWasCreatedBeforeLimitPeriodStart() {
                existing.setCreatedAt(PERIOD.getStartDate(LocalDate.now()).minusDays(1));
                List<Limit> limits = List.of(generalLimit);
                when(expenseManagerService.getExpenseByUserIdOrThrow(EXPENSE_ID, USER_ID)).thenReturn(existing);
                when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(limits);
                when(limitSpendingService.calculateSpent(eq(USER_ID), eq(limits), any(LocalDate.class)))
                        .thenReturn(Map.of(LIMIT_ID, BigDecimal.valueOf(1000)));
                when(limitCalculateService.calculateLimitStats(eq(limits), eq(USER_ID), any(LocalDate.class))).thenReturn(List.of());

                Long result = service.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest);

                assertEquals(EXPENSE_ID, result);
                assertEquals(AMOUNT, existing.getAmount());
            }

            @Test
            void shouldPublishLimitStatsEventsWhenActiveLimitsExist() {
                List<Limit> limits = List.of(generalLimit);
                when(expenseManagerService.getExpenseByUserIdOrThrow(EXPENSE_ID, USER_ID)).thenReturn(existing);
                when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(limits);
                when(limitSpendingService.calculateSpent(eq(USER_ID), eq(limits), any(LocalDate.class)))
                        .thenReturn(Map.of(LIMIT_ID, BigDecimal.valueOf(20)));
                when(limitCalculateService.calculateLimitStats(eq(limits), eq(USER_ID), any(LocalDate.class))).thenReturn(List.of(statsDto));
                when(statsDto.limitId()).thenReturn(LIMIT_ID);
                when(statsDto.percentage()).thenReturn(BigDecimal.valueOf(20));
                when(statsDto.periodType()).thenReturn(PERIOD);

                service.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest);

                verify(outboxService).save(eq(LIMIT_AGGREGATE), eq(LIMIT_ID.toString()), eq(LIMIT_STATS_TOPIC), any(LimitStatsEvent.class));
            }

            @Test
            void shouldThrowExceptionWhenExpenseDoesNotExist() {
                when(expenseManagerService.getExpenseByUserIdOrThrow(EXPENSE_ID, USER_ID)).thenThrow(new IllegalStateException());

                assertThrows(IllegalStateException.class, () -> service.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest));
                verify(walletService, never()).removeBalanceFromWallet(any(), any());
                verifyNoInteractions(limitRepository, roundUpService, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenRiskGuardRejectsExpense() {
                doThrow(new IllegalStateException()).when(riskGuardService)
                        .guard(any(), any(), any(), any(), any(), any(), any());

                assertThrows(IllegalStateException.class, () -> service.editExpense(requestDto, USER_ID, EXPENSE_ID, servletRequest));
                verifyNoInteractions(walletService, expenseManagerService, outboxService);
            }
        }
    }

    @Nested
    class DeleteExpense {

        private Expense expense;

        @BeforeEach
        void setUp() {
            expense = Expense.builder().id(EXPENSE_ID).amount(AMOUNT).category(CATEGORY).userId(USER_ID).build();
        }

        @Test
        void shouldDeleteExpenseAndRestoreBalanceWhenExpenseExists() {
            when(expenseManagerService.getExpenseByUserIdOrThrow(EXPENSE_ID, USER_ID)).thenReturn(expense);
            when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of());

            service.deleteExpense(EXPENSE_ID, USER_ID, AUTH_CODE);

            verify(additionalAuthorizationCodeResolver).resolve(AUTH_CODE);
            verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
            verify(walletService).lockWalletForUpdate(USER_ID);
            verify(roundUpService).handleExpenseForRoundUp(USER_ID, EXPENSE_ID, PiggyBankAutomationMode.ROLLBACK);
            verify(walletService).addBalanceToWallet(USER_ID, AMOUNT);
            verify(outboxService).save(eq(EXPENSE_AGGREGATE), eq(EXPENSE_ID.toString()), eq(EXPENSE_TOPIC), any(ExpenseActivityEvent.class));
            verify(expenseRepository).delete(expense);
        }

        @Test
        void shouldNotPublishLimitStatsWhenNoActiveLimits() {
            when(expenseManagerService.getExpenseByUserIdOrThrow(EXPENSE_ID, USER_ID)).thenReturn(expense);
            when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of());

            service.deleteExpense(EXPENSE_ID, USER_ID, AUTH_CODE);

            verifyNoInteractions(limitCalculateService);
        }

        @Test
        void shouldPublishLimitStatsEventsWhenActiveLimitsExist() {
            Limit limit = Limit.builder().id(LIMIT_ID).periodType(PERIOD).category(null).amount(LIMIT_AMOUNT).build();
            List<Limit> limits = List.of(limit);
            when(expenseManagerService.getExpenseByUserIdOrThrow(EXPENSE_ID, USER_ID)).thenReturn(expense);
            when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(limits);
            when(limitCalculateService.calculateLimitStats(eq(limits), eq(USER_ID), any(LocalDate.class))).thenReturn(List.of(statsDto));
            when(statsDto.limitId()).thenReturn(LIMIT_ID);
            when(statsDto.percentage()).thenReturn(BigDecimal.valueOf(10));
            when(statsDto.periodType()).thenReturn(PERIOD);

            service.deleteExpense(EXPENSE_ID, USER_ID, AUTH_CODE);

            verify(outboxService).save(eq(LIMIT_AGGREGATE), eq(LIMIT_ID.toString()), eq(LIMIT_STATS_TOPIC), any(LimitStatsEvent.class));
        }

        @Test
        void shouldThrowExceptionWhenExpenseDoesNotExist() {
            when(expenseManagerService.getExpenseByUserIdOrThrow(EXPENSE_ID, USER_ID)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> service.deleteExpense(EXPENSE_ID, USER_ID, AUTH_CODE));
            verify(walletService, never()).addBalanceToWallet(any(), any());
            verifyNoInteractions(roundUpService, outboxService, expenseRepository, limitRepository);
        }

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeIsInvalid() {
            doThrow(new IllegalStateException()).when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class, () -> service.deleteExpense(EXPENSE_ID, USER_ID, AUTH_CODE));
            verifyNoInteractions(walletService, expenseManagerService, roundUpService, outboxService, expenseRepository);
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteAllExpensesWhenUserHasExpenses() {
            Expense expense = Expense.builder().id(EXPENSE_ID).amount(AMOUNT).category(CATEGORY).userId(USER_ID).build();
            when(expenseRepository.findAllByUserId(USER_ID)).thenReturn(List.of(expense));

            service.deleteByUserId(USER_ID);

            verify(expenseRepository).deleteAll(expensesCaptor.capture());
            assertEquals(List.of(expense), expensesCaptor.getValue());
        }

        @Test
        void shouldDeleteEmptyListWhenUserHasNoExpenses() {
            when(expenseRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

            service.deleteByUserId(USER_ID);

            verify(expenseRepository).deleteAll(expensesCaptor.capture());
            assertTrue(expensesCaptor.getValue().isEmpty());
        }
    }
}