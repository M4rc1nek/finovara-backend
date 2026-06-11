package com.finovara.corebackend.report.finances.categorypercentage.expense.service;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.corebackend.expense.model.Expense;
import com.finovara.corebackend.report.finances.categorypercentage.expense.dto.ExpenseCategoryPercentageDto;
import com.finovara.corebackend.util.percentage.CalculatePercentage;
import com.finovara.corebackend.util.periodbalance.FinancialPeriodService;
import com.finovara.corebackend.util.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseCategoryPercentageService {
    private final UserManagerService userManagerService;
    private final FinancialPeriodService financialPeriodService;

    public ExpenseCategoryPercentageDto getExpensePercentageByCategoryReport(Long userId, ExpenseCategory category, PeriodType periodType) {
        BigDecimal totalExpenses = financialPeriodService.getExpensesSum(userId, periodType);
        List<Expense> expensesInCategory = financialPeriodService.getExpensesInPeriodByCategory(userId, periodType, category);

        BigDecimal totalExpensesInCategory = expensesInCategory.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percentage = CalculatePercentage.calculatePercentage(totalExpensesInCategory, totalExpenses);

        return new ExpenseCategoryPercentageDto(percentage, category);
    }
}