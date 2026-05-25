package com.finovara.activityservice.activity_log.accountactivity.secure.login.activity.model;

import com.finovara.activityservice.contracts.model.activity.LoginActivityStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Entity
@Table(name = "login_activity")
public class LoginActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;

    @Enumerated(EnumType.STRING)
    private LoginActivityStatus status;

    private LocalDateTime createdAt;

    private String browser;

    private String ipAddress;

    private String location;

    @Column(name = "user_id", nullable = false)
    private Long userId;

}
