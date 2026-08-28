package com.finovara.financeservice.util.transaction.expense;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.sharedaccount.expense.model.SharedExpense;
import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SharedExpenseManagerService {
    private final SharedExpenseRepository sharedExpenseRepository;

    public SharedExpense getSharedExpenseOrThrow(Long expenseId) {
        return sharedExpenseRepository.findById(expenseId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Expense not found"));

    }
}
