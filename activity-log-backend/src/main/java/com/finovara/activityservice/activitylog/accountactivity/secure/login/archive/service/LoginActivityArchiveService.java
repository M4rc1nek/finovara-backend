package com.finovara.activityservice.activitylog.accountactivity.secure.login.archive.service;

import com.finovara.activityservice.activitylog.accountactivity.secure.login.activity.model.LoginActivity;
import com.finovara.activityservice.activitylog.accountactivity.secure.login.archive.dto.LoginActivityArchiveDto;
import com.finovara.activityservice.activitylog.accountactivity.secure.login.archive.model.LoginActivityArchive;
import com.finovara.activityservice.activitylog.accountactivity.secure.login.archive.repository.LoginActivityArchiveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginActivityArchiveService {

    private final LoginActivityArchiveRepository loginActivityArchiveRepository;

    public LoginActivityArchive mapToArchive(LoginActivity loginActivity) {

        log.info("Archiving login activity. type: {}, userId: {}", loginActivity.getType(), loginActivity.getUserId());
        return LoginActivityArchive.builder()
                .userId(loginActivity.getUserId())
                .type("Login")
                .status(loginActivity.getStatus())
                .moveToArchiveDate(LocalDateTime.now())
                .activityLoginDate(loginActivity.getCreatedAt())
                .browser(loginActivity.getBrowser())
                .ipAddress(loginActivity.getIpAddress())
                .location(loginActivity.getLocation())
                .build();
    }

    @Transactional
    public void archive(List<LoginActivityArchive> activitiesToArchive) {
        loginActivityArchiveRepository.saveAll(activitiesToArchive);
    }

    public List<LoginActivityArchiveDto> getLoginActivityArchive(Long userId) {
        return loginActivityArchiveRepository.findAllByUserIdOrderByIdDesc(userId);
    }
}
