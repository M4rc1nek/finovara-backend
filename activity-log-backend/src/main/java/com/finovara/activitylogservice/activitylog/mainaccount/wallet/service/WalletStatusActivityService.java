package com.finovara.activitylogservice.activitylog.mainaccount.wallet.service;

import com.finovara.activitylogservice.activitylog.mainaccount.core.AccountActivityCore;
import com.finovara.activitylogservice.activitylog.mainaccount.core.DateRange;
import com.finovara.activitylogservice.activitylog.mainaccount.wallet.dto.WalletStatusActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.wallet.dto.WalletStatusActivityFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.wallet.mapper.WalletStatusActivityMapper;
import com.finovara.activitylogservice.activitylog.mainaccount.wallet.model.WalletStatusActivity;
import com.finovara.activitylogservice.activitylog.mainaccount.wallet.repository.WalletStatusActivityRepository;
import com.finovara.contracts.mainaccount.activity.event.wallet.WalletStatusActivityEvent;
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
public class WalletStatusActivityService extends AccountActivityCore<WalletStatusActivity, WalletStatusActivityDto> implements UserDataDeletable {

    @Value("${user-activity.wallet-status.page-size}")
    private int pageSize;

    private final WalletStatusActivityRepository walletStatusActivityRepository;
    private final WalletStatusActivityMapper walletStatusActivityMapper;

    @Transactional
    public void handleEvent(WalletStatusActivityEvent event) {
        WalletStatusActivity activity = WalletStatusActivity.builder()
                .userId(event.userId())
                .type(event.type())
                .reason(event.reason())
                .amount(event.amount())
                .createdAt(event.occurredAt())
                .build();

        walletStatusActivityRepository.save(activity);
        log.info("Created wallet activity type={}, reason={}, userId={}",
                event.type(), event.reason(), event.userId());
    }

    @Cacheable(value = "walletStatusActivity", key = "#userId + ':' + #sort + ':' + #page")
    public List<WalletStatusActivityDto> getWalletActivity(Long userId, SortType sort, int page) {
        return getActivities(userId, sort, page, pageSize);
    }

    public List<WalletStatusActivityDto> filterWalletActivity(Long userId, WalletStatusActivityFilterDto filter, int page) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.unsorted());
        DateRange created = DateRange.of(filter.createdAt());
        return walletStatusActivityRepository.findFilteredActivities(userId, filter, created.from(), created.to(), pageable)
                .stream()
                .map(walletStatusActivityMapper::mapToDto)
                .toList();
    }

    @Override
    protected List<WalletStatusActivity> getRepositoryFindByUserId(Long userId, Pageable pageable) {
        return walletStatusActivityRepository.findByUserId(userId, pageable);
    }

    @Override
    protected WalletStatusActivityDto mapToDto(WalletStatusActivity entity) {
        return walletStatusActivityMapper.mapToDto(entity);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        List<WalletStatusActivity> activities = walletStatusActivityRepository.findAllByUserId(userId);
        walletStatusActivityRepository.deleteAll(activities);
        log.info("Deleted wallet activity for userId={}", userId);
    }
}
