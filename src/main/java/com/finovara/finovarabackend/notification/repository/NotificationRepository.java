package com.finovara.finovarabackend.notification.repository;

import com.finovara.finovarabackend.notification.model.Notification;
import com.finovara.finovarabackend.notification.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.userAssigned.id = :userId")
    List<Notification> getAllNotifications(Long userId);

    @Query("SELECT n.type FROM Notification n WHERE n.userAssigned.id = :userId")
    Set<NotificationType> findAllTypesByUserAssignedId(Long userId);
}

