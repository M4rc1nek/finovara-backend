package com.finovara.finovarabackend.accountactivity.login.activities.service;

import com.finovara.finovarabackend.accountactivity.login.activities.dto.UserActivityLoginDto;
import com.finovara.finovarabackend.accountactivity.login.activities.model.UserActivityLogin;
import com.finovara.finovarabackend.accountactivity.login.activities.model.UserActivityLoginStatus;
import com.finovara.finovarabackend.accountactivity.login.activities.repository.UserActivityLoginRepository;
import com.finovara.finovarabackend.accountactivity.login.archive.model.ArchiveLoginActivity;
import com.finovara.finovarabackend.accountactivity.login.archive.service.ArchiveLoginActivityService;
import com.finovara.finovarabackend.config.TimeConfig;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.clientdata.metadata.ClientData;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordConfirmationService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
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

public class UserActivityLoginService {

    private final UserManagerService userManagerService;
    private final TimeConfig timeConfig;
    private final UserActivityLoginRepository userActivityLoginRepository;
    private final ArchiveLoginActivityService archiveLoginActivityService;

    private final ClientData clientData;

    private final PasswordConfirmationService passwordConfirmationService;

    @Value("${user-activity.login.page-size}")
    private int pageSize;

    @Transactional
    public void createUserActivityLogin(String email, UserActivityLoginStatus userActivityLoginStatus, HttpServletRequest request) {

        User user = userManagerService.getUserByEmailOrThrow(email);
        String ipAddress = clientData.getClientIp(request);

        UserActivityLogin userActivityLogin = UserActivityLogin.builder()
                .userAssigned(user)
                .type("Login")
                .status(userActivityLoginStatus)
                .date(LocalDateTime.now(timeConfig.clock()))
                .browser(clientData.getUserBrowser(request))
                .ipAddress(ipAddress)
                .location(clientData.getUserLocation(ipAddress))
                .build();

        userActivityLoginRepository.save(userActivityLogin);

        moveToArchiveActivities(email);
    }

    public List<UserActivityLoginDto> getUserActivityLogin(String email) {
        return userActivityLoginRepository
                .findByUserAssignedEmailOrderByIdDesc(email)
                .stream()
                .map(activity -> new UserActivityLoginDto(
                        activity.getType(),
                        activity.getStatus(),
                        activity.getDate(),
                        activity.getBrowser(),
                        activity.getIpAddress(),
                        activity.getLocation()
                ))
                .toList();

    }

    public void confirmPasswordToUserActivityLogin(String email, ConfirmPasswordDto confirmPasswordDto) {
        passwordConfirmationService.confirmPassword(email, confirmPasswordDto);
    }

    @Transactional
    private void moveToArchiveActivities(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        long countedActivities = userActivityLoginRepository.countActivityLoginByUserAssignedId(user.getId());

        if (countedActivities > pageSize) {
            List<UserActivityLogin> activitiesToMove = userActivityLoginRepository.findOldestByUserAssignedId(user.getId(), PageRequest.of(0, pageSize));
            List<ArchiveLoginActivity> activitiesToArchive = activitiesToMove.stream().map(archiveLoginActivityService::mapToArchive)
                    .toList();
            archiveLoginActivityService.archive(activitiesToArchive);

            userActivityLoginRepository.deleteAll(activitiesToMove);

        }

    }
}
