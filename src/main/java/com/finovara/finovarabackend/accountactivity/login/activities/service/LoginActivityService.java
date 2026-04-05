package com.finovara.finovarabackend.accountactivity.login.activities.service;

import com.finovara.finovarabackend.accountactivity.login.activities.dto.LoginActivityDto;
import com.finovara.finovarabackend.accountactivity.login.activities.model.LoginActivity;
import com.finovara.finovarabackend.accountactivity.login.activities.model.LoginActivityStatus;
import com.finovara.finovarabackend.accountactivity.login.activities.repository.LoginActivityRepository;
import com.finovara.finovarabackend.accountactivity.login.archive.model.LoginActivityArchive;
import com.finovara.finovarabackend.accountactivity.login.archive.service.LoginActivityArchiveService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.clientdata.metadata.ClientData;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordConfirmationService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor

public class LoginActivityService {

    private final UserManagerService userManagerService;
    private final LoginActivityRepository loginActivityRepository;
    private final LoginActivityArchiveService archiveLoginActivityService;

    private final ClientData clientData;

    private final PasswordConfirmationService passwordConfirmationService;

    @Value("${user-activity.login.page-size}")
    private int pageSize;

    @Transactional
    public void createLoginActivity(String email, LoginActivityStatus loginActivityStatus, HttpServletRequest request) {

        User user = userManagerService.getUserByEmailOrThrow(email);
        String ipAddress = clientData.getClientIp(request);

        LoginActivity loginActivity = LoginActivity.builder()
                .userAssigned(user)
                .type("Login")
                .status(loginActivityStatus)
                .date(LocalDateTime.now())
                .browser(clientData.getUserBrowser(request))
                .ipAddress(ipAddress)
                .location(clientData.getUserLocation(ipAddress))
                .build();

        loginActivityRepository.save(loginActivity);

        moveToArchive(email);
    }

    public List<LoginActivityDto> getLoginActivity(String email) {
        return loginActivityRepository.findByUserAssignedEmailOrderByDesc(email);
    }

    public void confirmPasswordToLoginActivity(String email, ConfirmPasswordDto confirmPasswordDto) {
        passwordConfirmationService.confirmPassword(email, confirmPasswordDto);
    }

    @Transactional
    private void moveToArchive(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        long countedActivities = loginActivityRepository.countActivityLoginByUserAssignedId(user.getId());

        if (countedActivities > pageSize) {
            List<LoginActivity> activitiesToMove = loginActivityRepository.findOldestByUserAssignedId(user.getId(), PageRequest.of(0, pageSize));
            List<LoginActivityArchive> activitiesToArchive = activitiesToMove.stream().map(archiveLoginActivityService::mapToArchive)
                    .toList();
            archiveLoginActivityService.archive(activitiesToArchive);

            loginActivityRepository.deleteAll(activitiesToMove);

        }

    }
}
