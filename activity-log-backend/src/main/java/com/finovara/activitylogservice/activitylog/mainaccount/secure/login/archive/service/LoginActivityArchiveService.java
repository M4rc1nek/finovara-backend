package com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.service;

import com.finovara.activitylogservice.activitylog.mainaccount.core.AccountActivityCore;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.model.LoginActivity;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.dto.LoginActivityArchiveDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.mapper.LoginActivityArchiveMapper;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.model.LoginActivityArchive;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.repository.LoginActivityArchiveRepository;
import com.finovara.contracts.user.datadeletable.UserDataDeletable;
import com.finovara.contracts.util.SortType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginActivityArchiveService extends AccountActivityCore<LoginActivityArchive, LoginActivityArchiveDto> implements UserDataDeletable {

    private final LoginActivityArchiveRepository loginActivityArchiveRepository;
    private final LoginActivityArchiveMapper loginActivityArchiveMapper;

    @Value("${user-activity.login-archive.page-size}")
    private int pageSize;

    public LoginActivityArchive mapToArchive(LoginActivity loginActivity) {
        log.info("Archiving login activity. type: {}, userId: {}", loginActivity.getType(), loginActivity.getUserId());
        return LoginActivityArchive.builder()
                .userId(loginActivity.getUserId())
                .type("Login")
                .status(loginActivity.getStatus())
                .moveToArchiveDate(LocalDateTime.now())
                .createdAt(loginActivity.getCreatedAt())
                .browser(loginActivity.getBrowser())
                .ipAddress(loginActivity.getIpAddress())
                .location(loginActivity.getLocation())
                .build();
    }

    @Transactional
    public void archive(List<LoginActivityArchive> activitiesToArchive) {
        loginActivityArchiveRepository.saveAll(activitiesToArchive);
    }

    @Cacheable(value = "loginActivityArchive", key = "#userId + ':' + #sort")
    public List<LoginActivityArchiveDto> getLoginActivityArchive(Long userId, SortType sort) {
        return getActivities(userId, sort, pageSize);
    }

    @Override
    protected List<LoginActivityArchive> getRepositoryFindByUserId(Long userId, Pageable pageable) {
        return loginActivityArchiveRepository.findByUserId(userId, pageable);
    }

    @Override
    protected LoginActivityArchiveDto mapToDto(LoginActivityArchive loginActivityArchive) {
        return loginActivityArchiveMapper.mapToDto(loginActivityArchive);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        loginActivityArchiveRepository.deleteByUserId(userId);
        log.info("Deleted login activity from archive for userId={}", userId);
    }
}