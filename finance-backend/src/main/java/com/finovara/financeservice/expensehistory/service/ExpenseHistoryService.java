package com.finovara.authbackend.expensehistory.service;

import com.finovara.authbackend.expense.dto.ExpenseDto;
import com.finovara.authbackend.expense.mapper.ExpenseMapper;
import com.finovara.authbackend.expense.model.Expense;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.PeriodType;
import com.finovara.authbackend.util.periodbalance.FinancialPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseHistoryService {
    private final FinancialPeriodService financialPeriodService;
    private final ExpenseMapper expenseMapper;

    public List<ExpenseDto> getExpenseByCategory(Long userId, PeriodType periodType, ExpenseCategory category) {
        List<Expense> expenses = financialPeriodService.getExpensesInPeriodByCategory(userId, periodType, category);

        return expenses.stream()
                .map(expenseMapper::mapExpenseToDto)
                .toList();
    }
}
