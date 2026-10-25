package com.finovara.notificationservice.notification.repository;

import com.finovara.notificationservice.notification.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId ORDER BY n.createdAt DESC, n.id DESC")
    List<Notification> findNewestByUserId(Long userId, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId ORDER BY n.createdAt ASC, n.id ASC")
    List<Notification> findOldestByUserId(Long userId, Pageable pageable);

    @Query("SELECT n.id FROM Notification n WHERE n.userId = :userId")
    List<Long> findIdsByUserId(Long userId);

    @Query("SELECT n.deduplicationKey FROM Notification n WHERE n.userId = :userId AND n.deduplicationKey IN :deduplicationKeys")
    List<String> findExistingDeduplicationKeys(Long userId, List<String> deduplicationKeys);
}