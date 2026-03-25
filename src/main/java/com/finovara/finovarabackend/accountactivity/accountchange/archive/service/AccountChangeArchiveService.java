package com.finovara.finovarabackend.accountactivity.accountchange.archive.service;

import com.finovara.finovarabackend.accountactivity.accountchange.activities.model.AccountChangesActivity;
import com.finovara.finovarabackend.accountactivity.accountchange.archive.dto.AccountChangeArchiveDto;
import com.finovara.finovarabackend.accountactivity.accountchange.archive.model.AccountChangeArchive;
import com.finovara.finovarabackend.accountactivity.accountchange.archive.repository.AccountChangeArchiveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountChangeArchiveService {

    private final AccountChangeArchiveRepository accountChangeArchiveRepository;

    public AccountChangeArchive mapToArchive(AccountChangesActivity accountChangesActivity) {

        return AccountChangeArchive.builder()
                .userAssigned(accountChangesActivity.getUserAssigned())
                .type(accountChangesActivity.getType())
                .moveToArchiveDate(LocalDateTime.now())
                .activityAccountChangesDate(accountChangesActivity.getDate())
                .browser(accountChangesActivity.getBrowser())
                .ipAddress(accountChangesActivity.getIpAddress())
                .location(accountChangesActivity.getLocation())
                .build();
    }

    @Transactional
    public void archive(List<AccountChangeArchive> archiveAccountChangesActivities) {
        accountChangeArchiveRepository.saveAll(archiveAccountChangesActivities);
    }

    public List<AccountChangeArchiveDto> getAccountChangeArchive(String email) {
        return accountChangeArchiveRepository.findAllByUserAssignedEmailOrderByIdDesc(email);
    }
}
