package com.finovara.notificationservice.kafka;

import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.repository.NotificationEmailSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEmailSettingsService {

    private final NotificationEmailSettingsRepository notificationEmailSettingsRepository;

    @Transactional
    public void createSettingsIfNotExist(Long userId) {
        try {
            notificationEmailSettingsRepository.save(NotificationEmailSettings.builder()
                    .userId(userId)
                    .notifyOnPasswordChange(false)
                    .notifyOnUsernameChange(false)
                    .notifyOnEmailChange(false)
                    .notifyOnAccountDeleted(false)
                    .build());
            log.info("Notification settings created for userId={}", userId);
        } catch (DataIntegrityViolationException e) {
            log.debug("Notification settings already exist for userId={}, skipping", userId);
        }
    }

    @Transactional
    public void deleteSettings(NotificationEmailSettings settings) {
        notificationEmailSettingsRepository.delete(settings);
    }
}