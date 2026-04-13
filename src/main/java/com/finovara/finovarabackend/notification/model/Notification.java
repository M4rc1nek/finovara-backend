package com.finovara.finovarabackend.notification.model;

import com.finovara.finovarabackend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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
    private LocalDate createdAt;

    @Column(name = "deduplication_key", nullable = false, length = 255)
    private String deduplicationKey;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String payload;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User userAssigned;

}
