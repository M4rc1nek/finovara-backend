package com.finovara.authservice.settings.security;

import com.finovara.authservice.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_settings")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class SecuritySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean additionalAuthorizationEnabled;

    private String additionalAuthorizationCode;

    private Integer additionalAuthorizationEmailCode;
    private int  additionalAuthorizationAttempts;
    private LocalDateTime additionalAuthorizationEmailCodeExpiresAt;
    private LocalDateTime additionalAuthorizationAttemptsExpiresAt;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;
}

