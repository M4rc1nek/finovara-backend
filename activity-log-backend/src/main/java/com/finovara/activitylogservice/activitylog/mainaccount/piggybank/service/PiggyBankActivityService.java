package com.finovara.activitylogservice.activitylog.mainaccount.piggybank.service;

import com.finovara.activitylogservice.activitylog.mainaccount.core.AccountActivityCore;
import com.finovara.activitylogservice.activitylog.mainaccount.core.DateRange;
import com.finovara.activitylogservice.activitylog.mainaccount.piggybank.dto.PiggyBankActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.piggybank.dto.PiggyBankActivityFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.piggybank.mapper.PiggyBankActivityMapper;
import com.finovara.activitylogservice.activitylog.mainaccount.piggybank.model.PiggyBankActivity;
import com.finovara.activitylogservice.activitylog.mainaccount.piggybank.repository.PiggyBankActivityRepository;
import com.finovara.contracts.mainaccount.activity.event.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.mainaccount.activity.event.piggybank.PiggyBankEditActivityEvent;
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
public class PiggyBankActivityService extends AccountActivityCore<PiggyBankActivity, PiggyBankActivityDto> implements UserDataDeletable {

    @Value("${user-activity.piggy-bank.page-size}")
    private int pageSize;

    private final PiggyBankActivityRepository piggyBankActivityRepository;
    private final PiggyBankActivityMapper piggyBankActivityMapper;

    @Transactional
    public void handleEvent(PiggyBankActivityEvent event) {
        PiggyBankActivity piggyBankActivity = PiggyBankActivity.builder()
                .userId(event.userId())
                .activityType(event.type())
                .piggyBankName(event.name())
                .goalType(event.goalType())
                .goalAmount(event.goalAmount())
                .amountPaid(event.amountPaid())
                .amountPaidOut(event.amountPaidOut())
                .createdAt(event.occurredAt())
                .build();

        piggyBankActivityRepository.save(piggyBankActivity);
        log.info("Created piggy bank activity. type={}, userId={}", event.type(), event.userId());
    }

    @Transactional
    public void handleEditEvent(PiggyBankEditActivityEvent event) {
        PiggyBankActivity piggyBankActivity = PiggyBankActivity.builder()
                .userId(event.userId())
                .activityType(event.type())
                .piggyBankName(event.name())
                .previousPiggyBankName(event.previousName())
                .goalType(event.goalType())
                .previousGoalType(event.previousGoalType())
                .goalAmount(event.goalAmount())
                .previousGoalAmount(event.previousGoalAmount())
                .createdAt(event.occurredAt())
                .build();

        piggyBankActivityRepository.save(piggyBankActivity);
        log.info("Created piggy bank edit activity. type={}, userId={}", event.type(), event.userId());
    }

    @Cacheable(value = "piggyBankActivity", key = "#userId + ':' + #sort + ':' + #page")
    public List<PiggyBankActivityDto> getPiggyBankActivities(Long userId, SortType sort, int page) {
        return getActivities(userId, sort, page, pageSize);
    }

    public List<PiggyBankActivityDto> filterPiggyBankActivity(Long userId, PiggyBankActivityFilterDto filter, int page) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.unsorted());
        DateRange created = DateRange.of(filter.createdAt());
        return piggyBankActivityRepository.findFilteredActivities(userId, filter, created.from(), created.to(), pageable)
                .stream()
                .map(piggyBankActivityMapper::mapToPiggyBankActivity)
                .toList();
    }

    @Override
    protected List<PiggyBankActivity> getRepositoryFindByUserId(Long userId, Pageable pageable) {
        return piggyBankActivityRepository.findByUserId(userId, pageable);
    }

    @Override
    protected PiggyBankActivityDto mapToDto(PiggyBankActivity entity) {
        return piggyBankActivityMapper.mapToPiggyBankActivity(entity);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        List<PiggyBankActivity> activities = piggyBankActivityRepository.findAllByUserId(userId);
        piggyBankActivityRepository.deleteAll(activities);
        log.info("Deleted piggy bank activity for userId={}", userId);
    }
}
