package com.finovara.activityservice.activitylog.accountactivity.expense.service;

import com.finovara.activityservice.activitylog.accountactivity.core.AccountActivityCore;
import com.finovara.activityservice.activitylog.accountactivity.expense.dto.ExpenseActivityDto;
import com.finovara.activityservice.activitylog.accountactivity.expense.mapper.ExpenseActivityMapper;
import com.finovara.activityservice.activitylog.accountactivity.expense.model.ExpenseActivity;
import com.finovara.activityservice.activitylog.accountactivity.expense.repository.ExpenseActivityRepository;
import com.finovara.activityservice.activitylog.datadeletable.UserDataDeletable;
import com.finovara.contracts.event.activity.expense.ExpenseActivityEvent;
import com.finovara.contracts.model.SortType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseActivityService extends AccountActivityCore<ExpenseActivity, ExpenseActivityDto> implements UserDataDeletable {

    @Value("${user-activity.expense.page-size}")
    private int pageSize;

    private final ExpenseActivityRepository expenseActivityRepository;
    private final ExpenseActivityMapper expenseActivityMapper;

    @Transactional
    public void handleEvent(ExpenseActivityEvent event) {
        ExpenseActivity expenseActivity = ExpenseActivity.builder()
                .userId(event.userId())
                .type(event.type())
                .amount(event.amount())
                .category(event.category())
                .previousAmount(event.previousAmount())
                .previousCategory(event.previousCategory())
                .createdAt(event.occurredAt())
                .build();

        expenseActivityRepository.save(expenseActivity);
        log.info("Created activity type: {}, userId: {}", event.type(), event.userId());

    }

    public List<ExpenseActivityDto> getExpenseActivity(Long userId, SortType sort) {
        return getActivities(userId, sort, pageSize);
    }

    @Override
    protected List<ExpenseActivity> getRepositoryFindByUserId(Long userId, Pageable pageable) {
        return expenseActivityRepository.findByUserId(userId, pageable);
    }

    @Override
    protected ExpenseActivityDto mapToDto(ExpenseActivity entity) {
        return expenseActivityMapper.mapToExpenseActivity(entity);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        expenseActivityRepository.deleteByUserId(userId);
        log.info("Deleted expense activity for userId={}", userId);
    }
}
