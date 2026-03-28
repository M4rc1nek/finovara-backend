package com.finovara.finovarabackend.expensehistory.service;

import com.finovara.finovarabackend.expense.dto.ExpenseDTO;
import com.finovara.finovarabackend.expense.mapper.ExpenseMapper;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseHistoryService {
    private final UserManagerService userManagerService;
    private final FinancialPeriodService financialPeriodService;
    private final ExpenseMapper expenseMapper;

    public List<ExpenseDTO> getExpenseByCategory(String email, PeriodType periodType, ExpenseCategory category) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        List<Expense> expenses = switch (periodType) {
            case DAILY -> financialPeriodService.getExpensesInPeriodByCategory(user.getId(), PeriodType.DAILY, category);
            case WEEKLY -> financialPeriodService.getExpensesInPeriodByCategory(user.getId(), PeriodType.WEEKLY, category);
            case MONTHLY -> financialPeriodService.getExpensesInPeriodByCategory(user.getId(), PeriodType.MONTHLY, category);
        };

        return expenses.stream()
                .map(expenseMapper::mapExpenseToDTO)
                .toList();
    }
}
