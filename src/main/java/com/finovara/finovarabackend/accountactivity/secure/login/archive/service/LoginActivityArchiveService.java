package com.finovara.finovarabackend.accountactivity.secure.login.archive.service;

import com.finovara.finovarabackend.accountactivity.secure.login.activities.model.LoginActivity;
import com.finovara.finovarabackend.accountactivity.secure.login.archive.dto.LoginActivityArchiveDto;
import com.finovara.finovarabackend.accountactivity.secure.login.archive.model.LoginActivityArchive;
import com.finovara.finovarabackend.accountactivity.secure.login.archive.repository.LoginActivityArchiveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoginActivityArchiveService {

    private final LoginActivityArchiveRepository loginActivityArchiveRepository;

    public LoginActivityArchive mapToArchive(LoginActivity loginActivity) {

        return LoginActivityArchive.builder()
                .userAssigned(loginActivity.getUserAssigned())
                .type("Login")
                .status(loginActivity.getStatus())
                .moveToArchiveDate(LocalDateTime.now())
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
        return loginActivityArchiveRepository.findAllByUserAssignedEmailOrderByIdDesc(email);
    }
}
