package com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.service;

import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.dto.AccountChangesActivityDto;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.model.AccountChangesActivity;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.repository.AccountChangesActivityRepository;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.archive.model.AccountChangeArchive;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.archive.service.AccountChangeArchiveService;
import com.finovara.finovarabackend.accountactivity.secure.core.SecurityActivityCore;
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
public class AccountChangesActivityService extends SecurityActivityCore<AccountChangesActivity, AccountChangeArchive> {

    private final AccountChangesActivityRepository accountChangesActivityRepository;
    private final AccountChangeArchiveService accountChangeArchiveService;

    @Value("${user-activity.account-changes.page-size}")
    private int pageSize;

    public AccountChangesActivityService(
            UserManagerService userManagerService,
            PasswordConfirmationService passwordConfirmationService,
            ClientData clientData,
            AccountChangesActivityRepository accountChangesActivityRepository,
            AccountChangeArchiveService accountChangeArchiveService
    ) {
        super(userManagerService, passwordConfirmationService, clientData);
        this.accountChangesActivityRepository = accountChangesActivityRepository;
        this.accountChangeArchiveService = accountChangeArchiveService;
    }

    @Transactional
    public void createAccountChangesActivity(String email, AccountChangesActivityType type, HttpServletRequest request) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        String ipAddress = clientData.getClientIp(request);

        AccountChangesActivity activity = AccountChangesActivity.builder()
                .userAssigned(user)
                .type(type)
                .createdAt(LocalDateTime.now())
                .browser(clientData.getUserBrowser(request))
                .ipAddress(ipAddress)
                .location(clientData.getUserLocation(ipAddress))
                .build();

        saveActivity(activity);
        moveToArchive(user, pageSize);
    }

    public List<AccountChangesActivityDto> getAccountChangesActivity(String email) {
        return accountChangesActivityRepository.findByUserAssignedEmailOrderByIdDesc(email);
    }

    public void confirmPasswordToAccountChangesActivity(String email, ConfirmPasswordDto dto) {
        passwordConfirmationService.confirmPassword(email, dto);
    }

    @Override
    protected void saveActivity(AccountChangesActivity activity) {
        accountChangesActivityRepository.save(activity);
    }

    @Override
    protected long countActivities(Long userId) {
        return accountChangesActivityRepository.countAccountChangesByUserAssignedId(userId);
    }

    @Override
    protected List<AccountChangesActivity> findActivitiesToArchive(Long userId, int pageSize) {
        return accountChangesActivityRepository.findFewByUserAssignedId(userId, PageRequest.of(0, pageSize));
    }

    @Override
    protected AccountChangeArchive mapToArchive(AccountChangesActivity activity) {
        return accountChangeArchiveService.mapToArchive(activity);
    }

    @Override
    protected void archive(List<AccountChangeArchive> archives) {
        accountChangeArchiveService.archive(archives);
    }

    @Override
    protected void deleteActivities(List<AccountChangesActivity> activities) {
        accountChangesActivityRepository.deleteAll(activities);
    }
}