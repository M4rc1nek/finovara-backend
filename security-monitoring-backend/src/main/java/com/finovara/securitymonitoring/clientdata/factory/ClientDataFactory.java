package com.finovara.securitymonitoring.clientdata.factory;

import com.finovara.securitymonitoring.login.model.LoginProfile;
import com.finovara.securitymonitoring.login.repository.LoginProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientDataFactory {

    private final LoginProfileRepository loginProfileRepository;

    @Transactional
    public void createDefaultDataIfNotExist(Long userId) {
        if (loginProfileRepository.existsByUserId(userId)) {
            log.debug("Login profile already exists for userId={}, skipping", userId);
            return;
        }

        LoginProfile loginProfile = LoginProfile.builder()
                .loginCount(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .userId(userId)
                .clientData(new ArrayList<>())
                .build();

        loginProfileRepository.save(loginProfile);
        log.info("Default login profile created for userId={}", userId);
    }
}