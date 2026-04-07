package com.finovara.finovarabackend.notification.repository;

import com.finovara.finovarabackend.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
