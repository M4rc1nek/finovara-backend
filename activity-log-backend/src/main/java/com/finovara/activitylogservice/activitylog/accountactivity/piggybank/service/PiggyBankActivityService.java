package com.finovara.activitylogservice.activitylog.accountactivity.piggybank.service;

import com.finovara.activitylogservice.activitylog.accountactivity.core.AccountActivityCore;
import com.finovara.activitylogservice.activitylog.accountactivity.piggybank.dto.PiggyBankActivityDto;
import com.finovara.activitylogservice.activitylog.accountactivity.piggybank.mapper.PiggyBankActivityMapper;
import com.finovara.activitylogservice.activitylog.accountactivity.piggybank.model.PiggyBankActivity;
import com.finovara.activitylogservice.activitylog.accountactivity.piggybank.repository.PiggyBankActivityRepository;
import com.finovara.contracts.datadeletable.UserDataDeletable;
import com.finovara.contracts.event.activity.piggybank.PiggyBankActivityEvent;
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
public class PiggyBankLifecycleActivityService extends AccountActivityCore<PiggyBankActivity, PiggyBankActivityDto> implements UserDataDeletable {

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
                .createdAt(event.occurredAt())
                .build();

        piggyBankActivityRepository.save(piggyBankActivity);
        log.info("Created activity type: {}, userId: {}", event.type(), event.userId());
    }

    public List<PiggyBankActivityDto> getPiggyBankActivities(Long userId, SortType sort) {
        return getActivities(userId, sort, pageSize);
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
        piggyBankActivityRepository.deleteByUserId(userId);
        log.info("Deleted piggy bank activity for userId={}", userId);
    }
}
