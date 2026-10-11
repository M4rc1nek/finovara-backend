package com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.service;

import com.finovara.activitylogservice.activitylog.mainaccount.core.AccountActivityCore;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.model.AccountChangeActivity;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.dto.AccountChangeArchiveDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.mapper.AccountChangeArchiveMapper;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.model.AccountChangeArchive;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.repository.AccountChangeArchiveRepository;
import com.finovara.contracts.user.datadeletable.UserDataDeletable;
import com.finovara.contracts.util.SortType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountChangeArchiveService extends AccountActivityCore<AccountChangeArchive, AccountChangeArchiveDto> implements UserDataDeletable {

    private final AccountChangeArchiveRepository accountChangeArchiveRepository;
    private final AccountChangeArchiveMapper accountChangeArchiveMapper;

    @Value("${user-activity.account-changes-archive.page-size}")
    private int pageSize;

    public AccountChangeArchive mapToArchive(AccountChangeActivity accountChangeActivity) {
        log.info("Archiving account change activity. type: {}, userId: {}", accountChangeActivity.getType(), accountChangeActivity.getUserId());
        return AccountChangeArchive.builder()
                .userId(accountChangeActivity.getUserId())
                .type(accountChangeActivity.getType())
                .moveToArchiveDate(LocalDateTime.now())
                .createdAt(accountChangeActivity.getCreatedAt())
                .browser(accountChangeActivity.getBrowser())
                .ipAddress(accountChangeActivity.getIpAddress())
                .location(accountChangeActivity.getLocation())
                .build();
    }

    @Transactional
    public void archive(List<AccountChangeArchive> archiveAccountChangesActivities) {
        accountChangeArchiveRepository.saveAll(archiveAccountChangesActivities);
    }

    @Cacheable(value = "accountChangeArchive", key = "#userId + ':' + #sort")
    public List<AccountChangeArchiveDto> getAccountChangeArchive(Long userId, SortType sort) {
        return getActivities(userId, sort, pageSize);
    }

    @Override
    protected List<AccountChangeArchive> getRepositoryFindByUserId(Long userId, Pageable pageable) {
        return accountChangeArchiveRepository.findByUserId(userId, pageable);
    }

    @Override
    protected AccountChangeArchiveDto mapToDto(AccountChangeArchive accountChangeArchive) {
        return accountChangeArchiveMapper.mapToDto(accountChangeArchive);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        accountChangeArchiveRepository.deleteByUserId(userId);
        log.info("Deleted account changes activity from archive for userId={}", userId);
    }
}