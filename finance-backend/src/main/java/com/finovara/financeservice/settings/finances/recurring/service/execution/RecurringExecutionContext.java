package com.finovara.financeservice.settings.finances.recurring.service.execution;

import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.wallet.model.Wallet;

import java.util.List;

public record RecurringExecutionContext(Wallet wallet, ExpenseSettings expenseSettings, List<Limit> limits,
                                        boolean piggyBankExists) {
}