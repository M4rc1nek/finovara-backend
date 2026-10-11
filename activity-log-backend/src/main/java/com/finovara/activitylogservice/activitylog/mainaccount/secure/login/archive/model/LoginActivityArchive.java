package com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.model;

import com.finovara.contracts.mainaccount.activity.model.LoginActivityStatus;
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

    private String browser;

    private String ipAddress;

    private String location;

    private LocalDateTime moveToArchiveDate;

    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Long userId;
}


