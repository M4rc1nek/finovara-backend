package com.finovara.finovarabackend.accountactivity.accountchanges.archive.service;

import com.finovara.finovarabackend.accountactivity.accountchanges.activities.model.AccountChangesActivity;
import com.finovara.finovarabackend.accountactivity.accountchanges.archive.dto.AccountChangeArchiveDto;
import com.finovara.finovarabackend.accountactivity.accountchanges.archive.model.AccountChangeArchive;
import com.finovara.finovarabackend.accountactivity.accountchanges.archive.repository.AccountChangeArchiveRepository;
import com.finovara.finovarabackend.config.TimeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountChangeArchiveService {

    private final TimeConfig timeConfig;
    private final AccountChangeArchiveRepository accountChangeArchiveRepository;

    public AccountChangeArchive mapToArchive(AccountChangesActivity accountChangesActivity) {

        return AccountChangeArchive.builder()
                .userAssigned(accountChangesActivity.getUserAssigned())
                .type(accountChangesActivity.getType())
                .moveToArchiveDate(LocalDateTime.now(timeConfig.clock()))
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
        return accountChangeArchiveRepository.findAllByUserAssignedEmailOrderByIdDesc(email)
                .stream().map(archive -> new AccountChangeArchiveDto(
                        archive.getType(),
                        archive.getMoveToArchiveDate(),
                        archive.getActivityAccountChangesDate(),
                        archive.getBrowser(),
                        archive.getIpAddress(),
                        archive.getLocation()
                ))
                .toList();
    }
}
