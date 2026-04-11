package com.finovara.finovarabackend.notification.model;

import com.finovara.finovarabackend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "notifications")
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
    private NotificationType type;

    private LocalDate createdAt;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String payload;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;

}
