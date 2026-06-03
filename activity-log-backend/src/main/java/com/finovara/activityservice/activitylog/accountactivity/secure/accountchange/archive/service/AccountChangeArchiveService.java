package com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.archive.service;

import com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.activity.model.AccountChangesActivity;
import com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.archive.dto.AccountChangeArchiveDto;
import com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.archive.model.AccountChangeArchive;
import com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.archive.repository.AccountChangeArchiveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountChangeArchiveService {

    private final AccountChangeArchiveRepository accountChangeArchiveRepository;

    public AccountChangeArchive mapToArchive(AccountChangesActivity accountChangesActivity) {

        log.info("Archiving account change activity. type: {}, userId: {}", accountChangesActivity.getType(), accountChangesActivity.getUserId());
        return AccountChangeArchive.builder()
                .userId(accountChangesActivity.getUserId())
                .type(accountChangesActivity.getType())
                .moveToArchiveDate(LocalDateTime.now())
                .activityAccountChangesDate(accountChangesActivity.getCreatedAt())
                .browser(accountChangesActivity.getBrowser())
                .ipAddress(accountChangesActivity.getIpAddress())
                .location(accountChangesActivity.getLocation())
                .build();
    }

    @Transactional
    public void archive(List<AccountChangeArchive> archiveAccountChangesActivities) {
        accountChangeArchiveRepository.saveAll(archiveAccountChangesActivities);
    }

    public List<AccountChangeArchiveDto> getAccountChangeArchive(Long userId) {
        return accountChangeArchiveRepository.findAllByUserIdOrderByIdDesc(userId);
    }
}
