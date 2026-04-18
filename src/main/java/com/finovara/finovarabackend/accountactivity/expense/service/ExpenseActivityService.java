package com.finovara.finovarabackend.accountactivity.expense.service;

import com.finovara.finovarabackend.accountactivity.core.AccountActivityCore;
import com.finovara.finovarabackend.accountactivity.expense.dto.ExpenseActivityDto;
import com.finovara.finovarabackend.accountactivity.expense.model.ExpenseActivityType;
import com.finovara.finovarabackend.accountactivity.expense.mapper.ExpenseActivityMapper;
import com.finovara.finovarabackend.accountactivity.expense.model.ExpenseActivity;
import com.finovara.finovarabackend.accountactivity.expense.repository.ExpenseActivityRepository;
import com.finovara.finovarabackend.util.model.SortType;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExpenseActivityService extends AccountActivityCore<ExpenseActivity, ExpenseActivityDto, Expense> {

    @Value("${user-activity.expense.page-size}")
    private int pageSize;

    private final ExpenseActivityRepository expenseActivityRepository;
    private final ExpenseActivityMapper expenseActivityMapper;

    public ExpenseActivityService(UserManagerService userManagerService,
                                  ExpenseActivityRepository expenseActivityRepository,
                                  ExpenseActivityMapper expenseActivityMapper) {
        super(userManagerService);
        this.expenseActivityRepository = expenseActivityRepository;
        this.expenseActivityMapper = expenseActivityMapper;
    }

    @Transactional
    public void createExpenseActivity(String email, ExpenseActivityType expenseActivityType, Expense expense) {
        ExpenseActivity expenseActivity = buildActivity(email, expense);
        expenseActivity.setType(expenseActivityType);
        expenseActivityRepository.save(expenseActivity);
    }

    @Transactional
    public void updateExpenseActivity(String email, ExpenseActivityType expenseActivityType, Expense expense, BigDecimal previousAmount, ExpenseCategory previousCategory) {
        ExpenseActivity expenseActivity = buildActivity(email, expense);
        expenseActivity.setType(expenseActivityType);
        expenseActivity.setPreviousCategory(previousCategory);
        expenseActivity.setPreviousAmount(previousAmount);
        expenseActivityRepository.save(expenseActivity);
    }

    public List<ExpenseActivityDto> getExpenseActivity(String email, SortType sort) {
        return getActivities(email, sort, pageSize);
    }

    @Override
    protected List<ExpenseActivity> getRepositoryFindByUserEmail(String email, Pageable pageable) {
        return expenseActivityRepository.findByUserAssignedEmail(email, pageable);
    }

    @Override
    protected ExpenseActivityDto mapToDto(ExpenseActivity entity) {
        return expenseActivityMapper.mapToExpenseActivity(entity);
    }

    @Override
    protected ExpenseActivity buildActivity(String email, Expense expense) {
        return ExpenseActivity.builder()
                .userAssigned(getUser(email))
                .amount(expense.getAmount())
                .category(expense.getCategory())
                .createdAt(LocalDateTime.now())
                .build();
    }

}
