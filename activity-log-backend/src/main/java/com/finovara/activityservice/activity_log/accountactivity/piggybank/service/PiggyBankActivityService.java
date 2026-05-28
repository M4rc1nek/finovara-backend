package com.finovara.activityservice.activity_log.accountactivity.piggybank.service;

import com.finovara.activityservice.activity_log.accountactivity.core.AccountActivityCore;
import com.finovara.activityservice.activity_log.accountactivity.piggybank.dto.PiggyBankActivityDto;
import com.finovara.activityservice.activity_log.accountactivity.piggybank.mapper.PiggyBankActivityMapper;
import com.finovara.activityservice.activity_log.accountactivity.piggybank.model.PiggyBankActivity;
import com.finovara.activityservice.activity_log.accountactivity.piggybank.repository.PiggyBankActivityRepository;
import com.finovara.contracts.event.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.model.SortType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PiggyBankActivityService extends AccountActivityCore<PiggyBankActivity, PiggyBankActivityDto> {

    @Value("${user-activity.piggy-bank.page-size}")
    private int pageSize;

    private final PiggyBankActivityRepository piggyBankActivityRepository;
    private final PiggyBankActivityMapper piggyBankActivityMapper;

    @Transactional
    public void handleEvent(PiggyBankActivityEvent event) {
        PiggyBankActivity piggyBankActivity = PiggyBankActivity.builder()
                .userId(event.userId())
                .piggyBankName(event.name())
                .activityType(event.type())
                .goalType(event.goalType())
                .goalAmount(event.goalAmount())
                .amountPaid(event.amountPaid())
                .createdAt(event.occurredAt())
                .build();

        piggyBankActivityRepository.save(piggyBankActivity);
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
}
