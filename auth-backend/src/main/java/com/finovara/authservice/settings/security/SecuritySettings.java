package com.finovara.authservice.settings.security;

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

    @Column(nullable = false)
    private boolean additionalAuthorizationVisible;

    private String additionalAuthorizationCode;
    private Integer additionalAuthorizationEmailCode;

    @Column(nullable = false)
    private int additionalAuthorizationAttempts;

    private LocalDateTime additionalAuthorizationEmailCodeExpiresAt;
    private LocalDateTime additionalAuthorizationAttemptsExpiresAt;

    @Column( nullable = false)
    private boolean twoFactorEnabled;

    private String twoFactorSecret;

    private String twoFactorPendingSecret;

    private LocalDateTime twoFactorPendingExpiresAt;

    @Column(columnDefinition = "TEXT")
    private String twoFactorBackupCodes;

    @Column(nullable = false)
    private int twoFactorFailedAttempts;

    private LocalDateTime twoFactorLockedUntil;

    private Long twoFactorLastUsedStep;

    @Version
    @Column(nullable = false)
    private Long version;
}
