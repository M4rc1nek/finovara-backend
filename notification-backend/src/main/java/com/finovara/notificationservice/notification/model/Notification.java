package com.finovara.notificationservice.notification.model;

import com.finovara.contracts.model.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "deduplication_key"}))
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deduplication_key", nullable = false, length = 255)
    private String deduplicationKey;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private Long userId;

}
