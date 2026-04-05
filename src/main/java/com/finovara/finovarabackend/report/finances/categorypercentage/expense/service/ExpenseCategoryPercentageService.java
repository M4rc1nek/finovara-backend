package com.finovara.finovarabackend.report.finances.categorypercentage.expense.service;

import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.report.finances.categorypercentage.expense.dto.ExpenseCategoryPercentageDto;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.percentage.CalculatePercentage;
import com.finovara.finovarabackend.util.periodbalance.FinancialPeriodService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseCategoryPercentageService {
    private final UserManagerService userManagerService;
    private final FinancialPeriodService financialPeriodService;

    public ExpenseCategoryPercentageDto getExpensePercentageByCategoryReport(String email, ExpenseCategory category, PeriodType periodType) {

        User user = userManagerService.getUserByEmailOrThrow(email);

        BigDecimal totalExpenses = financialPeriodService.getExpensesSum(user.getId(), periodType);
        List<Expense> expensesInCategory = financialPeriodService.getExpensesInPeriodByCategory(user.getId(), periodType, category);

        BigDecimal totalExpensesInCategory = expensesInCategory.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percentage = CalculatePercentage.calculatePercentage(totalExpensesInCategory, totalExpenses);

        return new ExpenseCategoryPercentageDto(percentage, category);
    }
}