package com.finovara.financeservice.trash.service;

import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.limit.repository.LimitRepository;
import com.finovara.financeservice.limit.service.LimitCalculateService;
import com.finovara.financeservice.settings.piggybank.roundup.service.RoundUpService;
import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;
import com.finovara.financeservice.trash.service.action.expense.ExpenseTrashRestoreStrategy;
import com.finovara.financeservice.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.finovara.contracts.mainaccount.activity.model.TransactionType.WITHDRAWAL;
import static com.finovara.contracts.mainaccount.activity.model.WalletTransactionReason.EXPENSE;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseTrashRestoreStrategyTest {

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
    private OutboxService outboxService;

    @InjectMocks
    private ExpenseTrashRestoreStrategy strategy;

    @Test
    void restoresExpenseAndReappliesItsWalletEffect() {
        BigDecimal amount = BigDecimal.TEN;
        Expense expense = Expense.builder()
                .id(10L)
                .userId(2L)
                .amount(amount)
                .category(ExpenseCategory.values()[0])
                .build();
        TrashItem item = TrashItem.builder()
                .entityType(TrashEntityType.EXPENSE)
                .entityId(10L)
                .userId(2L)
                .build();
        when(expenseRepository.findTrashedByIdAndUserIdForUpdate(10L, 2L)).thenReturn(Optional.of(expense));
        when(limitRepository.findAllActiveByUserId(2L)).thenReturn(List.of());

        strategy.restore(item);

        verify(expenseRepository).save(expense);
        verify(walletService).removeBalanceFromWallet(2L, amount, WITHDRAWAL, EXPENSE);
    }

    @Test
    void restoresAutomaticallyArchivedExpenseWithoutChangingWallet() {
        Expense expense = Expense.builder()
                .id(10L)
                .userId(2L)
                .amount(BigDecimal.TEN)
                .category(ExpenseCategory.values()[0])
                .build();
        TrashItem item = TrashItem.builder()
                .entityType(TrashEntityType.EXPENSE)
                .entityId(10L)
                .userId(2L)
                .financialEffectsReverted(false)
                .build();
        when(expenseRepository.findTrashedByIdAndUserIdForUpdate(10L, 2L)).thenReturn(Optional.of(expense));
        when(limitRepository.findAllActiveByUserId(2L)).thenReturn(List.of());

        strategy.restore(item);

        verify(expenseRepository).save(expense);
        verifyNoInteractions(walletService, roundUpService);
    }
}
