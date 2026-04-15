package com.finovara.finovarabackend.notification.repository;

import com.finovara.finovarabackend.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.userAssigned.id = :userId")
    List<Notification> getAllNotifications(Long userId);

    @Query("SELECT n.deduplicationKey FROM Notification n WHERE n.userAssigned.id = :userId")
    Set<String> findAllDeduplicationKeysByUserAssignedId(Long userId);
}

