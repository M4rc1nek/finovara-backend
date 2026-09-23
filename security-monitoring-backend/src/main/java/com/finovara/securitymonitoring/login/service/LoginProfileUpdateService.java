package com.finovara.securitymonitoring.login.service;

import com.finovara.contracts.activity.event.secure.login.activity.LoginActivityEvent;
import com.finovara.contracts.datadeletable.UserDataDeletable;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.activity.LoginActivityStatus;
import com.finovara.securitymonitoring.clientdata.model.ClientData;
import com.finovara.securitymonitoring.login.model.LoginProfile;
import com.finovara.securitymonitoring.login.repository.LoginProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginProfileUpdateService implements UserDataDeletable {

    private final LoginProfileRepository loginProfileRepository;

    @Transactional
    public void handleLoginEvent(LoginActivityEvent event) {
        if (event.status() != LoginActivityStatus.SUCCESSFUL) {
            return;
        }

        LoginProfile profile = loginProfileRepository.findByUserId(event.userId())
                .orElseThrow(() -> new RequestedEntityNotFoundException("Login profile not found for userId=" + event.userId()));

        if (!isKnownClient(profile, event.ipAddress(), event.browser())) {
            ClientData newClient = ClientData.builder()
                    .knownIpAddress(event.ipAddress())
                    .knownLocation(event.location())
                    .knownBrowser(event.browser())
                    .loginProfile(profile)
                    .build();
            profile.getClientData().add(newClient);
        }

        profile.setLastLoginIp(event.ipAddress());
        profile.setLastLoginLocation(event.location());
        profile.setLastLoginBrowser(event.browser());
        profile.setLastLoginAt(event.occurredAt());
        profile.setLoginCount(profile.getLoginCount() + 1);
        profile.setUpdatedAt(LocalDateTime.now());

        loginProfileRepository.save(profile);
    }

    private boolean isKnownClient(LoginProfile profile, String ip, String browser) {
        return profile.getClientData().stream()
                .anyMatch(cd -> ip.equals(cd.getKnownIpAddress()) && browser.equals(cd.getKnownBrowser()));
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        loginProfileRepository.deleteByUserId(userId);
        log.info("Deleted login profile for userId={}", userId);
    }
}
