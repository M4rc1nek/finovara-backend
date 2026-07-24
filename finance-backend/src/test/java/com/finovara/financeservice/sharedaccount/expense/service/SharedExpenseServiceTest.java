package com.finovara.financeservice.sharedaccount.expense.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseDto;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseRequest;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseResponse;
import com.finovara.financeservice.sharedaccount.expense.mapper.SharedExpenseMapper;
import com.finovara.financeservice.sharedaccount.expense.model.SharedExpense;
import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
import com.finovara.financeservice.sharedaccount.limit.model.SharedLimit;
import com.finovara.financeservice.sharedaccount.limit.repository.SharedLimitRepository;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.settings.expense.analysis.dto.ExpenseAnalysisMode;
import com.finovara.financeservice.sharedaccount.settings.expense.analysis.service.ExpenseAnalysisService;
import com.finovara.financeservice.sharedaccount.settings.expense.largeexpense.service.LargeExpenseNotificationService;
import com.finovara.financeservice.sharedaccount.settings.expense.spendcontrol.service.SpendControlService;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import com.finovara.financeservice.util.expense.SharedExpenseManagerService;
import com.finovara.financeservice.util.periodbalance.FinancialPeriodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedExpenseServiceTest {

    @Mock
    private SharedExpenseRepository sharedExpenseRepository;

    @Mock
    private SharedWalletService sharedWalletService;

    @Mock
    private SharedLimitRepository sharedLimitRepository;

    @Mock
    private FinancialPeriodService financialPeriodService;

    @Mock
    private SharedExpenseManagerService sharedExpenseManagerService;

    @Mock
    private SharedAccountParticipantsService sharedAccountParticipantsService;

    @Mock
    private SpendControlService spendControlService;

    @Mock
    private ExpenseAnalysisService expenseAnalysisService;

    @Mock
    private LargeExpenseNotificationService largeExpenseNotificationService;

    @Mock
    private SharedExpenseMapper sharedExpenseMapper;

    @Mock
    private AuthBackendClient authBackendClient;

    @InjectMocks
    private SharedExpenseService sharedExpenseService;

    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;
    }

    @Nested
    class AddExpense {

        private SharedExpenseDto sharedExpenseDto;
        private SharedExpenseRequest sharedExpenseRequest;
        private SharedAccountParticipantsResponse participants;
        private Long ownerId;
        private Long memberId;
        private BigDecimal amount;
        private ExpenseCategory category;
        private String description;
        private String username;

        @BeforeEach
        void setUp() {
            ownerId = 1L;
            memberId = 2L;
            amount = new BigDecimal("150.00");
            category = ExpenseCategory.FOOD;
            description = "Groceries";
            username = "testuser";

            sharedExpenseDto = mock(SharedExpenseDto.class);
            sharedExpenseRequest = mock(SharedExpenseRequest.class);
            when(sharedExpenseRequest.sharedExpenseDto()).thenReturn(sharedExpenseDto);
        }

        private void stubCategoryAndDescription() {
            when(sharedExpenseDto.category()).thenReturn(category);
            when(sharedExpenseDto.description()).thenReturn(description);
        }

        private void stubSuccessfulDependencies() {
            participants = mock(SharedAccountParticipantsResponse.class);
            when(participants.ownerId()).thenReturn(ownerId);
            when(participants.memberId()).thenReturn(memberId);
            when(sharedAccountParticipantsService.getParticipants(userId)).thenReturn(participants);
            when(authBackendClient.getUsername(userId)).thenReturn(username);
            when(sharedLimitRepository.findAllByUserId(userId)).thenReturn(List.of());
        }

        @Test
        void shouldAddExpenseAndReturnResponseWhenValid() {
            when(sharedExpenseDto.amount()).thenReturn(amount);
            stubCategoryAndDescription();
            stubSuccessfulDependencies();

            SharedExpenseResponse response = sharedExpenseService.addExpense(sharedExpenseRequest, userId);

            verify(sharedWalletService).removeBalanceFromWallet(userId, amount);
            verify(spendControlService).handleSpendControl(userId, amount);
            verify(expenseAnalysisService).handleExpenseAnalysis(eq(userId), any(), eq(amount), eq(ExpenseAnalysisMode.ADD));
            verify(sharedExpenseRepository).save(any(SharedExpense.class));
            verify(largeExpenseNotificationService).handleLargeNotification(eq(userId), any(SharedExpense.class));

            assertEquals(userId, response.userId());
            assertEquals(username, response.username());
            assertNull(response.expenseId());
        }

        @Test
        void shouldBuildExpenseWithOwnerAndMemberIdsFromParticipants() {
            when(sharedExpenseDto.amount()).thenReturn(amount);
            stubCategoryAndDescription();
            stubSuccessfulDependencies();
            ArgumentCaptor<SharedExpense> captor = ArgumentCaptor.forClass(SharedExpense.class);

            sharedExpenseService.addExpense(sharedExpenseRequest, userId);

            verify(sharedExpenseRepository).save(captor.capture());
            SharedExpense saved = captor.getValue();
            assertEquals(ownerId, saved.getOwnerId());
            assertEquals(memberId, saved.getMemberId());
            assertEquals(userId, saved.getCreatedByUserId());
            assertEquals(amount, saved.getAmount());
            assertEquals(category, saved.getCategory());
            assertEquals(description, saved.getDescription());
        }

        @Test
        void shouldThrowExceptionWhenAmountIsZero() {
            when(sharedExpenseDto.amount()).thenReturn(BigDecimal.ZERO);

            assertThrows(InvalidInputException.class,
                    () -> sharedExpenseService.addExpense(sharedExpenseRequest, userId));

            verify(sharedAccountParticipantsService, never()).getParticipants(any());
            verify(sharedExpenseRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNegative() {
            when(sharedExpenseDto.amount()).thenReturn(new BigDecimal("-10.00"));

            assertThrows(InvalidInputException.class,
                    () -> sharedExpenseService.addExpense(sharedExpenseRequest, userId));

            verify(sharedAccountParticipantsService, never()).getParticipants(any());
            verify(spendControlService, never()).handleSpendControl(any(), any());
            verify(sharedExpenseRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenAmountIsBelowOne() {
            when(sharedExpenseDto.amount()).thenReturn(new BigDecimal("0.99"));

            assertThrows(InvalidInputException.class,
                    () -> sharedExpenseService.addExpense(sharedExpenseRequest, userId));

            verify(sharedExpenseRepository, never()).save(any());
        }

        @Test
        void shouldAddExpenseWhenAmountIsExactlyOne() {
            when(sharedExpenseDto.amount()).thenReturn(BigDecimal.ONE);
            stubCategoryAndDescription();
            stubSuccessfulDependencies();

            SharedExpenseResponse response = sharedExpenseService.addExpense(sharedExpenseRequest, userId);

            assertEquals(userId, response.userId());
            verify(sharedExpenseRepository).save(any(SharedExpense.class));
        }

        @Test
        void shouldThrowExceptionWhenGeneralLimitExceeded() {
            SharedLimit generalLimit = mock(SharedLimit.class);
            when(generalLimit.getCategory()).thenReturn(null);
            when(generalLimit.getAmount()).thenReturn(new BigDecimal("100.00"));

            when(sharedExpenseDto.amount()).thenReturn(amount);
            stubCategoryAndDescription();
            when(sharedLimitRepository.findAllByUserId(userId)).thenReturn(List.of(generalLimit));
            when(financialPeriodService.getSharedExpensesSum(any(), any(), any())).thenReturn(new BigDecimal("50.00"));

            MissingRequirementException exception = assertThrows(MissingRequirementException.class,
                    () -> sharedExpenseService.addExpense(sharedExpenseRequest, userId));

            assertEquals("General limit exceeded", exception.getMessage());
            verify(sharedExpenseRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenCategoryLimitExceeded() {
            SharedLimit categoryLimit = mock(SharedLimit.class);
            when(categoryLimit.getCategory()).thenReturn(category);
            when(categoryLimit.getAmount()).thenReturn(new BigDecimal("100.00"));

            when(sharedExpenseDto.amount()).thenReturn(amount);
            stubCategoryAndDescription();
            when(sharedLimitRepository.findAllByUserId(userId)).thenReturn(List.of(categoryLimit));
            when(financialPeriodService.getSharedExpensesSum(any(), any(), any())).thenReturn(new BigDecimal("80.00"));

            MissingRequirementException exception = assertThrows(MissingRequirementException.class,
                    () -> sharedExpenseService.addExpense(sharedExpenseRequest, userId));

            assertEquals("Category limit exceeded", exception.getMessage());
            verify(sharedExpenseRepository, never()).save(any());
        }

        @Test
        void shouldNotThrowWhenLimitDoesNotApplyToCategory() {
            SharedLimit otherCategoryLimit = mock(SharedLimit.class);
            when(otherCategoryLimit.getCategory()).thenReturn(ExpenseCategory.TRANSPORT);

            when(sharedExpenseDto.amount()).thenReturn(amount);
            stubCategoryAndDescription();
            stubSuccessfulDependencies();
            when(sharedLimitRepository.findAllByUserId(userId)).thenReturn(List.of(otherCategoryLimit));

            SharedExpenseResponse response = sharedExpenseService.addExpense(sharedExpenseRequest, userId);

            assertEquals(userId, response.userId());
            verify(financialPeriodService, never()).getSharedExpensesSum(any(), any(), any());
            verify(sharedExpenseRepository).save(any(SharedExpense.class));
        }

        @Test
        void shouldNotThrowWhenTotalWithinLimit() {
            SharedLimit categoryLimit = mock(SharedLimit.class);
            when(categoryLimit.getCategory()).thenReturn(category);
            when(categoryLimit.getAmount()).thenReturn(new BigDecimal("300.00"));

            when(sharedExpenseDto.amount()).thenReturn(amount);
            stubCategoryAndDescription();
            stubSuccessfulDependencies();
            when(sharedLimitRepository.findAllByUserId(userId)).thenReturn(List.of(categoryLimit));
            when(financialPeriodService.getSharedExpensesSum(any(), any(), any())).thenReturn(new BigDecimal("50.00"));

            SharedExpenseResponse response = sharedExpenseService.addExpense(sharedExpenseRequest, userId);

            assertEquals(userId, response.userId());
            verify(sharedExpenseRepository).save(any(SharedExpense.class));
        }
    }

    @Nested
    class EditExpense {

        private SharedExpenseDto sharedExpenseDto;
        private SharedExpenseRequest sharedExpenseRequest;
        private Long expenseId;
        private Long ownerId;
        private Long memberId;
        private SharedExpense existingExpense;
        private BigDecimal oldAmount;
        private BigDecimal newAmount;
        private ExpenseCategory category;
        private String description;

        @BeforeEach
        void setUp() {
            expenseId = 5L;
            ownerId = 1L;
            memberId = 2L;
            oldAmount = new BigDecimal("100.00");
            newAmount = new BigDecimal("200.00");
            category = ExpenseCategory.TRANSPORT;
            description = "Updated description";

            existingExpense = mock(SharedExpense.class);
            sharedExpenseDto = mock(SharedExpenseDto.class);
            sharedExpenseRequest = mock(SharedExpenseRequest.class);
            when(sharedExpenseRequest.sharedExpenseDto()).thenReturn(sharedExpenseDto);
        }

        private void stubEditDto() {
            when(sharedExpenseDto.amount()).thenReturn(newAmount);
            when(sharedExpenseDto.category()).thenReturn(category);
            when(sharedExpenseDto.description()).thenReturn(description);
        }

        @Test
        void shouldEditExpenseWhenUserIsOwner() {
            when(sharedExpenseManagerService.getSharedExpenseOrThrow(expenseId)).thenReturn(existingExpense);
            when(existingExpense.getOwnerId()).thenReturn(ownerId);
            when(existingExpense.getAmount()).thenReturn(oldAmount);
            stubEditDto();
            when(sharedLimitRepository.findAllByUserId(ownerId)).thenReturn(List.of());

            Long result = sharedExpenseService.editExpense(sharedExpenseRequest, ownerId, expenseId);

            assertEquals(expenseId, result);
            verify(sharedWalletService).addBalanceToWallet(ownerId, oldAmount);
            verify(sharedWalletService).removeBalanceFromWallet(ownerId, newAmount);
            verify(existingExpense).setAmount(newAmount);
            verify(existingExpense).setCategory(category);
            verify(existingExpense).setDescription(description);
            verify(sharedExpenseRepository).save(existingExpense);
        }

        @Test
        void shouldEditExpenseWhenUserIsMember() {
            when(sharedExpenseManagerService.getSharedExpenseOrThrow(expenseId)).thenReturn(existingExpense);
            when(existingExpense.getOwnerId()).thenReturn(ownerId);
            when(existingExpense.getMemberId()).thenReturn(memberId);
            when(existingExpense.getAmount()).thenReturn(oldAmount);
            stubEditDto();
            when(sharedLimitRepository.findAllByUserId(memberId)).thenReturn(List.of());

            Long result = sharedExpenseService.editExpense(sharedExpenseRequest, memberId, expenseId);

            assertEquals(expenseId, result);
            verify(sharedWalletService).addBalanceToWallet(memberId, oldAmount);
            verify(sharedWalletService).removeBalanceFromWallet(memberId, newAmount);
            verify(existingExpense).setAmount(newAmount);
            verify(existingExpense).setCategory(category);
            verify(existingExpense).setDescription(description);
            verify(sharedExpenseRepository).save(existingExpense);
        }

        @Test
        void shouldThrowExceptionWhenUserIsNeitherOwnerNorMember() {
            Long strangerId = 99L;

            when(sharedExpenseManagerService.getSharedExpenseOrThrow(expenseId)).thenReturn(existingExpense);
            when(existingExpense.getOwnerId()).thenReturn(ownerId);
            when(existingExpense.getMemberId()).thenReturn(memberId);

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> sharedExpenseService.editExpense(sharedExpenseRequest, strangerId, expenseId));

            verify(sharedWalletService, never()).addBalanceToWallet(any(), any());
            verify(sharedWalletService, never()).removeBalanceFromWallet(any(), any());
            verify(sharedExpenseRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenLimitExceededAfterEdit() {
            SharedLimit categoryLimit = mock(SharedLimit.class);
            when(categoryLimit.getCategory()).thenReturn(category);
            when(categoryLimit.getAmount()).thenReturn(new BigDecimal("150.00"));

            when(sharedExpenseManagerService.getSharedExpenseOrThrow(expenseId)).thenReturn(existingExpense);
            when(existingExpense.getOwnerId()).thenReturn(ownerId);
            when(existingExpense.getCategory()).thenReturn(category);
            when(existingExpense.getAmount()).thenReturn(oldAmount);
            stubEditDto();
            when(sharedLimitRepository.findAllByUserId(ownerId)).thenReturn(List.of(categoryLimit));
            when(financialPeriodService.getSharedExpensesSum(any(), any(), any())).thenReturn(new BigDecimal("100.00"));

            MissingRequirementException exception = assertThrows(MissingRequirementException.class,
                    () -> sharedExpenseService.editExpense(sharedExpenseRequest, ownerId, expenseId));

            assertEquals("Category limit exceeded", exception.getMessage());
            verify(sharedExpenseRepository, never()).save(any());
        }

        @Test
        void shouldNotDoubleCountOldAmountWhenCategoryUnchanged() {
            SharedLimit categoryLimit = mock(SharedLimit.class);
            when(categoryLimit.getCategory()).thenReturn(category);
            when(categoryLimit.getAmount()).thenReturn(new BigDecimal("250.00"));

            when(sharedExpenseManagerService.getSharedExpenseOrThrow(expenseId)).thenReturn(existingExpense);
            when(existingExpense.getOwnerId()).thenReturn(ownerId);
            when(existingExpense.getCategory()).thenReturn(category);
            when(existingExpense.getAmount()).thenReturn(oldAmount);
            stubEditDto();
            when(sharedLimitRepository.findAllByUserId(ownerId)).thenReturn(List.of(categoryLimit));
            when(financialPeriodService.getSharedExpensesSum(any(), any(), any())).thenReturn(new BigDecimal("100.00"));

            Long result = sharedExpenseService.editExpense(sharedExpenseRequest, ownerId, expenseId);

            assertEquals(expenseId, result);
            verify(sharedExpenseRepository).save(existingExpense);
        }

        @Test
        void shouldThrowExceptionWhenOldCategoryDidNotApplyAndNewTotalExceedsLimit() {
            SharedLimit categoryLimit = mock(SharedLimit.class);
            when(categoryLimit.getCategory()).thenReturn(category);
            when(categoryLimit.getAmount()).thenReturn(new BigDecimal("200.00"));

            when(sharedExpenseManagerService.getSharedExpenseOrThrow(expenseId)).thenReturn(existingExpense);
            when(existingExpense.getOwnerId()).thenReturn(ownerId);
            when(existingExpense.getCategory()).thenReturn(ExpenseCategory.FOOD);
            when(existingExpense.getAmount()).thenReturn(oldAmount);
            stubEditDto();
            when(sharedLimitRepository.findAllByUserId(ownerId)).thenReturn(List.of(categoryLimit));
            when(financialPeriodService.getSharedExpensesSum(any(), any(), any())).thenReturn(new BigDecimal("50.00"));

            MissingRequirementException exception = assertThrows(MissingRequirementException.class,
                    () -> sharedExpenseService.editExpense(sharedExpenseRequest, ownerId, expenseId));

            assertEquals("Category limit exceeded", exception.getMessage());
        }

        @Test
        void shouldReturnExpenseIdAfterSuccessfulEdit() {
            when(sharedExpenseManagerService.getSharedExpenseOrThrow(expenseId)).thenReturn(existingExpense);
            when(existingExpense.getOwnerId()).thenReturn(ownerId);
            when(existingExpense.getAmount()).thenReturn(oldAmount);
            stubEditDto();
            when(sharedLimitRepository.findAllByUserId(ownerId)).thenReturn(List.of());

            Long result = sharedExpenseService.editExpense(sharedExpenseRequest, ownerId, expenseId);

            assertEquals(expenseId, result);
        }
    }

    @Nested
    class GetExpense {

        @Test
        void shouldReturnMappedExpensesGroupedByUsername() {
            SharedExpense expenseOne = mock(SharedExpense.class);
            SharedExpense expenseTwo = mock(SharedExpense.class);
            SharedExpense expenseThree = mock(SharedExpense.class);

            when(expenseOne.getCreatedByUserId()).thenReturn(10L);
            when(expenseTwo.getCreatedByUserId()).thenReturn(10L);
            when(expenseThree.getCreatedByUserId()).thenReturn(20L);

            when(sharedExpenseRepository.findAllByOwnerIdOrMemberId(userId))
                    .thenReturn(List.of(expenseOne, expenseTwo, expenseThree));

            when(authBackendClient.getUsername(10L)).thenReturn("alice");
            when(authBackendClient.getUsername(20L)).thenReturn("bob");

            SharedExpenseDto dtoOne = mock(SharedExpenseDto.class);
            SharedExpenseDto dtoTwo = mock(SharedExpenseDto.class);
            SharedExpenseDto dtoThree = mock(SharedExpenseDto.class);

            when(sharedExpenseMapper.mapToDto(expenseOne, "alice")).thenReturn(dtoOne);
            when(sharedExpenseMapper.mapToDto(expenseTwo, "alice")).thenReturn(dtoTwo);
            when(sharedExpenseMapper.mapToDto(expenseThree, "bob")).thenReturn(dtoThree);

            List<SharedExpenseDto> result = sharedExpenseService.getExpense(userId);

            assertEquals(List.of(dtoOne, dtoTwo, dtoThree), result);
            verify(authBackendClient, times(1)).getUsername(10L);
            verify(authBackendClient, times(1)).getUsername(20L);
        }

        @Test
        void shouldReturnEmptyListWhenNoExpenses() {
            when(sharedExpenseRepository.findAllByOwnerIdOrMemberId(userId)).thenReturn(List.of());

            List<SharedExpenseDto> result = sharedExpenseService.getExpense(userId);

            assertTrue(result.isEmpty());
            verify(authBackendClient, never()).getUsername(any());
            verify(sharedExpenseMapper, never()).mapToDto(any(), any());
        }
    }

    @Nested
    class DeleteExpense {

        private Long expenseId;
        private SharedExpense expense;
        private BigDecimal amount;

        @BeforeEach
        void setUp() {
            expenseId = 5L;
            amount = new BigDecimal("75.00");
            expense = mock(SharedExpense.class);
        }

        @Test
        void shouldDeleteExpenseAndRestoreBalance() {
            when(sharedExpenseRepository.findByIdAndOwnerIdOrMemberId(expenseId, userId))
                    .thenReturn(Optional.of(expense));
            when(expense.getAmount()).thenReturn(amount);

            sharedExpenseService.deleteExpense(expenseId, userId);

            verify(sharedWalletService).addBalanceToWallet(userId, amount);
            verify(sharedExpenseRepository).delete(expense);
        }

        @Test
        void shouldThrowExceptionWhenExpenseNotFound() {
            when(sharedExpenseRepository.findByIdAndOwnerIdOrMemberId(expenseId, userId))
                    .thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> sharedExpenseService.deleteExpense(expenseId, userId));

            verify(sharedWalletService, never()).addBalanceToWallet(any(), any());
            verify(sharedExpenseRepository, never()).delete(any());
        }
    }
}