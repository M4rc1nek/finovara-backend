package com.finovara.financeservice.expense.processor;

import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.limit.repository.LimitRepository;
import com.finovara.financeservice.limit.service.LimitCalculateService;
import com.finovara.financeservice.trash.service.TrashMoverService;
import com.finovara.financeservice.wallet.service.WalletService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseProcessorTest {

    private static final Long FIRST_USER_ID = 1L;
    private static final Long SECOND_USER_ID = 2L;

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private LimitRepository limitRepository;
    @Mock
    private LimitCalculateService limitCalculateService;
    @Mock
    private WalletService walletService;
    @Mock
    private OutboxService outboxService;
    @Mock
    private TrashMoverService trashMoverService;

    @InjectMocks
    private ExpenseProcessor processor;

    @Nested
    class MoveAllExpensesToTrash {

        @Test
        void shouldMoveUserExpensesInBatchesWhenActiveExpensesExist() {
            Expense firstUserExpense = expense(10L, FIRST_USER_ID);
            Expense secondUserExpense = expense(20L, SECOND_USER_ID);
            when(expenseRepository.findUserIdsWithActiveExpenses()).thenReturn(
                    List.of(SECOND_USER_ID, FIRST_USER_ID));
            when(expenseRepository.findAllActiveByUserIdInForUpdate(List.of(FIRST_USER_ID, SECOND_USER_ID)))
                    .thenReturn(List.of(firstUserExpense, secondUserExpense));
            when(limitRepository.findAllActiveByUserIdIn(List.of(FIRST_USER_ID, SECOND_USER_ID)))
                    .thenReturn(List.of());
            when(limitCalculateService.calculateLimitStatsForUsers(anyMap(), any(LocalDate.class)))
                    .thenReturn(Map.of());

            processor.moveAllExpensesToTrash();

            verify(walletService).lockWalletsForUpdate(List.of(FIRST_USER_ID, SECOND_USER_ID));
            verify(expenseRepository).findAllActiveByUserIdInForUpdate(List.of(FIRST_USER_ID, SECOND_USER_ID));
            verify(trashMoverService).moveExpensesToTrash(List.of(firstUserExpense, secondUserExpense));
        }

        @Test
        void shouldNotQueryOrLockOtherDataWhenThereAreNoActiveExpenses() {
            when(expenseRepository.findUserIdsWithActiveExpenses()).thenReturn(List.of());

            processor.moveAllExpensesToTrash();

            verifyNoInteractions(walletService, limitRepository, limitCalculateService, trashMoverService);
            verify(expenseRepository, never()).findAllActiveByUserIdInForUpdate(anyList());
        }

        @Test
        void shouldPublishLimitStatsForUsersFromOneLimitsQuery() {
            Expense expense = expense(10L, FIRST_USER_ID);
            when(expenseRepository.findUserIdsWithActiveExpenses()).thenReturn(List.of(FIRST_USER_ID));
            when(expenseRepository.findAllActiveByUserIdInForUpdate(List.of(FIRST_USER_ID)))
                    .thenReturn(List.of(expense));
            when(limitRepository.findAllActiveByUserIdIn(List.of(FIRST_USER_ID))).thenReturn(List.of());
            when(limitCalculateService.calculateLimitStatsForUsers(anyMap(), any(LocalDate.class)))
                    .thenReturn(Map.of());

            processor.moveAllExpensesToTrash();

            verify(limitRepository).findAllActiveByUserIdIn(List.of(FIRST_USER_ID));
            verify(limitRepository, never()).findAllActiveByUserId(FIRST_USER_ID);
            verify(limitCalculateService).calculateLimitStatsForUsers(anyMap(), any(LocalDate.class));
        }
    }

    private Expense expense(Long id, Long userId) {
        return Expense.builder()
                .id(id)
                .userId(userId)
                .amount(BigDecimal.TEN)
                .build();
    }
}
