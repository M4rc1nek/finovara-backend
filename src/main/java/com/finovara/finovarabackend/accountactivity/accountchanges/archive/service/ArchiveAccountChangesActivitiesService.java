package com.finovara.finovarabackend.accountactivity.accountchanges.archive.service;

import com.finovara.finovarabackend.accountactivity.accountchanges.activities.model.UserActivityAccountChanges;
import com.finovara.finovarabackend.accountactivity.accountchanges.archive.dto.ArchiveAccountChangesActivitiesDto;
import com.finovara.finovarabackend.accountactivity.accountchanges.archive.model.ArchiveAccountChangesActivities;
import com.finovara.finovarabackend.accountactivity.accountchanges.archive.repository.ArchiveAccountChangesActivitiesRepository;
import com.finovara.finovarabackend.config.TimeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArchiveAccountChangesActivitiesService {

    private final TimeConfig timeConfig;
    private final ArchiveAccountChangesActivitiesRepository archiveAccountChangesActivitiesRepository;

    public ArchiveAccountChangesActivities mapToArchive(UserActivityAccountChanges userActivityAccountChanges) {

        return ArchiveAccountChangesActivities.builder()
                .userAssigned(userActivityAccountChanges.getUserAssigned())
                .type(userActivityAccountChanges.getType())
                .moveToArchiveDate(LocalDateTime.now(timeConfig.clock()))
                .activityAccountChangesDate(userActivityAccountChanges.getDate())
                .browser(userActivityAccountChanges.getBrowser())
                .ipAddress(userActivityAccountChanges.getIpAddress())
                .location(userActivityAccountChanges.getLocation())
                .build();
    }

    @Transactional
    public void archive(List<ArchiveAccountChangesActivities> archiveAccountChangesActivities) {
        archiveAccountChangesActivitiesRepository.saveAll(archiveAccountChangesActivities);
    }

    public List<ArchiveAccountChangesActivitiesDto> getAccountChangesActivities(String email) {
        return archiveAccountChangesActivitiesRepository.findAllByUserAssignedEmailOrderByIdDesc(email)
                .stream().map(archive -> new ArchiveAccountChangesActivitiesDto(
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
