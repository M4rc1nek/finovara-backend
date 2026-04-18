package com.finovara.finovarabackend.accountactivity.secure.login.activity.service;

import com.finovara.finovarabackend.accountactivity.secure.core.SecurityActivityCore;
import com.finovara.finovarabackend.accountactivity.secure.login.activity.dto.LoginActivityDto;
import com.finovara.finovarabackend.accountactivity.secure.login.activity.model.LoginActivity;
import com.finovara.finovarabackend.accountactivity.secure.login.activity.model.LoginActivityStatus;
import com.finovara.finovarabackend.accountactivity.secure.login.activity.repository.LoginActivityRepository;
import com.finovara.finovarabackend.accountactivity.secure.login.archive.model.LoginActivityArchive;
import com.finovara.finovarabackend.accountactivity.secure.login.archive.service.LoginActivityArchiveService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.clientdata.metadata.ClientData;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordConfirmationService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoginActivityService extends SecurityActivityCore<LoginActivity, LoginActivityArchive> {

    private final LoginActivityRepository loginActivityRepository;
    private final LoginActivityArchiveService archiveService;

    @Value("${user-activity.login.page-size}")
    private int pageSize;

    public LoginActivityService(
            UserManagerService userManagerService,
            PasswordConfirmationService passwordConfirmationService,
            ClientData clientData,
            LoginActivityRepository loginActivityRepository,
            LoginActivityArchiveService archiveService) {
        super(userManagerService, passwordConfirmationService, clientData);
        this.loginActivityRepository = loginActivityRepository;
        this.archiveService = archiveService;
    }

    @Transactional
    public void createLoginActivity(String email, LoginActivityStatus status, HttpServletRequest request) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        String ip = clientData.getClientIp(request);

        LoginActivity activity = LoginActivity.builder()
                .userAssigned(user)
                .type("Login")
                .status(status)
                .createdAt(LocalDateTime.now())
                .browser(clientData.getUserBrowser(request))
                .ipAddress(ip)
                .location(clientData.getUserLocation(ip))
                .build();

        saveActivity(activity);
        moveToArchive(user, pageSize);
    }

    public void confirmPassword(String email, ConfirmPasswordDto dto) {
        passwordConfirmationService.confirmPassword(email, dto);
    }

    public List<LoginActivityDto> getLoginActivity(String email) {
        return loginActivityRepository.findByUserAssignedEmailOrderByDesc(email);
    }

    @Override
    protected void saveActivity(LoginActivity a) {
        loginActivityRepository.save(a);
    }

    @Override
    protected long countActivities(Long userId) {
        return loginActivityRepository.countActivityLoginByUserAssignedId(userId);
    }

    @Override
    protected List<LoginActivity> findActivitiesToArchive(Long userId, int pageSize) {
        return loginActivityRepository.findOldestByUserAssignedId(userId, PageRequest.of(0, pageSize));
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
}
