package com.finovara.finovarabackend.accountactivity.accountchanges.activities.service;

import com.finovara.finovarabackend.accountactivity.accountchanges.activities.dto.UserActivityAccountChangesDto;
import com.finovara.finovarabackend.accountactivity.accountchanges.activities.model.UserActivityAccountChanges;
import com.finovara.finovarabackend.accountactivity.accountchanges.activities.model.UserActivityAccountChangesType;
import com.finovara.finovarabackend.accountactivity.accountchanges.activities.repository.UserActivityAccountChangesRepository;
import com.finovara.finovarabackend.accountactivity.accountchanges.archive.model.ArchiveAccountChangesActivities;
import com.finovara.finovarabackend.accountactivity.accountchanges.archive.service.ArchiveAccountChangesActivitiesService;
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
public class UserActivityAccountChangesService {

    @Value("${user-activity.account-changes.page-size}")
    private int pageSize;

    private final UserManagerService userManagerService;
    private final UserActivityAccountChangesRepository userActivityAccountChangesRepository;
    private final PasswordConfirmationService passwordConfirmationService;
    private final ArchiveAccountChangesActivitiesService archiveAccountChangesActivitiesService;

    private final TimeConfig timeConfig;
    private final ClientData clientData;

    @Transactional
    public void createUserActivityAccountChanges(String email, UserActivityAccountChangesType type, HttpServletRequest request) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        String ipAddress = clientData.getClientIp(request);

        UserActivityAccountChanges userActivityAccountChanges = UserActivityAccountChanges.builder()
                .userAssigned(user)
                .type(type)
                .date(LocalDateTime.now(timeConfig.clock()))
                .browser(clientData.getUserBrowser(request))
                .ipAddress(ipAddress)
                .location(clientData.getUserLocation(ipAddress))
                .build();

        userActivityAccountChangesRepository.save(userActivityAccountChanges);

        deleteOldAccountChangesActivities(email);
    }

    public List<UserActivityAccountChangesDto> getUserActivityAccountChanges(String email) {
        return userActivityAccountChangesRepository.findByUserAssignedEmailOrderByIdDesc(email)
                .stream().map(activity -> new UserActivityAccountChangesDto(
                        activity.getType(),
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
    public void deleteOldAccountChangesActivities(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        long countedAccountChangesActivities = userActivityAccountChangesRepository.countAccountChangesByUserAssignedId(user.getId());

        if (countedAccountChangesActivities > pageSize) {
            List<UserActivityAccountChanges> activitiesToMove = userActivityAccountChangesRepository.findFewByUserAssignedId(user.getId(), PageRequest.of(0, pageSize));
            List<ArchiveAccountChangesActivities> activitiesToArchive = activitiesToMove.stream().map(archiveAccountChangesActivitiesService::mapToArchive)
                    .toList();
            archiveAccountChangesActivitiesService.archive(activitiesToArchive);

            userActivityAccountChangesRepository.deleteAll(activitiesToMove);
        }
    }
}
