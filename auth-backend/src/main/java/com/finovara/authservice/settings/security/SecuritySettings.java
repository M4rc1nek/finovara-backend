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
    private int additionalAuthorizationAttempts;
    private LocalDateTime additionalAuthorizationEmailCodeExpiresAt;
    private LocalDateTime additionalAuthorizationAttemptsExpiresAt;

    @Version
    private Long version;
}

