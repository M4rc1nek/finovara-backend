package com.finovara.activitylogservice.activitylog.accountactivity.secure.login.activity.service;

import com.finovara.activitylogservice.activitylog.accountactivity.secure.core.SecurityActivityCore;
import com.finovara.activitylogservice.activitylog.accountactivity.secure.login.activity.dto.LoginActivityDto;
import com.finovara.activitylogservice.activitylog.accountactivity.secure.login.activity.model.LoginActivity;
import com.finovara.activitylogservice.activitylog.accountactivity.secure.login.activity.repository.LoginActivityRepository;
import com.finovara.activitylogservice.activitylog.accountactivity.secure.login.archive.model.LoginActivityArchive;
import com.finovara.activitylogservice.activitylog.accountactivity.secure.login.archive.service.LoginActivityArchiveService;
import com.finovara.activitylogservice.activitylog.datadeletable.UserDataDeletable;
import com.finovara.activitylogservice.feignclient.CoreBackendClient;
import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import com.finovara.contracts.event.activity.secure.login.activity.LoginActivityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginActivityService extends SecurityActivityCore<LoginActivity, LoginActivityArchive>  implements UserDataDeletable {

    private final LoginActivityRepository loginActivityRepository;
    private final LoginActivityArchiveService archiveService;
    private final CoreBackendClient coreBackendClient;

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

    public List<LoginActivityDto> getLoginActivity(Long userId) {
        return loginActivityRepository.findByUserIdOrderByDesc(userId);
    }

    public void confirmPassword(Long userId, ConfirmPasswordDto dto) {
        coreBackendClient.verifyPassword(userId, dto);
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
        return archiveService.mapToArchive(a);
    }

    @Override
    protected void archive(List<LoginActivityArchive> a) {
        archiveService.archive(a);
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
