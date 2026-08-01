package com.finovara.authservice.settings.secutiy;

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

    @Column(nullable = false)
    private String additionalAuthorizationCodeHash;

    @Column(nullable = false)
    private LocalDateTime additionalCodeGeneratedAt;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;
}

