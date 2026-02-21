package com.finovara.finovarabackend.accountactivity.accountchanges.activities.service;

import com.finovara.finovarabackend.accountactivity.accountchanges.activities.dto.AccountChangesActivityDto;
import com.finovara.finovarabackend.accountactivity.accountchanges.activities.model.AccountChangesActivity;
import com.finovara.finovarabackend.accountactivity.accountchanges.activities.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.accountchanges.activities.repository.AccountChangesActivityRepository;
import com.finovara.finovarabackend.accountactivity.accountchanges.archive.model.AccountChangeArchive;
import com.finovara.finovarabackend.accountactivity.accountchanges.archive.service.AccountChangeArchiveService;
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
public class AccountChangesActivityService {

    @Value("${user-activity.account-changes.page-size}")
    private int pageSize;

    private final UserManagerService userManagerService;
    private final AccountChangesActivityRepository accountChangesActivityRepository;
    private final PasswordConfirmationService passwordConfirmationService;
    private final AccountChangeArchiveService accountChangeArchiveService;

    private final TimeConfig timeConfig;
    private final ClientData clientData;

    @Transactional
    public void createAccountChangesActivity(String email, AccountChangesActivityType type, HttpServletRequest request) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        String ipAddress = clientData.getClientIp(request);

        AccountChangesActivity accountChangesActivity = AccountChangesActivity.builder()
                .userAssigned(user)
                .type(type)
                .date(LocalDateTime.now(timeConfig.clock()))
                .browser(clientData.getUserBrowser(request))
                .ipAddress(ipAddress)
                .location(clientData.getUserLocation(ipAddress))
                .build();

        accountChangesActivityRepository.save(accountChangesActivity);

        moveToArchive(email);
    }

    public List<AccountChangesActivityDto> getAccountChangesActivity(String email) {
        return accountChangesActivityRepository.findByUserAssignedEmailOrderByIdDesc(email)
                .stream().map(activity -> new AccountChangesActivityDto(
                        activity.getType(),
                        activity.getDate(),
                        activity.getBrowser(),
                        activity.getIpAddress(),
                        activity.getLocation()
                ))
                .toList();
    }

    public void confirmPasswordToAccountChangesActivity(String email, ConfirmPasswordDto confirmPasswordDto) {
        passwordConfirmationService.confirmPassword(email, confirmPasswordDto);
    }

    @Transactional
    public void moveToArchive(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        long countedAccountChangesActivities = accountChangesActivityRepository.countAccountChangesByUserAssignedId(user.getId());

        if (countedAccountChangesActivities > pageSize) {
            List<AccountChangesActivity> activitiesToMove = accountChangesActivityRepository.findFewByUserAssignedId(user.getId(), PageRequest.of(0, pageSize));
            List<AccountChangeArchive> activitiesToArchive = activitiesToMove.stream().map(accountChangeArchiveService::mapToArchive)
                    .toList();
            accountChangeArchiveService.archive(activitiesToArchive);

            accountChangesActivityRepository.deleteAll(activitiesToMove);
        }
    }
}
