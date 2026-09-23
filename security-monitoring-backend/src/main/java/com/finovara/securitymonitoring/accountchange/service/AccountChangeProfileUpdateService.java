package com.finovara.securitymonitoring.accountchange.service;

import com.finovara.contracts.activity.event.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.datadeletable.UserDataDeletable;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.securitymonitoring.accountchange.model.AccountChangeProfile;
import com.finovara.securitymonitoring.accountchange.repository.AccountChangeProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountChangeProfileUpdateService implements UserDataDeletable {

    private final AccountChangeProfileRepository accountChangeProfileRepository;

    @Transactional
    public void handleAccountChangeEvent(AccountChangesActivityEvent event) {
        AccountChangeProfile profile = accountChangeProfileRepository.findByUserId(event.userId())
                .orElseThrow(() -> new RequestedEntityNotFoundException("Account change profile not found for userId=" + event.userId()));

        switch (event.type()) {
            case PASSWORD_CHANGED -> {
                profile.setPasswordChangeCount(profile.getPasswordChangeCount() + 1);
                profile.setLastPasswordChangeAt(event.occurredAt());
            }
            case EMAIL_CHANGED -> {
                profile.setEmailChangeCount(profile.getEmailChangeCount() + 1);
                profile.setLastEmailChangeAt(event.occurredAt());
            }
            case USERNAME_CHANGED -> {
                profile.setUsernameChangeCount(profile.getUsernameChangeCount() + 1);
                profile.setLastUsernameChangeAt(event.occurredAt());
            }
            case PROFILE_IMG_CHANGED -> {
                profile.setProfileImageChangeCount(profile.getProfileImageChangeCount() + 1);
                profile.setLastProfileImageChangeAt(event.occurredAt());
            }
        }

        profile.setTotalChangeCount(profile.getTotalChangeCount() + 1);
        profile.setLastChangeType(event.type());
        profile.setLastChangeAt(event.occurredAt());
        profile.setUpdatedAt(LocalDateTime.now());

        accountChangeProfileRepository.save(profile);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        accountChangeProfileRepository.deleteByUserId(userId);
        log.info("Deleted account-change profile for userId={}", userId);
    }
}