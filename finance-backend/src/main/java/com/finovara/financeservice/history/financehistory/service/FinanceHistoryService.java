package com.finovara.financeservice.history.financehistory.service;

import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.contracts.util.model.RevenueCategory;
import com.finovara.financeservice.expense.dto.ExpenseDto;
import com.finovara.financeservice.expense.mapper.ExpenseMapper;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.revenue.dto.RevenueDto;
import com.finovara.financeservice.revenue.mapper.RevenueMapper;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.util.periodbalance.FinancialPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FinanceHistoryService {
    private final FinancialPeriodService financialPeriodService;
    private final ExpenseMapper expenseMapper;
    private final RevenueMapper revenueMapper;

    @Cacheable(value = "historyByCategory:revenue", key = "#userId + ':' + #periodType + ':' + #category")
    public List<RevenueDto> getRevenueByCategory(Long userId, PeriodType periodType, RevenueCategory category) {
        List<Revenue> revenues = financialPeriodService.getRevenuesInPeriodByCategory(userId, periodType, category);

        return revenues.stream()
                .map(revenueMapper::mapRevenueToDto)
                .toList();
    }

    @Cacheable(value = "historyByCategory:expense", key = "#userId + ':' + #periodType + ':' + #category")
    public List<ExpenseDto> getExpenseByCategory(Long userId, PeriodType periodType, ExpenseCategory category) {
        List<Expense> expenses = financialPeriodService.getExpensesInPeriodByCategory(userId, periodType, category);

        return expenses.stream()
                .map(expenseMapper::mapExpenseToDto)
                .toList();
    }

}
