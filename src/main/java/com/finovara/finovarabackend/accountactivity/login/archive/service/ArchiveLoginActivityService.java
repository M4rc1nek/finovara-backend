package com.finovara.finovarabackend.accountactivity.login.archive.service;

import com.finovara.finovarabackend.accountactivity.login.activities.model.UserActivityLogin;
import com.finovara.finovarabackend.accountactivity.login.archive.dto.ArchiveLoginActivityDto;
import com.finovara.finovarabackend.accountactivity.login.archive.model.ArchiveLoginActivity;
import com.finovara.finovarabackend.accountactivity.login.archive.repository.ArchiveLoginActivityRepository;
import com.finovara.finovarabackend.config.TimeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArchiveLoginActivityService {

    private final TimeConfig timeConfig;
    private final ArchiveLoginActivityRepository archiveLoginActivityRepository;

    public ArchiveLoginActivity mapToArchive(UserActivityLogin userActivityLogin) {

        return ArchiveLoginActivity.builder()
                .userAssigned(userActivityLogin.getUserAssigned())
                .type("Login")
                .status(userActivityLogin.getStatus())
                .moveToArchiveDate(LocalDateTime.now(timeConfig.clock()))
                .activityLoginDate(userActivityLogin.getDate())
                .browser(userActivityLogin.getBrowser())
                .ipAddress(userActivityLogin.getIpAddress())
                .location(userActivityLogin.getLocation())
                .build();
    }

    @Transactional
    public void archive(List<ArchiveLoginActivity> activitiesToArchive) {
        archiveLoginActivityRepository.saveAll(activitiesToArchive);
    }

    public List<ArchiveLoginActivityDto> getArchiveLoginActivity(String email) {
        return archiveLoginActivityRepository.findAllByUserAssignedEmailOrderByIdDesc(email)
                .stream().map(archive -> new ArchiveLoginActivityDto(
                        archive.getType(),
                        archive.getStatus(),
                        archive.getMoveToArchiveDate(),
                        archive.getActivityLoginDate(),
                        archive.getBrowser(),
                        archive.getIpAddress(),
                        archive.getLocation()
                ))
                .toList();
    }
}
