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

    @Cacheable(value = "loginActivity", key = "#userId + ':' + #sort")
    public List<LoginActivityDto> getLoginActivity(Long userId, SortType sort) {
        return getActivities(userId, sort, pageSize);
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
    protected void saveActivity(LoginActivity a) {
        loginActivityRepository.save(a);
    }

    @Override
    protected long countActivities(Long userId) {
        return loginActivityRepository.countActivityLoginByUserId(userId);
    }

    @Override
    protected List<LoginActivity> findActivitiesToArchive(Long userId, int pageSize) {
        return loginActivityRepository.findOldestByUserId(userId, PageRequest.of(0, pageSize));
    }

    @Override
    protected LoginActivityArchive mapToArchive(LoginActivity a) {
        return loginActivityArchiveService.mapToArchive(a);
    }

    @Override
    protected void archive(List<LoginActivityArchive> a) {
        loginActivityArchiveService.archive(a);
    }

    @Override
    protected void deleteActivities(List<LoginActivity> a) {
        loginActivityRepository.deleteAll(a);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        loginActivityRepository.deleteByUserId(userId);
        log.info("Deleted login activity for userId={}", userId);
    }
}