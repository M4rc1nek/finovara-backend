package com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.service;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.dto.AccountChangeActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.mapper.AccountChangeActivityMapper;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.model.AccountChangeActivity;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.repository.AccountChangeActivityRepository;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.model.AccountChangeArchive;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.service.AccountChangeArchiveService;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.core.SecurityActivityCore;
import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.contracts.mainaccount.activity.event.secure.accountchange.activity.AccountChangeActivityEvent;
import com.finovara.contracts.user.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.user.datadeletable.UserDataDeletable;
import com.finovara.contracts.util.SortType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountChangeActivityService extends SecurityActivityCore<AccountChangeActivity, AccountChangeArchive, AccountChangeActivityDto> implements UserDataDeletable {

    private final AccountChangeActivityRepository accountChangeActivityRepository;
    private final AccountChangeArchiveService accountChangeArchiveService;
    private final AccountChangeActivityMapper accountChangeActivityMapper;
    private final AuthBackendClient authBackendClient;

    @Value("${user-activity.account-changes.page-size}")
    private int pageSize;

    @Transactional
    public void handleEvent(AccountChangeActivityEvent event) {
        AccountChangeActivity activity = AccountChangeActivity.builder()
                .userId(event.userId())
                .type(event.type())
                .browser(event.browser())
                .ipAddress(event.ipAddress())
                .location(event.location())
                .createdAt(event.occurredAt())
                .build();

        saveActivity(activity);
        log.info("Created activity type: {}, userId: {}", event.type(), event.userId());
        moveToArchive(event.userId(), pageSize);
    }

    @Cacheable(value = "accountChangeActivity", key = "#userId + ':' + #sort")
    public List<AccountChangeActivityDto> getAccountChangeActivity(Long userId, SortType sort) {
        return getActivities(userId, sort, pageSize);
    }

    public void confirmPassword(Long userId, ConfirmPasswordDto confirmPasswordDto) {
        authBackendClient.verifyPassword(userId, confirmPasswordDto);
    }

    @Override
    protected List<AccountChangeActivity> getRepositoryFindByUserId(Long userId, Pageable pageable) {
        return accountChangeActivityRepository.findByUserId(userId, pageable);
    }

    @Override
    protected AccountChangeActivityDto mapToDto(AccountChangeActivity accountChangeActivity) {
        return accountChangeActivityMapper.mapToDto(accountChangeActivity);
    }

    @Override
    protected void saveActivity(AccountChangeActivity activity) {
        accountChangeActivityRepository.save(activity);
    }

    @Override
    protected long countActivities(Long userId) {
        return accountChangeActivityRepository.countAccountChangesByUserId(userId);
    }

    @Override
    protected List<AccountChangeActivity> findActivitiesToArchive(Long userId, int pageSize) {
        return accountChangeActivityRepository.findFewByUserId(userId, PageRequest.of(0, pageSize));
    }

    @Override
    protected AccountChangeArchive mapToArchive(AccountChangeActivity activity) {
        return accountChangeArchiveService.mapToArchive(activity);
    }

    @Override
    protected void archive(List<AccountChangeArchive> archives) {
        accountChangeArchiveService.archive(archives);
    }

    @Override
    protected void deleteActivities(List<AccountChangeActivity> activities) {
        accountChangeActivityRepository.deleteAll(activities);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        accountChangeActivityRepository.deleteByUserId(userId);
        log.info("Deleted account changes activity for userId={}", userId);
    }
}