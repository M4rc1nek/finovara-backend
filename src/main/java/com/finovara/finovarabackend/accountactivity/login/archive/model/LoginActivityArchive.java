package com.finovara.finovarabackend.accountactivity.login.archive.model;

import com.finovara.finovarabackend.accountactivity.login.activities.model.LoginActivityStatus;
import com.finovara.finovarabackend.user.model.User;
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

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;
}


