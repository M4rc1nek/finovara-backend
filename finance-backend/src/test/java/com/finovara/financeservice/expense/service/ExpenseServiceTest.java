package com.finovara.financeservice.expense.service;

import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.authorization.dto.ConfirmAuthorizationCodeDto;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.expense.dto.ExpenseDto;
import com.finovara.financeservice.expense.dto.ExpenseRequestDto;
import com.finovara.financeservice.expense.mapper.ExpenseMapper;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.limit.repository.LimitRepository;
import com.finovara.financeservice.limit.service.LimitCalculateService;
import com.finovara.financeservice.limit.dto.LimitStatsDto;
import com.finovara.financeservice.settings.finances.expense.controlamount.service.ControlAmountService;
import com.finovara.financeservice.settings.finances.expense.quantitylimit.dto.CountQuantityLimitDto;
import com.finovara.financeservice.settings.finances.expense.quantitylimit.service.CountQuantityLimitService;
import com.finovara.financeservice.settings.finances.expense.smartscan.dto.SmartScanMode;
import com.finovara.financeservice.settings.finances.expense.smartscan.service.SmartScanService;
import com.finovara.financeservice.settings.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.financeservice.settings.piggybank.roundup.service.RoundUpService;
import com.finovara.financeservice.util.transaction.TransactionOrigin;
import com.finovara.financeservice.util.transaction.expense.ExpenseManagerService;
import com.finovara.financeservice.util.periodbalance.FinancialPeriodService;
import com.finovara.financeservice.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

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

    private ExpenseService expenseService;

    private Long userId;
    private ExpenseCategory category;
    private ExpenseCategory otherCategory;
    private PeriodType periodType;

    @BeforeEach
    void setUp() {
        expenseService = new ExpenseService(
                kafkaTemplate,
                outboxService,
                expenseRepository,
                limitRepository,
                limitCalculateService,
                walletService,
                roundUpService,
                countQuantityLimitService,
                controlAmountService,
                smartScanService,
                expenseManagerService,
                expenseMapper,
                financialPeriodService,
                authBackendClient,
                new AdditionalAuthorizationCodeResolver()
        );

        userId = 1L;
        category = ExpenseCategory.values()[0];
        otherCategory = ExpenseCategory.values().length > 1 ? ExpenseCategory.values()[1] : ExpenseCategory.values()[0];
        periodType = PeriodType.values()[0];
    }

    private ExpenseRequestDto buildRequestDto(BigDecimal amount, ExpenseCategory expenseCategory) {
        ExpenseDto expenseDto = new ExpenseDto(null, userId, amount, expenseCategory, LocalDate.now(), "description");
        return new ExpenseRequestDto(expenseDto, mock(ConfirmPasswordDto.class), mock(ConfirmAuthorizationCodeDto.class), mock(CountQuantityLimitDto.class));
    }

    private Limit buildLimit(Long id, ExpenseCategory limitCategory, BigDecimal amount) {
        return Limit.builder()
                .id(id)
                .periodType(periodType)
                .category(limitCategory)
                .amount(amount)
                .isActive(true)
                .userId(userId)
                .build();
    }

    private Expense buildExistingExpense(Long id, Long ownerId, BigDecimal amount, ExpenseCategory expenseCategory) {
        return Expense.builder()
                .id(id)
                .amount(amount)
                .category(expenseCategory)
                .createdAt(LocalDate.now())
                .description("old description")
                .userId(ownerId)
                .build();
    }

    @Nested
    class AddExpenseTests {

        @Test
        void shouldAddExpenseAndReturnIdWhenValidRequest() {
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(100), category);
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
                Expense expense = invocation.getArgument(0);
                expense.setId(10L);
                return expense;
            });

            Long result = expenseService.addExpense(requestDto, userId, TransactionOrigin.USER_MANUAL);

            assertEquals(10L, result);
        }

        @Test
        void shouldRemoveBalanceFromWalletWhenAddingExpense() {
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(50), category);
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
                Expense expense = invocation.getArgument(0);
                expense.setId(5L);
                return expense;
            });

            expenseService.addExpense(requestDto, userId, TransactionOrigin.USER_MANUAL);

            verify(walletService).removeBalanceFromWallet(userId, BigDecimal.valueOf(50));
        }

        @Test
        void shouldSaveExpenseWithCurrentDateWhenAddingExpense() {
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(50), category);
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
                Expense expense = invocation.getArgument(0);
                expense.setId(5L);
                return expense;
            });

            expenseService.addExpense(requestDto, userId, TransactionOrigin.USER_MANUAL);

            verify(expenseRepository).save(any(Expense.class));
        }

        @Test
        void shouldCallSmartScanServiceWithAddModeWhenAddingExpense() {
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(50), category);
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
                Expense expense = invocation.getArgument(0);
                expense.setId(5L);
                return expense;
            });

            expenseService.addExpense(requestDto, userId, TransactionOrigin.USER_MANUAL);

            verify(smartScanService).handleSmartScan(userId, requestDto.confirmPasswordDto(), BigDecimal.valueOf(50), SmartScanMode.ADD);
        }

        @Test
        void shouldHandleRoundUpWithApplyModeWhenAddingExpense() {
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(50), category);
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
                Expense expense = invocation.getArgument(0);
                expense.setId(7L);
                return expense;
            });

            expenseService.addExpense(requestDto, userId, TransactionOrigin.USER_MANUAL);

            verify(roundUpService).handleExpenseForRoundUp(userId, 7L, PiggyBankAutomationMode.APPLY);
        }

        @Test
        void shouldCallControlAmountServiceWhenAddingExpense() {
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(50), category);
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
                Expense expense = invocation.getArgument(0);
                expense.setId(7L);
                return expense;
            });

            expenseService.addExpense(requestDto, userId, TransactionOrigin.USER_MANUAL);

            verify(controlAmountService).handleExpenseAmountControl(userId, BigDecimal.valueOf(50));
        }

        @Test
        void shouldCallCountQuantityLimitServiceWhenAddingExpense() {
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(50), category);
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
                Expense expense = invocation.getArgument(0);
                expense.setId(7L);
                return expense;
            });

            expenseService.addExpense(requestDto, userId, TransactionOrigin.USER_MANUAL);

            verify(countQuantityLimitService).handleExpenseLimitExceeded(userId, requestDto.countQuantityLimitDto(),
                    requestDto.countQuantityLimitDto().periodType(), requestDto.confirmPasswordDto());
        }

        @Test
        void shouldPublishLimitStatsEventForEachActiveLimitWhenAddingExpense() {
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(50), category);
            Limit firstLimit = buildLimit(1L, category, BigDecimal.valueOf(1000));
            Limit secondLimit = buildLimit(2L, null, BigDecimal.valueOf(2000));
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of(firstLimit, secondLimit));
            when(financialPeriodService.getExpensesSum(eq(userId), any(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
                Expense expense = invocation.getArgument(0);
                expense.setId(7L);
                return expense;
            });
            LimitStatsDto firstStats = new LimitStatsDto(1L, periodType, category, BigDecimal.valueOf(1000), BigDecimal.valueOf(50), BigDecimal.valueOf(950), BigDecimal.valueOf(5), null, LocalDate.now());
            LimitStatsDto secondStats = new LimitStatsDto(2L, periodType, null, BigDecimal.valueOf(2000), BigDecimal.valueOf(50), BigDecimal.valueOf(1950), BigDecimal.valueOf(2), null, LocalDate.now());
            when(limitCalculateService.calculateLimitStats(eq(firstLimit), eq(userId), any(LocalDate.class))).thenReturn(firstStats);
            when(limitCalculateService.calculateLimitStats(eq(secondLimit), eq(userId), any(LocalDate.class))).thenReturn(secondStats);

            expenseService.addExpense(requestDto, userId, TransactionOrigin.USER_MANUAL);

            verify(kafkaTemplate, times(2)).send(eq("limit.calculate-stats"), any());
        }

        @Test
        void shouldThrowExceptionWhenAmountIsLessThanOne() {
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(0.5), category);
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());

            assertThrows(InvalidInputException.class, () -> expenseService.addExpense(requestDto, userId, TransactionOrigin.USER_MANUAL));

            verify(walletService, never()).removeBalanceFromWallet(anyLong(), any());
            verify(expenseRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenGeneralLimitExceeded() {
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(500), category);
            Limit generalLimit = buildLimit(1L, null, BigDecimal.valueOf(400));
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of(generalLimit));
            when(financialPeriodService.getExpensesSum(userId, periodType, null)).thenReturn(BigDecimal.ZERO);

            assertThrows(MissingRequirementException.class, () -> expenseService.addExpense(requestDto, userId, TransactionOrigin.USER_MANUAL));

            verify(walletService, never()).removeBalanceFromWallet(anyLong(), any());
            verify(expenseRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenCategoryLimitExceeded() {
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(500), category);
            Limit categoryLimit = buildLimit(1L, category, BigDecimal.valueOf(400));
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of(categoryLimit));
            when(financialPeriodService.getExpensesSum(userId, periodType, category)).thenReturn(BigDecimal.ZERO);

            assertThrows(MissingRequirementException.class, () -> expenseService.addExpense(requestDto, userId, TransactionOrigin.USER_MANUAL));

            verify(walletService, never()).removeBalanceFromWallet(anyLong(), any());
        }

        @Test
        void shouldSaveOutboxEventWhenAddingExpense() {
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(50), category);
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
                Expense expense = invocation.getArgument(0);
                expense.setId(7L);
                return expense;
            });

            expenseService.addExpense(requestDto, userId, TransactionOrigin.USER_MANUAL);

            verify(outboxService).save(eq("Expense"), eq("7"), eq("activity.expense"), any());
        }

        @Test
        void shouldConfirmAuthorizationCodeWhenOriginIsUserManual() {
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(50), category);
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
                Expense expense = invocation.getArgument(0);
                expense.setId(7L);
                return expense;
            });

            expenseService.addExpense(requestDto, userId, TransactionOrigin.USER_MANUAL);

            verify(authBackendClient).confirmAuthorizationCode(eq(userId), any());
        }

        @Test
        void shouldNotConfirmAuthorizationCodeWhenOriginIsNotUserManual() {
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(50), category);
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
                Expense expense = invocation.getArgument(0);
                expense.setId(7L);
                return expense;
            });

            expenseService.addExpense(requestDto, userId, TransactionOrigin.RECURRING_SYSTEM);

            verifyNoInteractions(authBackendClient);
        }
    }

    @Nested
    class EditExpenseTests {

        @Test
        void shouldEditExpenseAndReturnIdWhenValidRequest() {
            Long expenseId = 3L;
            Expense existingExpense = buildExistingExpense(expenseId, userId, BigDecimal.valueOf(100), category);
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(200), category);
            when(expenseManagerService.getExpenseByIdOrThrow(expenseId)).thenReturn(existingExpense);
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());

            Long result = expenseService.editExpense(requestDto, userId, expenseId);

            assertEquals(expenseId, result);
        }

        @Test
        void shouldThrowExceptionWhenExpenseNotFoundEntirely() {
            Long expenseId = 3L;
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(200), category);
            doThrow(new RequestedEntityNotFoundException("Expense not found"))
                    .when(expenseManagerService).getExpenseByIdOrThrow(expenseId);

            assertThrows(RequestedEntityNotFoundException.class, () -> expenseService.editExpense(requestDto, userId, expenseId));

            verifyNoInteractions(walletService);
        }

        @Test
        void shouldThrowExceptionWhenExpenseBelongsToDifferentUser() {
            Long expenseId = 3L;
            Expense existingExpense = buildExistingExpense(expenseId, 999L, BigDecimal.valueOf(100), category);
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(200), category);
            when(expenseManagerService.getExpenseByIdOrThrow(expenseId)).thenReturn(existingExpense);

            assertThrows(RequestedEntityNotFoundException.class, () -> expenseService.editExpense(requestDto, userId, expenseId));

            verifyNoInteractions(walletService);
            verify(expenseRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenLimitExceededOnEdit() {
            Long expenseId = 3L;
            Expense existingExpense = buildExistingExpense(expenseId, userId, BigDecimal.valueOf(100), category);
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(900), category);
            Limit generalLimit = buildLimit(1L, null, BigDecimal.valueOf(400));
            when(expenseManagerService.getExpenseByIdOrThrow(expenseId)).thenReturn(existingExpense);
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of(generalLimit));
            when(financialPeriodService.getExpensesSum(userId, periodType, null)).thenReturn(BigDecimal.ZERO);

            assertThrows(MissingRequirementException.class, () -> expenseService.editExpense(requestDto, userId, expenseId));

            verifyNoInteractions(walletService);
            verify(expenseRepository, never()).save(any());
        }

        @Test
        void shouldAdjustWalletBalanceWhenEditingExpense() {
            Long expenseId = 3L;
            Expense existingExpense = buildExistingExpense(expenseId, userId, BigDecimal.valueOf(100), category);
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(250), category);
            when(expenseManagerService.getExpenseByIdOrThrow(expenseId)).thenReturn(existingExpense);
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());

            expenseService.editExpense(requestDto, userId, expenseId);

            verify(walletService).addBalanceToWallet(userId, BigDecimal.valueOf(100));
            verify(walletService).removeBalanceFromWallet(userId, BigDecimal.valueOf(250));
        }

        @Test
        void shouldRollbackThenApplyRoundUpWhenEditingExpense() {
            Long expenseId = 3L;
            Expense existingExpense = buildExistingExpense(expenseId, userId, BigDecimal.valueOf(100), category);
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(250), category);
            when(expenseManagerService.getExpenseByIdOrThrow(expenseId)).thenReturn(existingExpense);
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());

            expenseService.editExpense(requestDto, userId, expenseId);

            InOrder inOrder = inOrder(roundUpService);
            inOrder.verify(roundUpService).handleExpenseForRoundUp(userId, expenseId, PiggyBankAutomationMode.ROLLBACK);
            inOrder.verify(roundUpService).handleExpenseForRoundUp(userId, expenseId, PiggyBankAutomationMode.APPLY);
        }

        @Test
        void shouldUpdateExpenseFieldsWhenEditingExpense() {
            Long expenseId = 3L;
            Expense existingExpense = buildExistingExpense(expenseId, userId, BigDecimal.valueOf(100), category);
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(250), otherCategory);
            when(expenseManagerService.getExpenseByIdOrThrow(expenseId)).thenReturn(existingExpense);
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());

            expenseService.editExpense(requestDto, userId, expenseId);

            assertEquals(BigDecimal.valueOf(250), existingExpense.getAmount());
            assertEquals(otherCategory, existingExpense.getCategory());
            assertEquals("description", existingExpense.getDescription());
        }

        @Test
        void shouldCallSmartScanServiceWithEditModeWhenEditingExpense() {
            Long expenseId = 3L;
            Expense existingExpense = buildExistingExpense(expenseId, userId, BigDecimal.valueOf(100), category);
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(250), category);
            when(expenseManagerService.getExpenseByIdOrThrow(expenseId)).thenReturn(existingExpense);
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());

            expenseService.editExpense(requestDto, userId, expenseId);

            verify(smartScanService).handleSmartScan(userId, requestDto.confirmPasswordDto(), BigDecimal.valueOf(250), SmartScanMode.EDIT);
        }

        @Test
        void shouldCallControlAmountServiceWhenEditingExpense() {
            Long expenseId = 3L;
            Expense existingExpense = buildExistingExpense(expenseId, userId, BigDecimal.valueOf(100), category);
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(250), category);
            when(expenseManagerService.getExpenseByIdOrThrow(expenseId)).thenReturn(existingExpense);
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());

            expenseService.editExpense(requestDto, userId, expenseId);

            verify(controlAmountService).handleExpenseAmountControl(userId, BigDecimal.valueOf(250));
        }

        @Test
        void shouldSaveOutboxEventWhenEditingExpense() {
            Long expenseId = 3L;
            Expense existingExpense = buildExistingExpense(expenseId, userId, BigDecimal.valueOf(100), category);
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(250), category);
            when(expenseManagerService.getExpenseByIdOrThrow(expenseId)).thenReturn(existingExpense);
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());

            expenseService.editExpense(requestDto, userId, expenseId);

            verify(outboxService).save(eq("Expense"), eq(expenseId.toString()), eq("activity.expense"), any());
        }

        @Test
        void shouldPublishLimitStatsEventsWhenEditingExpense() {
            Long expenseId = 3L;
            Expense existingExpense = buildExistingExpense(expenseId, userId, BigDecimal.valueOf(100), category);
            ExpenseRequestDto requestDto = buildRequestDto(BigDecimal.valueOf(250), category);
            Limit limit = buildLimit(1L, null, BigDecimal.valueOf(1000));
            when(expenseManagerService.getExpenseByIdOrThrow(expenseId)).thenReturn(existingExpense);
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of(limit));
            when(financialPeriodService.getExpensesSum(userId, periodType, null)).thenReturn(BigDecimal.ZERO);
            LimitStatsDto stats = new LimitStatsDto(1L, periodType, null, BigDecimal.valueOf(1000), BigDecimal.valueOf(250), BigDecimal.valueOf(750), BigDecimal.valueOf(25), null, LocalDate.now());
            when(limitCalculateService.calculateLimitStats(eq(limit), eq(userId), any(LocalDate.class))).thenReturn(stats);

            expenseService.editExpense(requestDto, userId, expenseId);

            verify(kafkaTemplate).send(eq("limit.calculate-stats"), any());
        }
    }

    @Nested
    class GetExpenseTests {

        @Test
        void shouldReturnMappedExpenseDtosWhenExpensesExist() {
            Expense firstExpense = buildExistingExpense(1L, userId, BigDecimal.valueOf(50), category);
            Expense secondExpense = buildExistingExpense(2L, userId, BigDecimal.valueOf(75), otherCategory);
            ExpenseDto firstDto = new ExpenseDto(1L, userId, BigDecimal.valueOf(50), category, LocalDate.now(), "d1");
            ExpenseDto secondDto = new ExpenseDto(2L, userId, BigDecimal.valueOf(75), otherCategory, LocalDate.now(), "d2");
            when(expenseRepository.findAllByUserId(userId)).thenReturn(List.of(firstExpense, secondExpense));
            when(expenseMapper.mapExpenseToDto(firstExpense)).thenReturn(firstDto);
            when(expenseMapper.mapExpenseToDto(secondExpense)).thenReturn(secondDto);

            List<ExpenseDto> result = expenseService.getExpense(userId);

            assertEquals(List.of(firstDto, secondDto), result);
        }

        @Test
        void shouldReturnEmptyListWhenNoExpensesExist() {
            when(expenseRepository.findAllByUserId(userId)).thenReturn(List.of());

            List<ExpenseDto> result = expenseService.getExpense(userId);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class DeleteExpenseTests {

        @Test
        void shouldDeleteExpenseWhenExpenseExists() {
            Long expenseId = 4L;
            Expense expense = buildExistingExpense(expenseId, userId, BigDecimal.valueOf(80), category);
            when(expenseRepository.findByIdAndUserId(expenseId, userId)).thenReturn(java.util.Optional.of(expense));
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());

            expenseService.deleteExpense(expenseId, userId, "authCode");

            verify(expenseRepository).delete(expense);
        }

        @Test
        void shouldThrowExceptionWhenExpenseNotFoundOnDelete() {
            Long expenseId = 4L;
            when(expenseRepository.findByIdAndUserId(expenseId, userId)).thenReturn(java.util.Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> expenseService.deleteExpense(expenseId, userId, "authCode"));

            verify(expenseRepository, never()).delete(any());
            verifyNoInteractions(walletService);
        }

        @Test
        void shouldRollbackRoundUpWhenDeletingExpense() {
            Long expenseId = 4L;
            Expense expense = buildExistingExpense(expenseId, userId, BigDecimal.valueOf(80), category);
            when(expenseRepository.findByIdAndUserId(expenseId, userId)).thenReturn(java.util.Optional.of(expense));
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());

            expenseService.deleteExpense(expenseId, userId, "authCode");

            verify(roundUpService).handleExpenseForRoundUp(userId, expenseId, PiggyBankAutomationMode.ROLLBACK);
        }

        @Test
        void shouldAddBalanceBackToWalletWhenDeletingExpense() {
            Long expenseId = 4L;
            Expense expense = buildExistingExpense(expenseId, userId, BigDecimal.valueOf(80), category);
            when(expenseRepository.findByIdAndUserId(expenseId, userId)).thenReturn(java.util.Optional.of(expense));
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());

            expenseService.deleteExpense(expenseId, userId, "authCode");

            verify(walletService).addBalanceToWallet(userId, BigDecimal.valueOf(80));
        }

        @Test
        void shouldSaveOutboxEventWhenDeletingExpense() {
            Long expenseId = 4L;
            Expense expense = buildExistingExpense(expenseId, userId, BigDecimal.valueOf(80), category);
            when(expenseRepository.findByIdAndUserId(expenseId, userId)).thenReturn(java.util.Optional.of(expense));
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());

            expenseService.deleteExpense(expenseId, userId, "authCode");

            verify(outboxService).save(eq("Expense"), eq(expenseId.toString()), eq("activity.expense"), any());
        }

        @Test
        void shouldPublishLimitStatsEventsWhenDeletingExpense() {
            Long expenseId = 4L;
            Expense expense = buildExistingExpense(expenseId, userId, BigDecimal.valueOf(80), category);
            Limit limit = buildLimit(1L, null, BigDecimal.valueOf(500));
            when(expenseRepository.findByIdAndUserId(expenseId, userId)).thenReturn(java.util.Optional.of(expense));
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of(limit));
            LimitStatsDto stats = new LimitStatsDto(1L, periodType, null, BigDecimal.valueOf(500), BigDecimal.ZERO, BigDecimal.valueOf(500), BigDecimal.ZERO, null, LocalDate.now());
            when(limitCalculateService.calculateLimitStats(eq(limit), eq(userId), any(LocalDate.class))).thenReturn(stats);

            expenseService.deleteExpense(expenseId, userId, "authCode");

            verify(kafkaTemplate).send(eq("limit.calculate-stats"), any());
        }
    }

    @Nested
    class DeleteByUserIdTests {

        @Test
        void shouldDeleteAllExpensesForUserWhenCalled() {
            expenseService.deleteByUserId(userId);

            verify(expenseRepository).deleteByUserId(userId);
        }
    }
}