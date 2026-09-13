package com.finovara.securitymonitoring.accountchange.model;

import com.finovara.contracts.model.activity.AccountChangesActivityType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_change_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountChangeProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long passwordChangeCount = 0L;

    private LocalDateTime lastPasswordChangeAt;

    @Column(nullable = false)
    private Long emailChangeCount = 0L;

    private LocalDateTime lastEmailChangeAt;

    @Column(nullable = false)
    private Long usernameChangeCount = 0L;

    private LocalDateTime lastUsernameChangeAt;

    @Column(nullable = false)
    private Long profileImageChangeCount = 0L;

    private LocalDateTime lastProfileImageChangeAt;

    @Column(nullable = false)
    private Long totalChangeCount = 0L;

    @Enumerated(EnumType.STRING)
    private AccountChangesActivityType lastChangeType;

    private LocalDateTime lastChangeAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false, unique = true)
    private Long userId;
}
