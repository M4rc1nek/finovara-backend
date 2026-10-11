package com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.service;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.dto.AccountChangesActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.model.AccountChangesActivity;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.repository.AccountChangesActivityRepository;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.model.AccountChangeArchive;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.service.AccountChangeArchiveService;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.core.SecurityActivityCore;
import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.contracts.mainaccount.activity.event.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.user.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.user.datadeletable.UserDataDeletable;
import com.finovara.contracts.util.SortType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountChangesActivityService extends SecurityActivityCore<AccountChangesActivity, AccountChangeArchive, AccountChangesActivityDto> implements UserDataDeletable {

    private final AccountChangesActivityRepository accountChangesActivityRepository;
    private final AccountChangeArchiveService accountChangeArchiveService;
    private final AuthBackendClient authBackendClient;

    @Value("${user-activity.account-changes.page-size}")
    private int pageSize;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "accountChangesActivity", allEntries = true),
            @CacheEvict(value = "accountChangeArchive", allEntries = true)
    })
    public void handleEvent(AccountChangesActivityEvent event) {
        AccountChangesActivity activity = AccountChangesActivity.builder()
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

    @Cacheable(value = "accountChangesActivity", key = "#userId + ':' + #sort")
    public List<AccountChangesActivityDto> getAccountChangesActivity(Long userId, SortType sort) {
        return getActivities(userId, sort, pageSize);
    }

    public void confirmPassword(Long userId, ConfirmPasswordDto confirmPasswordDto) {
        authBackendClient.verifyPassword(userId, confirmPasswordDto);
    }

    @Override
    protected List<AccountChangesActivity> getRepositoryFindByUserId(Long userId, Pageable pageable) {
        return accountChangesActivityRepository.findByUserId(userId, pageable);
    }

    @Override
    protected AccountChangesActivityDto mapToDto(AccountChangesActivity a) {
        return new AccountChangesActivityDto(a.getType(), a.getCreatedAt(),
                a.getBrowser(), a.getIpAddress(), a.getLocation());
    }

    @Override
    protected void saveActivity(AccountChangesActivity activity) {
        accountChangesActivityRepository.save(activity);
    }

    @Override
    protected long countActivities(Long userId) {
        return accountChangesActivityRepository.countAccountChangesByUserId(userId);
    }

    @Override
    protected List<AccountChangesActivity> findActivitiesToArchive(Long userId, int pageSize) {
        return accountChangesActivityRepository.findFewByUserId(userId, PageRequest.of(0, pageSize));
    }

    @Override
    protected AccountChangeArchive mapToArchive(AccountChangesActivity activity) {
        return accountChangeArchiveService.mapToArchive(activity);
    }

    @Override
    protected void archive(List<AccountChangeArchive> archives) {
        accountChangeArchiveService.archive(archives);
    }

    @Override
    protected void deleteActivities(List<AccountChangesActivity> activities) {
        accountChangesActivityRepository.deleteAll(activities);
    }

    @Override
    @Transactional
    @CacheEvict(value = "accountChangesActivity", allEntries = true)
    public void deleteByUserId(Long userId) {
        accountChangesActivityRepository.deleteByUserId(userId);
        log.info("Deleted account changes activity for userId={}", userId);
    }
}