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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrashMoverService {

    private final ExpenseTrashMover expenseTrashMover;
    private final RevenueTrashMover revenueTrashMover;
    private final LimitTrashMover limitTrashMover;
    private final PiggyBankTrashMover piggyBankTrashMover;
    private final FundReservationTrashMover fundReservationTrashMover;

    public void moveExpenseToTrash(Expense expense, Long userId, boolean revertFinancialEffects) {
        expenseTrashMover.move(expense, userId, revertFinancialEffects);
    }

    public void moveExpensesToTrash(List<Expense> expenses) {
        expenseTrashMover.moveAll(expenses);
    }

    public void moveRevenueToTrash(Revenue revenue, Long userId, boolean revertFinancialEffects) {
        revenueTrashMover.move(revenue, userId, revertFinancialEffects);
    }

    public void moveRevenuesToTrash(List<Revenue> revenues) {
        revenueTrashMover.moveAll(revenues);
    }

    public void moveLimitToTrash(Limit limit, Long userId) {
        limitTrashMover.move(limit, userId);
    }

    public void movePiggyBankToTrash(PiggyBank piggyBank, Long userId) {
        piggyBankTrashMover.move(piggyBank, userId);
    }

    public void moveFundReservationToTrash(FundReservation reservation, Wallet wallet, Long userId) {
        fundReservationTrashMover.move(reservation, wallet, userId);
    }
}
