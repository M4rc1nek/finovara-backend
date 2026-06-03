package com.finovara.notificationservice.notificationemail.repository;

import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationEmailSettingsRepository extends JpaRepository<NotificationEmailSettings, Long> {
    Optional<NotificationEmailSettings> findByUserId(Long userId);
}

