package com.finovara.securitymonitoring.accountchange.factory;

import com.finovara.securitymonitoring.accountchange.model.AccountChangeProfile;
import com.finovara.securitymonitoring.accountchange.repository.AccountChangeProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountChangeProfileFactory {

    private final AccountChangeProfileRepository accountChangeProfileRepository;

    @Transactional
    public void createDefaultDataIfNotExist(Long userId) {
        if (accountChangeProfileRepository.existsByUserId(userId)) {
            log.debug("Account change profile already exists for userId={}, skipping", userId);
            return;
        }

        AccountChangeProfile profile = AccountChangeProfile.builder()
                .passwordChangeCount(0L)
                .emailChangeCount(0L)
                .usernameChangeCount(0L)
                .profileImageChangeCount(0L)
                .totalChangeCount(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .userId(userId)
                .build();

        accountChangeProfileRepository.save(profile);
        log.info("Default account change profile created for userId={}", userId);
    }
}