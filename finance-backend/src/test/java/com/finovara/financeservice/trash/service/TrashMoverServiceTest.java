package com.finovara.financeservice.trash.service;

import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.trash.service.action.expense.ExpenseTrashMover;
import com.finovara.financeservice.trash.service.action.fundreservation.FundReservationTrashMover;
import com.finovara.financeservice.trash.service.action.limit.LimitTrashMover;
import com.finovara.financeservice.trash.service.action.piggybank.PiggyBankTrashMover;
import com.finovara.financeservice.trash.service.action.revenue.RevenueTrashMover;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.reservation.model.FundReservation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrashMoverServiceTest {

    private static final Long USER_ID = 17L;

    @Mock
    private ExpenseTrashMover expenseTrashMover;
    @Mock
    private RevenueTrashMover revenueTrashMover;
    @Mock
    private LimitTrashMover limitTrashMover;
    @Mock
    private PiggyBankTrashMover piggyBankTrashMover;
    @Mock
    private FundReservationTrashMover fundReservationTrashMover;
    @Mock
    private Expense expense;
    @Mock
    private Revenue revenue;
    @Mock
    private Limit limit;
    @Mock
    private PiggyBank piggyBank;
    @Mock
    private FundReservation reservation;
    @Mock
    private Wallet wallet;

    @InjectMocks
    private TrashMoverService service;

    @Nested
    class MoveExpense {
        @Test
        void shouldDelegateExpenseMovementToExpenseStrategy() {
            service.moveExpenseToTrash(expense, USER_ID, true);

            verify(expenseTrashMover).move(expense, USER_ID, true);
        }

        @Test
        void shouldDelegateBatchExpenseMovementToExpenseStrategy() {
            List<Expense> expenses = List.of(expense);

            service.moveExpensesToTrash(expenses);

            verify(expenseTrashMover).moveAll(expenses);
        }
    }

    @Nested
    class MoveRevenue {
        @Test
        void shouldDelegateRevenueMovementToRevenueStrategy() {
            service.moveRevenueToTrash(revenue, USER_ID, false);

            verify(revenueTrashMover).move(revenue, USER_ID, false);
        }

        @Test
        void shouldDelegateBatchRevenueMovementToRevenueStrategy() {
            List<Revenue> revenues = List.of(revenue);

            service.moveRevenuesToTrash(revenues);

            verify(revenueTrashMover).moveAll(revenues);
        }
    }

    @Nested
    class MoveOtherEntities {
        @Test
        void shouldDelegateLimitMovementToLimitStrategy() {
            service.moveLimitToTrash(limit, USER_ID);

            verify(limitTrashMover).move(limit, USER_ID);
        }

        @Test
        void shouldDelegatePiggyBankMovementToPiggyBankStrategy() {
            service.movePiggyBankToTrash(piggyBank, USER_ID);

            verify(piggyBankTrashMover).move(piggyBank, USER_ID);
        }

        @Test
        void shouldDelegateReservationMovementToReservationStrategy() {
            service.moveFundReservationToTrash(reservation, wallet, USER_ID);

            verify(fundReservationTrashMover).move(reservation, wallet, USER_ID);
        }
    }
}
