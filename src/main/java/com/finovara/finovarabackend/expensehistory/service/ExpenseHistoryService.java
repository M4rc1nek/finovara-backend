package com.finovara.finovarabackend.expensehistory.service;

import com.finovara.finovarabackend.exception.UserNotFoundException;
import com.finovara.finovarabackend.expense.dto.ExpenseDTO;
import com.finovara.finovarabackend.expense.mapper.ExpenseMapper;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.util.service.time.SpentInPeriodService;
import com.finovara.finovarabackend.util.service.user.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseHistoryService {
    private final ExpenseRepository expenseRepository;
    private final SpentInPeriodService spentInPeriodService;
    private final UserManagerService userManagerService;
    private final ExpenseMapper expenseMapper;

    public List<ExpenseDTO> getExpenseByCategory(String email, ExpenseCategory category) {
        User user = userManagerService.getUserByEmailOrThrow(email);


        LocalDate startMonth = spentInPeriodService.today().withDayOfMonth(1);
        LocalDate today = spentInPeriodService.today();

        List<Expense> expenses = expenseRepository.findAllByUserAndCategoryAndDateRange(user.getId(), category, startMonth, today);

        return expenses.stream()
                .map(expenseMapper::mapExpenseToDTO)
                .toList();
    }
}
