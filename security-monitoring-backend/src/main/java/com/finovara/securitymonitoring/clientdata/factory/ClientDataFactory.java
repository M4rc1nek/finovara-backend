package com.finovara.securitymonitoring.factory;

import com.finovara.securitymonitoring.clientdata.model.ClientData;
import com.finovara.securitymonitoring.clientdata.repository.ClientDataRepository;
import com.finovara.securitymonitoring.login.model.LoginProfile;
import com.finovara.securitymonitoring.login.repository.LoginProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
                .lastLoginIp(null)
                .lastLoginLocation(null)
                .lastLoginBrowser(null)
                .lastLoginAt(null)
                .loginCount(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .userId(userId)
                .build();

        ClientData clientData = ClientData.builder()
                .knownIpAddress(null)
                .knownLocation(null)
                .knownBrowser(null)
                .loginProfile(loginProfile)
                .build();

        loginProfile.setClientData(List.of(clientData));

        loginProfileRepository.save(loginProfile);

        log.info("Default login profile and client data created for userId={}", userId);
    }
}

