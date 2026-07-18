package com.finovara.financeservice.sharedaccount.expense.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseDto;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseResponse;
import com.finovara.financeservice.sharedaccount.limit.repository.SharedLimitRepository;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.expense.model.SharedExpense;
import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
import com.finovara.financeservice.sharedaccount.expense.mapper.SharedExpenseMapper;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import com.finovara.financeservice.util.expense.SharedExpenseManagerService;
import com.finovara.financeservice.util.periodbalance.FinancialPeriodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    private SharedExpenseMapper sharedExpenseMapper;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private SharedAccountParticipantsService sharedAccountParticipantsService;

    @InjectMocks
    private SharedExpenseService sharedExpenseService;

    private Long userId;
    private SharedExpenseDto sharedExpenseDto;

    @BeforeEach
    void setUp() {
        userId = 1L;
        sharedExpenseDto = mock(SharedExpenseDto.class);
    }

    @Nested
    class AddExpense {

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

            when(sharedLimitRepository.findAllByUserId(userId)).thenReturn(List.of());
        }

        @Test
        void shouldAddExpenseAndReturnResponse() {
            when(sharedExpenseDto.amount()).thenReturn(amount);
            when(sharedExpenseDto.category()).thenReturn(category);
            when(sharedExpenseDto.description()).thenReturn(description);

            participants = mock(SharedAccountParticipantsResponse.class);
            when(participants.ownerId()).thenReturn(ownerId);
            when(participants.memberId()).thenReturn(memberId);
            when(sharedAccountParticipantsService.getParticipants(userId)).thenReturn(participants);
            when(authBackendClient.getUsername(userId)).thenReturn(username);

            SharedExpenseResponse response = sharedExpenseService.addExpense(sharedExpenseDto, userId);

            verify(sharedWalletService).removeBalanceFromWallet(userId, amount);
            verify(sharedExpenseRepository).save(any(SharedExpense.class));

            assertEquals(userId, response.userId());
            assertEquals(username, response.username());
            assertNull(response.expenseId());
        }

        @Test
        void shouldThrowWhenAmountIsZero() {
            when(sharedExpenseDto.amount()).thenReturn(BigDecimal.ZERO);

            assertThrows(InvalidInputException.class,
                    () -> sharedExpenseService.addExpense(sharedExpenseDto, userId));

            verify(sharedAccountParticipantsService, never()).getParticipants(any());
            verify(sharedExpenseRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenAmountIsNegative() {
            when(sharedExpenseDto.amount()).thenReturn(new BigDecimal("-10.00"));

            assertThrows(InvalidInputException.class,
                    () -> sharedExpenseService.addExpense(sharedExpenseDto, userId));

            verify(sharedAccountParticipantsService, never()).getParticipants(any());
            verify(sharedExpenseRepository, never()).save(any());
        }
    }

    @Nested
    class EditExpense {

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
        }

        @Test
        void shouldEditExpenseWhenUserIsOwner() {
            when(sharedExpenseManagerService.getSharedExpenseOrThrow(expenseId)).thenReturn(existingExpense);
            when(existingExpense.getOwnerId()).thenReturn(ownerId);
            when(existingExpense.getAmount()).thenReturn(oldAmount);

            when(sharedExpenseDto.amount()).thenReturn(newAmount);
            when(sharedExpenseDto.category()).thenReturn(category);
            when(sharedExpenseDto.description()).thenReturn(description);

            when(sharedLimitRepository.findAllByUserId(ownerId)).thenReturn(List.of());

            Long result = sharedExpenseService.editExpense(sharedExpenseDto, ownerId, expenseId);

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

            when(sharedExpenseDto.amount()).thenReturn(newAmount);
            when(sharedExpenseDto.category()).thenReturn(category);
            when(sharedExpenseDto.description()).thenReturn(description);

            when(sharedLimitRepository.findAllByUserId(memberId)).thenReturn(List.of());

            Long result = sharedExpenseService.editExpense(sharedExpenseDto, memberId, expenseId);

            assertEquals(expenseId, result);
            verify(sharedWalletService).addBalanceToWallet(memberId, oldAmount);
            verify(sharedWalletService).removeBalanceFromWallet(memberId, newAmount);
            verify(existingExpense).setAmount(newAmount);
            verify(existingExpense).setCategory(category);
            verify(existingExpense).setDescription(description);
            verify(sharedExpenseRepository).save(existingExpense);
        }

        @Test
        void shouldThrowWhenUserIsNeitherOwnerNorMember() {
            Long strangerId = 99L;

            when(sharedExpenseManagerService.getSharedExpenseOrThrow(expenseId)).thenReturn(existingExpense);
            when(existingExpense.getOwnerId()).thenReturn(ownerId);
            when(existingExpense.getMemberId()).thenReturn(memberId);

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> sharedExpenseService.editExpense(sharedExpenseDto, strangerId, expenseId));

            verify(sharedWalletService, never()).addBalanceToWallet(any(), any());
            verify(sharedWalletService, never()).removeBalanceFromWallet(any(), any());
            verify(sharedExpenseRepository, never()).save(any());
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
        void shouldThrowWhenExpenseNotFound() {
            when(sharedExpenseRepository.findByIdAndOwnerIdOrMemberId(expenseId, userId))
                    .thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> sharedExpenseService.deleteExpense(expenseId, userId));

            verify(sharedWalletService, never()).addBalanceToWallet(any(), any());
            verify(sharedExpenseRepository, never()).delete(any());
        }
    }
}