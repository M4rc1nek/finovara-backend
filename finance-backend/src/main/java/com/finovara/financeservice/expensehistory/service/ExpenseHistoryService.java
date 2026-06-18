package com.finovara.financeservice.expensehistory.service;

import com.finovara.financeservice.expense.dto.ExpenseDto;
import com.finovara.financeservice.expense.mapper.ExpenseMapper;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.PeriodType;
import com.finovara.financeservice.util.periodbalance.FinancialPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseHistoryService {
    private final FinancialPeriodService financialPeriodService;
    private final ExpenseMapper expenseMapper;

    @Cacheable(value = "expense:historyByCategory", key = "#userId + ':' + #periodType + ':' + #category")
    public List<ExpenseDto> getExpenseByCategory(Long userId, PeriodType periodType, ExpenseCategory category) {
        List<Expense> expenses = financialPeriodService.getExpensesInPeriodByCategory(userId, periodType, category);

        return expenses.stream()
                .map(expenseMapper::mapExpenseToDto)
                .toList();
    }
}
