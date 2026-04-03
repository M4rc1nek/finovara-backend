package com.finovara.finovarabackend.report.finances.categoryspending.service;

import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.report.finances.categoryspending.dto.CategorySpendingDto;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.service.calculate.percentage.CalculatePercentage;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpensePercentageByCategory {
    private final UserManagerService userManagerService;
    private final FinancialPeriodService financialPeriodService;

    public CategorySpendingDto getExpensePercentageByCategoryReport(String email, ExpenseCategory category, PeriodType periodType) {

        User user = userManagerService.getUserByEmailOrThrow(email);

        BigDecimal summedExpenses = financialPeriodService.getExpensesSum(user.getId(), periodType);
        List<Expense> expenseCategory = financialPeriodService.getExpensesInPeriodByCategory(user.getId(), periodType, category);

        BigDecimal summedExpenseCategory = expenseCategory.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percentage = CalculatePercentage.calculatePercentage(summedExpenseCategory, summedExpenses);

        return new CategorySpendingDto(percentage, category);
    }
}
