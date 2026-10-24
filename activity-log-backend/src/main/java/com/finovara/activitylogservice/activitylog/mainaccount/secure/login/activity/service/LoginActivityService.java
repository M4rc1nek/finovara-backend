package com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.service;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.core.SecurityActivityCore;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.dto.LoginActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.mapper.LoginActivityMapper;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.model.LoginActivity;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.repository.LoginActivityRepository;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.model.LoginActivityArchive;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.service.LoginActivityArchiveService;
import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.contracts.mainaccount.activity.event.secure.login.activity.LoginActivityEvent;
import com.finovara.contracts.user.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.user.datadeletable.UserDataDeletable;
import com.finovara.contracts.util.SortType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginActivityService extends SecurityActivityCore<LoginActivity, LoginActivityArchive, LoginActivityDto> implements UserDataDeletable {

    private final LoginActivityRepository loginActivityRepository;
    private final LoginActivityArchiveService loginActivityArchiveService;
    private final LoginActivityMapper loginActivityMapper;
    private final AuthBackendClient authBackendClient;

    @Value("${user-activity.login.page-size}")
    private int pageSize;

    @Transactional
    public void handleEvent(LoginActivityEvent event) {
        LoginActivity activity = LoginActivity.builder()
                .userId(event.userId())
                .type("Login")
                .status(event.status())
                .browser(event.browser())
                .ipAddress(event.ipAddress())
                .location(event.location())
                .createdAt(event.occurredAt())
                .build();

        saveActivity(activity);
        log.info("Created activity type: {}, userId: {}", activity.getType(), event.userId());
        moveToArchive(event.userId(), pageSize);
    }

    @Cacheable(value = "loginActivity", key = "#userId + ':' + #sort + ':' + #page")
    public List<LoginActivityDto> getLoginActivity(Long userId, SortType sort, int page) {
        return getActivities(userId, sort, page, pageSize);
    }

    public void confirmPassword(Long userId, ConfirmPasswordDto confirmPasswordDto) {
        authBackendClient.verifyPassword(userId, confirmPasswordDto);
    }

    @Override
    protected List<LoginActivity> getRepositoryFindByUserId(Long userId, Pageable pageable) {
        return loginActivityRepository.findByUserId(userId, pageable);
    }

    @Override
    protected LoginActivityDto mapToDto(LoginActivity loginActivity) {
        return loginActivityMapper.mapToDto(loginActivity);
    }

    @Override
    protected void saveActivity(LoginActivity activity) {
        loginActivityRepository.save(activity);
    }

    @Override
    protected List<LoginActivity> findOldestForArchive(Long userId, int limit) {
        return loginActivityRepository.findOldestForArchive(userId, PageRequest.of(0, limit));
    }

    @Override
    protected LoginActivityArchive mapToArchive(LoginActivity activity) {
        return loginActivityArchiveService.mapToArchive(activity);
    }

    @Override
    protected void archive(List<LoginActivityArchive> archives) {
        loginActivityArchiveService.archive(archives);
    }

    @Override
    protected void deleteActivities(List<LoginActivity> activities) {
        loginActivityRepository.deleteAll(activities);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        loginActivityRepository.deleteByUserId(userId);
        log.info("Deleted login activity for userId={}", userId);
    }
}