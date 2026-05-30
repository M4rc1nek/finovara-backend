package com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.activity.service;

import com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.activity.dto.AccountChangesActivityDto;
import com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.activity.model.AccountChangesActivity;
import com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.activity.repository.AccountChangesActivityRepository;
import com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.archive.model.AccountChangeArchive;
import com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.archive.service.AccountChangeArchiveService;
import com.finovara.activityservice.activitylog.accountactivity.secure.core.SecurityActivityCore;
import com.finovara.activityservice.feignclient.CoreBackendClient;
import com.finovara.contracts.dto.ConfirmPasswordDto;
import com.finovara.contracts.event.secure.accountchange.activity.AccountChangesActivityEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountChangesActivityService extends SecurityActivityCore<AccountChangesActivity, AccountChangeArchive> {

    private final AccountChangesActivityRepository accountChangesActivityRepository;
    private final AccountChangeArchiveService accountChangeArchiveService;
    private final CoreBackendClient coreBackendClient;

    @Value("${user-activity.account-changes.page-size}")
    private int pageSize;

    @Transactional
    public void handleEvent(AccountChangesActivityEvent event) {
        AccountChangesActivity activity = AccountChangesActivity.builder()
                .userId(event.userId())
                .type(event.type())
                .browser(event.browser())
                .ipAddress(event.ipAddress())
                .location(event.location())
                .createdAt(event.occurredAt())
                .build();

        saveActivity(activity);
        moveToArchive(event.userId(), pageSize);
    }

    public List<AccountChangesActivityDto> getAccountChangesActivity(Long userId) {
        return accountChangesActivityRepository.findByUserIdOrderByIdDesc(userId);
    }

    public void confirmPassword(Long userId, ConfirmPasswordDto dto) {
        coreBackendClient.verifyPassword(userId, dto);
    }

    @Override
    protected void saveActivity(AccountChangesActivity activity) {
        accountChangesActivityRepository.save(activity);
    }

    @Override
    protected long countActivities(Long userId) {
        return accountChangesActivityRepository.countAccountChangesByUserId(userId);
    }

    @Override
    protected List<AccountChangesActivity> findActivitiesToArchive(Long userId, int pageSize) {
        return accountChangesActivityRepository.findFewByUserId(userId, PageRequest.of(0, pageSize));
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
