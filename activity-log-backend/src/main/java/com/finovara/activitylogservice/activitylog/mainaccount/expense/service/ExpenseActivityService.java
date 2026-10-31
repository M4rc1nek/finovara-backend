package com.finovara.activitylogservice.activitylog.mainaccount.expense.service;

import com.finovara.activitylogservice.activitylog.mainaccount.core.AccountActivityCore;
import com.finovara.activitylogservice.activitylog.mainaccount.core.DateRange;
import com.finovara.activitylogservice.activitylog.mainaccount.expense.dto.ExpenseActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.expense.dto.ExpenseActivityFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.expense.mapper.ExpenseActivityMapper;
import com.finovara.activitylogservice.activitylog.mainaccount.expense.model.ExpenseActivity;
import com.finovara.activitylogservice.activitylog.mainaccount.expense.repository.ExpenseActivityRepository;
import com.finovara.contracts.mainaccount.activity.event.expense.ExpenseActivityEvent;
import com.finovara.contracts.user.datadeletable.UserDataDeletable;
import com.finovara.contracts.util.SortType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    @Cacheable(value = "expenseActivity", key = "#userId + ':' + #sort + ':' + #page")
    public List<ExpenseActivityDto> getExpenseActivity(Long userId, SortType sort, int page) {
        return getActivities(userId, sort, page, pageSize);
    }

    public List<ExpenseActivityDto> filterExpenseActivity(Long userId, ExpenseActivityFilterDto filter, int page) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.unsorted());
        DateRange created = DateRange.of(filter.createdAt());
        return expenseActivityRepository.findFilteredActivities(userId, filter, created.from(), created.to(), pageable)
                .stream()
                .map(expenseActivityMapper::mapToExpenseActivity)
                .toList();
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
        List<ExpenseActivity> activities = expenseActivityRepository.findAllByUserId(userId);
        expenseActivityRepository.deleteAll(activities);
        log.info("Deleted expense activity for userId={}", userId);
    }
}
