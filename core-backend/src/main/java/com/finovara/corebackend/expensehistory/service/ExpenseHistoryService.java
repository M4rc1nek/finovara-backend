package com.finovara.corebackend.expensehistory.service;

import com.finovara.corebackend.expense.dto.ExpenseDto;
import com.finovara.corebackend.expense.mapper.ExpenseMapper;
import com.finovara.corebackend.expense.model.Expense;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.corebackend.user.model.User;
import com.finovara.contracts.model.PeriodType;
import com.finovara.corebackend.util.periodbalance.FinancialPeriodService;
import com.finovara.corebackend.util.user.service.UserManagerService;
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
