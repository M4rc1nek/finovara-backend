package com.finovara.corebackend.expense.service;

import com.finovara.contracts.event.activity.expense.ExpenseActivityEvent;
import com.finovara.contracts.model.activity.ExpenseActivityType;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.corebackend.expense.dto.ExpenseDto;
import com.finovara.corebackend.expense.dto.ExpenseRequestDto;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.corebackend.expense.mapper.ExpenseMapper;
import com.finovara.corebackend.expense.model.Expense;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.corebackend.expense.repository.ExpenseRepository;
import com.finovara.corebackend.limit.repository.LimitRepository;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.usersetting.finances.expense.controlamount.service.ControlAmountService;
import com.finovara.corebackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.corebackend.usersetting.finances.expense.countlimit.service.CountQuantityLimitService;
import com.finovara.corebackend.usersetting.finances.expense.smartscan.dto.SmartScanMode;
import com.finovara.corebackend.usersetting.finances.expense.smartscan.service.SmartScanService;
import com.finovara.corebackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.corebackend.usersetting.piggybank.roundup.service.RoundUpService;
import com.finovara.contracts.dto.ConfirmPasswordDto;
import com.finovara.corebackend.util.expense.ExpenseManagerService;
import com.finovara.contracts.model.PeriodType;
import com.finovara.corebackend.util.periodbalance.FinancialPeriodService;
import com.finovara.corebackend.util.user.service.UserManagerService;
import com.finovara.corebackend.wallet.service.WalletService;
import org.springframework.kafka.core.KafkaTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
    private WalletService walletService;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private RoundUpService roundUpService;
    @Mock
    private CountQuantityLimitService countQuantityLimitService;
    @Mock
    private ControlAmountService controlAmountService;
    @Mock
    private SmartScanService smartScanService;
    @Mock
    private UserManagerService userManagerService;
    @Mock
    private FinancialPeriodService financialPeriodService;
    @Mock
    private ExpenseMapper expenseMapper;
    @Mock
    private ExpenseManagerService expenseManagerService;

    private User user;
    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        user = new User();
        user.setId(userId);

    }

    @Nested
    class AddExpenseTests {

        @Test
        void shouldAddExpenseSuccessfully() {
            BigDecimal amount = new BigDecimal("100");

            ExpenseRequestDto dto = new ExpenseRequestDto(new ExpenseDto(null, null, amount, ExpenseCategory.SAVINGS,
                    null, "test"), new ConfirmPasswordDto("password"), new CountQuantityLimitDto(true,
                    PeriodType.DAILY, 10));

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(limitRepository.getLimitAmountByUserIdAndType(anyLong(), any())).thenReturn(Optional.empty());
            when(financialPeriodService.getExpensesSum(anyLong(), eq(PeriodType.DAILY))).thenReturn(BigDecimal.ZERO);

            when(expenseRepository.save(any())).thenAnswer(invocation -> {
                Expense expense = invocation.getArgument(0);
                expense.setId(1L);
                return expense;
            });

            Long result = expenseService.addExpense(dto, userId, PeriodType.DAILY);
            assertEquals(1L, result);
            verify(countQuantityLimitService).handleExpenseLimitExceeded(userId, dto.countQuantityLimitDto(), dto.countQuantityLimitDto().periodType(), dto.confirmPasswordDto());
            ArgumentCaptor<ExpenseActivityEvent> eventCaptor = ArgumentCaptor.forClass(ExpenseActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.expense"), eventCaptor.capture());
            assertEquals(ExpenseActivityType.ADDED_EXPENSE, eventCaptor.getValue().type());
            verify(smartScanService).handleSmartScan(userId, dto.confirmPasswordDto(), amount, SmartScanMode.ADD);
            verify(walletService).removeBalanceFromWallet(userId, amount);
            verify(expenseRepository).save(any(Expense.class));
            verify(roundUpService).handleExpenseForRoundUp(eq(userId), anyLong(), eq(PiggyBankAutomationMode.APPLY));
            verify(controlAmountService).handleExpenseAmountControl(userId, amount);
        }
    }

    @Test
    void shouldThrowExceptionWhenAmountIsLessThanOne() {
        ExpenseRequestDto dto = new ExpenseRequestDto(new ExpenseDto(null, null, new BigDecimal("0.50"), ExpenseCategory.SAVINGS, null, "test"), new ConfirmPasswordDto("password"), new CountQuantityLimitDto(true, PeriodType.DAILY, 10));

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(financialPeriodService.getExpensesSum(anyLong(), eq(PeriodType.DAILY))).thenReturn(BigDecimal.ZERO);

        assertThrows(InvalidInputException.class, () -> expenseService.addExpense(dto, userId, PeriodType.DAILY));

        verify(expenseRepository, never()).save(any());
        verifyNoInteractions(walletService, smartScanService, roundUpService);
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userManagerService.getUserByIdOrThrow(anyLong())).thenThrow(new RequestedEntityNotFoundException("x"));

        assertThrows(RequestedEntityNotFoundException.class, () -> expenseService.addExpense(null, 1L, null));
    }

    @Nested
    class EditExpenseTests {
        @Test
        void shouldEditExpenseSuccessfully() {
            Long expenseId = 1L;

            Expense expense = new Expense();
            expense.setId(expenseId);
            expense.setAmount(new BigDecimal("100"));
            expense.setCategory(ExpenseCategory.SAVINGS);
            expense.setUserAssigned(user);

            ExpenseRequestDto dto = new ExpenseRequestDto(new ExpenseDto(null, null, new BigDecimal("200"),
                    ExpenseCategory.FOOD, null, "new"), new ConfirmPasswordDto("pass"), new CountQuantityLimitDto(true, PeriodType.DAILY, 10));

            when(userManagerService.getUserByIdOrThrow(1L)).thenReturn(user);
            when(expenseManagerService.getExpenseByIdOrThrow(expenseId)).thenReturn(expense);

            expenseService.editExpense(dto, 1L, expenseId, null);

            verify(expenseRepository).save(expense);
        }

        @Test
        void shouldThrowWhenExpenseNotFoundOnEdit() {
            when(expenseManagerService.getExpenseByIdOrThrow(anyLong())).thenThrow(new RequestedEntityNotFoundException("x"));

            assertThrows(RequestedEntityNotFoundException.class, () -> expenseService.editExpense(null, 1L, 1L, null));
        }

        @Test
        void shouldThrowWhenUserNotFoundOnEdit() {
            when(userManagerService.getUserByIdOrThrow(anyLong())).thenThrow(new RequestedEntityNotFoundException("x"));

            assertThrows(RequestedEntityNotFoundException.class, () -> expenseService.editExpense(null, 1L, 1L, null));
        }
    }

    @Nested
    class GetExpenseTests {
        @Test
        void shouldReturnExpenses() {
            when(userManagerService.getUserByIdOrThrow(1L)).thenReturn(user);
            when(expenseRepository.findAllByUserAssignedId(1L)).thenReturn(List.of(new Expense(), new Expense()));
            when(expenseMapper.mapExpenseToDto(any())).thenReturn(new ExpenseDto(null, null, BigDecimal.TEN, ExpenseCategory.FOOD,
                    null, "x"));

            List<ExpenseDto> result = expenseService.getExpense(1L);

            assertEquals(2, result.size());
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoExpenses() {
            when(userManagerService.getUserByIdOrThrow(1L)).thenReturn(user);
            when(expenseRepository.findAllByUserAssignedId(1L)).thenReturn(List.of());

            List<ExpenseDto> result = expenseService.getExpense(1L);

            assertTrue(result.isEmpty());

            verify(expenseMapper, never()).mapExpenseToDto(any());
        }

    }

    @Nested
    class DeleteExpenseTests {

        @Test
        void shouldDeleteExpenseSuccessfully() {
            Expense expense = new Expense();
            expense.setId(1L);
            expense.setUserAssigned(user);
            expense.setAmount(new BigDecimal("100"));

            when(userManagerService.getUserByIdOrThrow(1L)).thenReturn(user);
            when(expenseRepository.findByIdAndUserAssignedId(1L, 1L)).thenReturn(Optional.of(expense));

            expenseService.deleteExpense(1L, 1L);

            verify(expenseRepository).delete(expense);
        }

        @Test
        void shouldThrowWhenExpenseNotFound() {

            when(userManagerService.getUserByIdOrThrow(1L)).thenReturn(user);
            when(expenseRepository.findByIdAndUserAssignedId(anyLong(), anyLong())).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> expenseService.deleteExpense(1L, 1L));
        }

    }
}