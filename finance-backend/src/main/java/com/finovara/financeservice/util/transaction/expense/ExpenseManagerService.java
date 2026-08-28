package com.finovara.financeservice.util.transaction.expense;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExpenseManagerService {
    private final ExpenseRepository expenseRepository;

    public Expense getExpenseByUserIdOrThrow(Long expenseId, Long userId) {
        return expenseRepository.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Expense not found"));
    }

    public Expense getExpenseByIdOrThrow(Long expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Expense not found"));

    }
}
