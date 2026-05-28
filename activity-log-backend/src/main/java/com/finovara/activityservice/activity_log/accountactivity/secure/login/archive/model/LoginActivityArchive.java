package com.finovara.activityservice.activity_log.accountactivity.secure.login.archive.model;

import com.finovara.contracts.model.activity.LoginActivityStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "login_activity_archive")
@Builder
public class LoginActivityArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;

    @Enumerated(EnumType.STRING)
    private LoginActivityStatus status;

    private LocalDateTime moveToArchiveDate;

    private LocalDateTime activityLoginDate;

    private String browser;

    private String ipAddress;

    private String location;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}


