package com.finovara.finovarabackend.accountactivity.expense.service;

import com.finovara.finovarabackend.accountactivity.expense.dto.ExpenseActivityDto;
import com.finovara.finovarabackend.accountactivity.expense.model.ExpenseActivitySort;
import com.finovara.finovarabackend.accountactivity.expense.model.ExpenseActivityType;
import com.finovara.finovarabackend.accountactivity.expense.mapper.ExpenseActivityMapper;
import com.finovara.finovarabackend.accountactivity.expense.model.ExpenseActivity;
import com.finovara.finovarabackend.accountactivity.expense.repository.ExpenseActivityRepository;
import com.finovara.finovarabackend.config.TimeConfig;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseActivityService {

    @Value("${user-activity.expense.page-size}")
    private int pageSize;

    private final UserManagerService userManagerService;
    private final TimeConfig timeConfig;
    private final ExpenseActivityRepository expenseActivityRepository;
    private  final ExpenseActivityMapper expenseActivityMapper;

    @Transactional
    public void createExpenseActivity(String email, ExpenseActivityType expenseActivityType, Expense expense) {
        buildExpenseActivity(email, expenseActivityType, expense);
    }

    @Transactional
    public void updateExpenseActivity(String email, ExpenseActivityType expenseActivityType, Expense expense, BigDecimal previousAmount, ExpenseCategory previousCategory) {
        ExpenseActivity expenseActivity = buildExpenseActivity(email, expenseActivityType, expense);
        expenseActivity.setPreviousCategory(previousCategory);
        expenseActivity.setPreviousAmount(previousAmount);
    }

    // testy dla reszty expense + reszta accountactivity

    public List<ExpenseActivityDto> getExpenseActivity(String email, ExpenseActivitySort sort) {

        Pageable pageable = switch (sort) {
            case NEWEST -> PageRequest.of(0, pageSize, Sort.by("date").descending());
            case OLDEST -> PageRequest.of(0, pageSize, Sort.by("date").ascending());
            case AMOUNT_DESC -> PageRequest.of(0, pageSize, Sort.by("amount").descending());
            case AMOUNT_ASC -> PageRequest.of(0, pageSize, Sort.by("amount").ascending());
        };

        return expenseActivityRepository.findByUserAssignedEmail(email, pageable)
                .stream().map(expenseActivityMapper::mapToExpenseActivity)
                .toList();
    }

    @Transactional
    private ExpenseActivity buildExpenseActivity(String email, ExpenseActivityType expenseActivityType, Expense expense) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        ExpenseActivity expenseActivity = ExpenseActivity.builder()
                .userAssigned(user)
                .type(expenseActivityType)
                .amount(expense.getAmount())
                .category(expense.getCategory())
                .date(LocalDateTime.now(timeConfig.clock()))
                .build();
        expenseActivityRepository.save(expenseActivity);
        return expenseActivity;
    }

}
