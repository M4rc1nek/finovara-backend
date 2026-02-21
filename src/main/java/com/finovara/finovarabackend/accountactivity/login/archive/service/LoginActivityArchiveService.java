package com.finovara.finovarabackend.accountactivity.login.archive.service;

import com.finovara.finovarabackend.accountactivity.login.activities.model.LoginActivity;
import com.finovara.finovarabackend.accountactivity.login.archive.dto.LoginActivityArchiveDto;
import com.finovara.finovarabackend.accountactivity.login.archive.model.LoginActivityArchive;
import com.finovara.finovarabackend.accountactivity.login.archive.repository.LoginActivityArchiveRepository;
import com.finovara.finovarabackend.config.TimeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoginActivityArchiveService {

    private final TimeConfig timeConfig;
    private final LoginActivityArchiveRepository loginActivityArchiveRepository;

    public LoginActivityArchive mapToArchive(LoginActivity loginActivity) {

        return LoginActivityArchive.builder()
                .userAssigned(loginActivity.getUserAssigned())
                .type("Login")
                .status(loginActivity.getStatus())
                .moveToArchiveDate(LocalDateTime.now(timeConfig.clock()))
                .activityLoginDate(loginActivity.getDate())
                .browser(loginActivity.getBrowser())
                .ipAddress(loginActivity.getIpAddress())
                .location(loginActivity.getLocation())
                .build();
    }

    @Transactional
    public void archive(List<LoginActivityArchive> activitiesToArchive) {
        loginActivityArchiveRepository.saveAll(activitiesToArchive);
    }

    public List<LoginActivityArchiveDto> getLoginActivityArchive(String email) {
        return loginActivityArchiveRepository.findAllByUserAssignedEmailOrderByIdDesc(email)
                .stream().map(archive -> new LoginActivityArchiveDto(
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
