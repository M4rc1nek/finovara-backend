package com.finovara.financeservice.trash.service.action.expense;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.mainaccount.transaction.event.limit.LimitStatsEvent;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.limit.dto.LimitStatsDto;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.limit.model.LimitStatus;
import com.finovara.financeservice.limit.repository.LimitRepository;
import com.finovara.financeservice.limit.service.LimitCalculateService;
import com.finovara.financeservice.settings.piggybank.roundup.service.RoundUpService;
import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;
import com.finovara.financeservice.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.finovara.contracts.mainaccount.activity.model.TransactionType.WITHDRAWAL;
import static com.finovara.contracts.mainaccount.activity.model.WalletTransactionReason.EXPENSE;
import static com.finovara.financeservice.settings.piggybank.autopayments.model.PiggyBankAutomationMode.APPLY;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class ExpenseTrashRestoreStrategyTest {

    private static final Long EXPENSE_ID = 10L;
    private static final Long USER_ID = 2L;
    private static final BigDecimal AMOUNT = BigDecimal.TEN;

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

    private Expense expense;
    private TrashItem item;

    @BeforeEach
    void setUp() {
        expense = Expense.builder()
                .id(EXPENSE_ID)
                .userId(USER_ID)
                .amount(AMOUNT)
                .category(ExpenseCategory.values()[0])
                .build();
        item = TrashItem.builder()
                .entityType(TrashEntityType.EXPENSE)
                .entityId(EXPENSE_ID)
                .userId(USER_ID)
                .build();
        when(expenseRepository.findTrashedByIdAndUserIdForUpdate(EXPENSE_ID, USER_ID))
                .thenReturn(Optional.of(expense));
    }

    @Nested
    class Restore {
        @Test
        void shouldRestoreExpenseAndReverseFinancialEffectsWhenFlagIsTrue() {
            when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of());
            item.setFinancialEffectsReverted(true);

            strategy.restore(item);

            verify(expenseRepository).save(expense);
            verify(walletService).removeBalanceFromWallet(USER_ID, AMOUNT, WITHDRAWAL, EXPENSE);
            verify(roundUpService).handleExpenseForRoundUp(USER_ID, EXPENSE_ID, APPLY);
        }

        @Test
        void shouldRestoreExpenseWithoutChangingWalletWhenFinancialEffectsWereNotReverted() {
            when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of());
            item.setFinancialEffectsReverted(false);

            strategy.restore(item);

            verify(expenseRepository).save(expense);
            verifyNoInteractions(walletService, roundUpService);
        }

        @Test
        void shouldNotRestoreFinancialEffectsWhenFlagIsNull() {
            when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of());
            strategy.restore(item);

            verifyNoInteractions(walletService, roundUpService);
        }

        @Test
        void shouldPublishLimitStatsUsingSingleEventSaveWhenActiveLimitsExist() {
            Long limitId = 15L;
            Limit limit = Limit.builder().id(limitId).build();
            LimitStatsDto stats = new LimitStatsDto(limitId, PeriodType.MONTHLY, ExpenseCategory.values()[0],
                    BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN, LimitStatus.NONE,
                    LocalDate.now());
            when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of(limit));
            when(limitCalculateService.calculateLimitStats(eq(List.of(limit)), eq(USER_ID), any(LocalDate.class)))
                    .thenReturn(List.of(stats));

            strategy.restore(item);

            verify(outboxService).save(eq("Limit"), eq(limitId.toString()), eq("limit.calculate-stats"),
                    any(LimitStatsEvent.class));
        }

        @Test
        void shouldThrowExceptionWhenExpenseIsMissingFromTrash() {
            when(expenseRepository.findTrashedByIdAndUserIdForUpdate(EXPENSE_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> strategy.restore(item));
        }
    }
}
