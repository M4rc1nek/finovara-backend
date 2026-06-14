package com.finovara.authbackend.expense.service;

import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import com.finovara.contracts.event.activity.expense.ExpenseActivityEvent;
import com.finovara.contracts.event.notification.limit.LimitStatsEvent;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.activity.ExpenseActivityType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.authbackend.expense.dto.ExpenseDto;
import com.finovara.authbackend.expense.dto.ExpenseRequestDto;
import com.finovara.authbackend.expense.mapper.ExpenseMapper;
import com.finovara.authbackend.expense.model.Expense;
import com.finovara.authbackend.expense.repository.ExpenseRepository;
import com.finovara.authbackend.limit.dto.LimitStatsDto;
import com.finovara.authbackend.limit.model.Limit;
import com.finovara.authbackend.limit.model.LimitStatus;
import com.finovara.authbackend.limit.repository.LimitRepository;
import com.finovara.authbackend.limit.service.LimitCalculateService;
import com.finovara.authbackend.usersetting.finances.expense.controlamount.service.ControlAmountService;
import com.finovara.authbackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.authbackend.usersetting.finances.expense.countlimit.service.CountQuantityLimitService;
import com.finovara.authbackend.usersetting.finances.expense.smartscan.dto.SmartScanMode;
import com.finovara.authbackend.usersetting.finances.expense.smartscan.service.SmartScanService;
import com.finovara.authbackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.authbackend.usersetting.piggybank.roundup.service.RoundUpService;
import com.finovara.authbackend.util.expense.ExpenseManagerService;
import com.finovara.authbackend.util.periodbalance.FinancialPeriodService;
import com.finovara.authbackend.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @InjectMocks
    private ExpenseService expenseService;

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
    private KafkaTemplate<String, Object> kafkaTemplate;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
    }

    private LimitStatsDto buildLimitStats(Long limitId, double percentage, PeriodType periodType) {
        return new LimitStatsDto(limitId, periodType, new BigDecimal("100"), new BigDecimal("50"),
                new BigDecimal("50"), BigDecimal.valueOf(percentage), LimitStatus.NONE, LocalDate.now());
    }

    private Expense buildExpense(Long id, BigDecimal amount, ExpenseCategory category) {
        Expense expense = new Expense();
        expense.setId(id);
        expense.setAmount(amount);
        expense.setCategory(category);
        expense.setUserId(userId);
        return expense;
    }

    private void stubSuccessfulAdd(BigDecimal amount) {
        when(limitRepository.getLimitAmountByUserIdAndType(anyLong(), any())).thenReturn(Optional.empty());
        when(financialPeriodService.getExpensesSum(anyLong(), any())).thenReturn(BigDecimal.ZERO);
        when(expenseRepository.save(any())).thenAnswer(inv -> {
            Expense e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });
        when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());
    }

    private void stubSuccessfulEdit(Expense existing) {
        when(expenseManagerService.getExpenseByIdOrThrow(existing.getId())).thenReturn(existing);
        when(limitRepository.getLimitAmountByUserIdAndType(anyLong(), any())).thenReturn(Optional.empty());
        when(financialPeriodService.getExpensesSum(anyLong(), any())).thenReturn(BigDecimal.ZERO);
        when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());
    }

    @Nested
    class AddExpenseTests {

        @Test
        void shouldAddExpenseSuccessfully() {

            BigDecimal amount = new BigDecimal("100");
            ExpenseRequestDto request = buildAddRequest(amount, ExpenseCategory.FOOD, "test");
            stubSuccessfulAdd(amount);

            Long result = expenseService.addExpense(request, userId, PeriodType.DAILY);

            assertEquals(1L, result);
            verify(walletService).removeBalanceFromWallet(userId, amount);
            verify(expenseRepository).save(any());
            verify(smartScanService).handleSmartScan(eq(userId), any(), eq(amount), eq(SmartScanMode.ADD));
            verify(roundUpService).handleExpenseForRoundUp(eq(userId), anyLong(), eq(PiggyBankAutomationMode.APPLY));
            verify(controlAmountService).handleExpenseAmountControl(userId, amount);

            ArgumentCaptor<ExpenseActivityEvent> captor = ArgumentCaptor.forClass(ExpenseActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.expense"), captor.capture());
            assertEquals(ExpenseActivityType.ADDED_EXPENSE, captor.getValue().type());
        }

        @Test
        void shouldThrowAmountLessThanOne() {

            ExpenseRequestDto request = buildAddRequest(new BigDecimal("0.50"), ExpenseCategory.FOOD, "test");
            when(limitRepository.getLimitAmountByUserIdAndType(anyLong(), any())).thenReturn(Optional.empty());
            when(financialPeriodService.getExpensesSum(anyLong(), any())).thenReturn(BigDecimal.ZERO);

            assertThrows(InvalidInputException.class, () -> expenseService.addExpense(request, userId, PeriodType.DAILY));

            verify(walletService, never()).removeBalanceFromWallet(anyLong(), any());
            verify(expenseRepository, never()).save(any());
            verify(smartScanService, never()).handleSmartScan(anyLong(), any(), any(), any());
        }

        @Test
        void shouldThrowLimitExceeded() {
            ExpenseRequestDto request = buildAddRequest(new BigDecimal("200"), ExpenseCategory.FOOD, "test");
            when(limitRepository.getLimitAmountByUserIdAndType(anyLong(), any()))
                    .thenReturn(Optional.of(new BigDecimal("100")));
            when(financialPeriodService.getExpensesSum(anyLong(), any())).thenReturn(new BigDecimal("50"));

            assertThrows(MissingRequirementException.class,
                    () -> expenseService.addExpense(request, userId, PeriodType.DAILY));

            verifyNoInteractions(walletService, expenseRepository, smartScanService);
        }

        @Test
        void shouldPublishLimitStatsEventForEachLimitOnAdd() {

            ExpenseRequestDto request = buildAddRequest(new BigDecimal("100"), ExpenseCategory.FOOD, "test");
            Limit limit1 = mock(Limit.class);
            Limit limit2 = mock(Limit.class);
            when(limitRepository.getLimitAmountByUserIdAndType(anyLong(), any())).thenReturn(Optional.empty());
            when(financialPeriodService.getExpensesSum(anyLong(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseRepository.save(any())).thenAnswer(inv -> {
                Expense e = inv.getArgument(0);
                e.setId(1L);
                return e;
            });
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of(limit1, limit2));
            when(limitCalculateService.calculateLimitStats(eq(limit1), eq(userId), any(LocalDate.class)))
                    .thenReturn(buildLimitStats(1L, 30.0, PeriodType.DAILY));
            when(limitCalculateService.calculateLimitStats(eq(limit2), eq(userId), any(LocalDate.class)))
                    .thenReturn(buildLimitStats(2L, 70.0, PeriodType.MONTHLY));

            expenseService.addExpense(request, userId, PeriodType.DAILY);

            verify(kafkaTemplate, times(2)).send(eq("limit.calculate-stats"), any(LimitStatsEvent.class));
        }

        @Test
        void shouldPublishCorrectLimitStatsDataOnAdd() {

            ExpenseRequestDto request = buildAddRequest(new BigDecimal("100"), ExpenseCategory.FOOD, "test");
            Limit limit = mock(Limit.class);
            when(limitRepository.getLimitAmountByUserIdAndType(anyLong(), any())).thenReturn(Optional.empty());
            when(financialPeriodService.getExpensesSum(anyLong(), any())).thenReturn(BigDecimal.ZERO);
            when(expenseRepository.save(any())).thenAnswer(inv -> {
                Expense e = inv.getArgument(0);
                e.setId(1L);
                return e;
            });
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of(limit));
            when(limitCalculateService.calculateLimitStats(eq(limit), eq(userId), any(LocalDate.class)))
                    .thenReturn(buildLimitStats(5L, 80.0, PeriodType.DAILY));

            expenseService.addExpense(request, userId, PeriodType.DAILY);

            ArgumentCaptor<LimitStatsEvent> captor = ArgumentCaptor.forClass(LimitStatsEvent.class);
            verify(kafkaTemplate).send(eq("limit.calculate-stats"), captor.capture());
            assertEquals(userId, captor.getValue().userId());
            assertEquals(5L, captor.getValue().limitId());
            assertEquals(PeriodType.DAILY, captor.getValue().periodType());
        }

        @Test
        void shouldNotPublishLimitStatsNoLimitsExistOnAdd() {

            ExpenseRequestDto request = buildAddRequest(new BigDecimal("100"), ExpenseCategory.FOOD, "test");
            stubSuccessfulAdd(new BigDecimal("100"));

            expenseService.addExpense(request, userId, PeriodType.DAILY);

            verify(kafkaTemplate, never()).send(eq("limit.calculate-stats"), any());
        }

        private ExpenseRequestDto buildAddRequest(BigDecimal amount, ExpenseCategory category, String description) {
            ExpenseDto dto = new ExpenseDto(null, null, amount, category, null, description);
            CountQuantityLimitDto countQuantityLimitDto = new CountQuantityLimitDto(false, PeriodType.DAILY, 10);
            return new ExpenseRequestDto(dto, new ConfirmPasswordDto("pass"), countQuantityLimitDto);
        }
    }

    @Nested
    class EditExpenseTests {

        @Test
        void shouldEditExpenseSuccessfully() {

            Expense existing = buildExpense(1L, new BigDecimal("100"), ExpenseCategory.FOOD);
            ExpenseRequestDto request = buildEditRequest(new BigDecimal("200"), ExpenseCategory.SAVINGS, "new");
            stubSuccessfulEdit(existing);

            expenseService.editExpense(request, userId, 1L, PeriodType.DAILY);

            verify(walletService).addBalanceToWallet(userId, new BigDecimal("100"));
            verify(walletService).removeBalanceFromWallet(userId, new BigDecimal("200"));
            verify(expenseRepository).save(existing);

            ArgumentCaptor<ExpenseActivityEvent> captor = ArgumentCaptor.forClass(ExpenseActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.expense"), captor.capture());
            assertEquals(ExpenseActivityType.EDITED_EXPENSE, captor.getValue().type());
        }

        @Test
        void shouldThrowExpenseNotFound() {
            when(expenseManagerService.getExpenseByIdOrThrow(anyLong()))
                    .thenThrow(new RequestedEntityNotFoundException("Expense not found"));

            ExpenseRequestDto request = buildEditRequest(new BigDecimal("200"), ExpenseCategory.FOOD, "x");

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> expenseService.editExpense(request, userId, 1L, PeriodType.DAILY));

            verifyNoInteractions(walletService, expenseRepository);
        }

        @Test
        void shouldThrowExpenseBelongsToAnotherUser() {

            Expense existing = buildExpense(1L, new BigDecimal("100"), ExpenseCategory.FOOD);
            existing.setUserId(99L);

            when(expenseManagerService.getExpenseByIdOrThrow(1L)).thenReturn(existing);
            ExpenseRequestDto request = buildEditRequest(new BigDecimal("200"), ExpenseCategory.FOOD, "x");

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> expenseService.editExpense(request, userId, 1L, PeriodType.DAILY));

            verifyNoInteractions(walletService, expenseRepository);
        }

        @Test
        void shouldPublishLimitStatsEventForEachLimitOnEdit() {

            Expense existing = buildExpense(1L, new BigDecimal("100"), ExpenseCategory.FOOD);
            ExpenseRequestDto request = buildEditRequest(new BigDecimal("200"), ExpenseCategory.SAVINGS, "new");

            Limit limit1 = mock(Limit.class);
            Limit limit2 = mock(Limit.class);

            when(expenseManagerService.getExpenseByIdOrThrow(1L)).thenReturn(existing);
            when(limitRepository.getLimitAmountByUserIdAndType(anyLong(), any())).thenReturn(Optional.empty());
            when(financialPeriodService.getExpensesSum(anyLong(), any())).thenReturn(BigDecimal.ZERO);
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of(limit1, limit2));
            when(limitCalculateService.calculateLimitStats(eq(limit1), eq(userId), any(LocalDate.class)))
                    .thenReturn(buildLimitStats(1L, 40.0, PeriodType.DAILY));
            when(limitCalculateService.calculateLimitStats(eq(limit2), eq(userId), any(LocalDate.class)))
                    .thenReturn(buildLimitStats(2L, 60.0, PeriodType.MONTHLY));

            expenseService.editExpense(request, userId, 1L, PeriodType.DAILY);

            verify(kafkaTemplate, times(2)).send(eq("limit.calculate-stats"), any(LimitStatsEvent.class));
        }

        @Test
        void shouldPublishCorrectLimitStatsDataOnEdit() {

            Expense existing = buildExpense(1L, new BigDecimal("100"), ExpenseCategory.FOOD);
            ExpenseRequestDto request = buildEditRequest(new BigDecimal("200"), ExpenseCategory.SAVINGS, "new");
            Limit limit = mock(Limit.class);

            when(expenseManagerService.getExpenseByIdOrThrow(1L)).thenReturn(existing);
            when(limitRepository.getLimitAmountByUserIdAndType(anyLong(), any())).thenReturn(Optional.empty());
            when(financialPeriodService.getExpensesSum(anyLong(), any())).thenReturn(BigDecimal.ZERO);
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of(limit));
            when(limitCalculateService.calculateLimitStats(eq(limit), eq(userId), any(LocalDate.class)))
                    .thenReturn(buildLimitStats(3L, 55.0, PeriodType.WEEKLY));

            expenseService.editExpense(request, userId, 1L, PeriodType.DAILY);

            ArgumentCaptor<LimitStatsEvent> captor = ArgumentCaptor.forClass(LimitStatsEvent.class);
            verify(kafkaTemplate).send(eq("limit.calculate-stats"), captor.capture());
            assertEquals(userId, captor.getValue().userId());
            assertEquals(3L, captor.getValue().limitId());
            assertEquals(PeriodType.WEEKLY, captor.getValue().periodType());
        }

        @Test
        void shouldNotPublishLimitStatsNoLimitsExistOnEdit() {

            Expense existing = buildExpense(1L, new BigDecimal("100"), ExpenseCategory.FOOD);
            ExpenseRequestDto request = buildEditRequest(new BigDecimal("200"), ExpenseCategory.SAVINGS, "new");
            stubSuccessfulEdit(existing);

            expenseService.editExpense(request, userId, 1L, PeriodType.DAILY);

            verify(kafkaTemplate, never()).send(eq("limit.calculate-stats"), any());
        }

        private ExpenseRequestDto buildEditRequest(BigDecimal amount, ExpenseCategory category, String description) {
            ExpenseDto dto = new ExpenseDto(null, null, amount, category, null, description);
            return new ExpenseRequestDto(dto, new ConfirmPasswordDto("pass"), null);
        }
    }

    @Test
    void shouldReturnMappedExpenses() {
        when(expenseRepository.findAllByUserId(userId)).thenReturn(List.of(new Expense(), new Expense()));
        when(expenseMapper.mapExpenseToDto(any()))
                .thenReturn(new ExpenseDto(null, null, BigDecimal.TEN, ExpenseCategory.FOOD, null, "x"));

        List<?> result = expenseService.getExpense(userId);

        assertEquals(2, result.size());
    }

    @Nested
    class DeleteExpenseTests {

        @Test
        void shouldDeleteExpenseSuccessfully() {

            Expense expense = buildExpense(1L, new BigDecimal("100"), ExpenseCategory.FOOD);
            when(expenseRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(expense));
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());

            expenseService.deleteExpense(1L, userId);

            verify(roundUpService).handleExpenseForRoundUp(userId, 1L, PiggyBankAutomationMode.ROLLBACK);
            verify(walletService).addBalanceToWallet(userId, expense.getAmount());
            verify(expenseRepository).delete(expense);

            ArgumentCaptor<ExpenseActivityEvent> captor = ArgumentCaptor.forClass(ExpenseActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.expense"), captor.capture());
            assertEquals(ExpenseActivityType.DELETED_EXPENSE, captor.getValue().type());
        }

        @Test
        void shouldThrowExpenseNotFound() {
            when(expenseRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> expenseService.deleteExpense(1L, userId));

            verifyNoInteractions(walletService, roundUpService);
            verify(expenseRepository, never()).delete(any());
        }

        @Test
        void shouldPublishLimitStatsEventForEachLimitOnDelete() {

            Expense expense = buildExpense(1L, new BigDecimal("100"), ExpenseCategory.FOOD);
            Limit limit1 = mock(Limit.class);
            Limit limit2 = mock(Limit.class);
            when(expenseRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(expense));
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of(limit1, limit2));
            when(limitCalculateService.calculateLimitStats(eq(limit1), eq(userId), any(LocalDate.class)))
                    .thenReturn(buildLimitStats(1L, 20.0, PeriodType.DAILY));
            when(limitCalculateService.calculateLimitStats(eq(limit2), eq(userId), any(LocalDate.class)))
                    .thenReturn(buildLimitStats(2L, 45.0, PeriodType.MONTHLY));

            expenseService.deleteExpense(1L, userId);

            verify(kafkaTemplate, times(2)).send(eq("limit.calculate-stats"), any(LimitStatsEvent.class));
        }

        @Test
        void shouldPublishCorrectLimitStatsDataOnDelete() {

            Expense expense = buildExpense(1L, new BigDecimal("100"), ExpenseCategory.FOOD);
            Limit limit = mock(Limit.class);
            when(expenseRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(expense));
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of(limit));
            when(limitCalculateService.calculateLimitStats(eq(limit), eq(userId), any(LocalDate.class)))
                    .thenReturn(buildLimitStats(7L, 90.0, PeriodType.WEEKLY));

            expenseService.deleteExpense(1L, userId);

            ArgumentCaptor<LimitStatsEvent> captor = ArgumentCaptor.forClass(LimitStatsEvent.class);
            verify(kafkaTemplate).send(eq("limit.calculate-stats"), captor.capture());
            assertEquals(userId, captor.getValue().userId());
            assertEquals(7L, captor.getValue().limitId());
            assertEquals(PeriodType.WEEKLY, captor.getValue().periodType());
        }

        @Test
        void shouldNotPublishLimitStatsNoLimitsExistOnDelete() {

            Expense expense = buildExpense(1L, new BigDecimal("100"), ExpenseCategory.FOOD);
            when(expenseRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(expense));
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());

            expenseService.deleteExpense(1L, userId);

            verify(kafkaTemplate, never()).send(eq("limit.calculate-stats"), any());
        }
    }
}