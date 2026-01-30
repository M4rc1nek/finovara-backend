package com.finovara.finovarabackend.util.service.expense;

import com.finovara.finovarabackend.exception.ExpenseNotFoundException;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExpenseManagerService {
    private final ExpenseRepository expenseRepository;

    public Expense getExpenseByUserIdOrThrow(Long expenseId, Long userId) {
        return expenseRepository.findByIdAndUserAssignedId(expenseId, userId)
                .orElseThrow(() -> new ExpenseNotFoundException("Expense not found"));
    }

    public Expense getExpenseByIdOrThrow(Long expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ExpenseNotFoundException("Expense not found"));

    }
}
