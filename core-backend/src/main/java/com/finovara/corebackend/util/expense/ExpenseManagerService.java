package com.finovara.corebackend.util.expense;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.corebackend.expense.model.Expense;
import com.finovara.corebackend.expense.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExpenseManagerService {
    private final ExpenseRepository expenseRepository;

    public Expense getExpenseByUserIdOrThrow(Long expenseId, Long userId) {
        return expenseRepository.findByIdAndUserAssignedId(expenseId, userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Expense not found"));
    }

    public Expense getExpenseByIdOrThrow(Long expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Expense not found"));

    }
}
