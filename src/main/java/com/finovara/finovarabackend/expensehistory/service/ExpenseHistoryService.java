package com.finovara.finovarabackend.expensehistory.service;

import com.finovara.finovarabackend.expense.dto.ExpenseDto;
import com.finovara.finovarabackend.expense.mapper.ExpenseMapper;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.periodbalance.FinancialPeriodService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseHistoryService {
    private final UserManagerService userManagerService;
    private final FinancialPeriodService financialPeriodService;
    private final ExpenseMapper expenseMapper;

    public List<ExpenseDto> getExpenseByCategory(Long userId, PeriodType periodType, ExpenseCategory category) {
        User user = userManagerService.getUserByIdOrThrow(userId);

        List<Expense> expenses = financialPeriodService.getExpensesInPeriodByCategory(user.getId(), periodType, category);

        return expenses.stream()
                .map(expenseMapper::mapExpenseToDto)
                .toList();
    }
}
