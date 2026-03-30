package com.finovara.finovarabackend.report.finances.categoryspending.service;

import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.report.finances.categoryspending.dto.CategorySpendingDto;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategorySpendingService {
    private final UserManagerService userManagerService;
    private final FinancialPeriodService financialPeriodService;

    public CategorySpendingDto getCategorySpendingReport(String email, ExpenseCategory category, PeriodType periodType) {

        User user = userManagerService.getUserByEmailOrThrow(email);

        BigDecimal summedExpenses = financialPeriodService.getExpensesSum(user.getId(), periodType);
        List<Expense> expenseCategory = financialPeriodService.getExpensesInPeriodByCategory(user.getId(), periodType, category);

        BigDecimal summedExpenseCategory = expenseCategory.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percentage = BigDecimal.ZERO;

        if (summedExpenses.compareTo(BigDecimal.ZERO) > 0) {
            percentage = summedExpenseCategory
                    .multiply(BigDecimal.valueOf(100))
                    .divide(summedExpenses, 2, RoundingMode.HALF_UP);
        }
        return new CategorySpendingDto(percentage, category);
    }
}
