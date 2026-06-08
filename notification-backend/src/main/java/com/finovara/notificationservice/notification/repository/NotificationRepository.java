package com.finovara.notificationservice.notification.repository;

import com.finovara.notificationservice.notification.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Modifying
    @Query("DELETE from Notification n where n.userId = :userId")
    void deleteByUserId(Long userId);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId")
    List<Notification> findAllByUserAssignedId(Long userId, Pageable pageable);

    @Query("SELECT n.deduplicationKey FROM Notification n WHERE n.userId = :userId")
    Set<String> findAllDeduplicationKeysByUserAssignedId(Long userId);
}

