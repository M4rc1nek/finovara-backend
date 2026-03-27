package com.finovara.finovarabackend.report.finances.categoryspending.service;

import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.finances.dto.CategorySpendingDto;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportsCategorySpendingService {
    private final UserManagerService userManagerService;
    private final ExpenseRepository expenseRepository;

    public CategorySpendingDto getCategorySpendingReport(String email, ExpenseCategory category) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        LocalDate startMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate today = LocalDate.now();

        List<Expense> expenses = expenseRepository.findAllByUserAndCategoryAndDateRange(user.getId(), category, startMonth, today);

        BigDecimal sumExpensesAmount = Optional.ofNullable(expenseRepository.sumExpensesByUserAndDateRange(user.getId(), startMonth, today))
                .orElse(BigDecimal.ZERO);

        BigDecimal sumExpensesCategory = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percentage = BigDecimal.ZERO;

        if (sumExpensesAmount.compareTo(BigDecimal.ZERO) > 0) {
            percentage = sumExpensesCategory
                    .multiply(BigDecimal.valueOf(100))
                    .divide(sumExpensesAmount, 2, RoundingMode.HALF_UP);
        }
        return new CategorySpendingDto(percentage, category);
    }
}
