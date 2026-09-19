package com.finovara.securitymonitoring.riskengine.model;

import com.finovara.contracts.securitymonitoring.dto.RiskAction;
import com.finovara.contracts.securitymonitoring.dto.RiskTriggerType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_operation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sourceEventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskTriggerType triggerType;

    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskAction action;

    @Column(nullable = false)
    private boolean passwordConfirmed;

    @Column(nullable = false)
    private boolean emailCodeConfirmed;

    private String emailCode;
    private LocalDateTime emailCodeExpiresAt;

    @Column(nullable = false)
    private LocalDate operationDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Long userId;

    public boolean requiresPassword() {
        return action == RiskAction.SOFT_CHALLENGE || action == RiskAction.FULL_VERIFICATION_REQUIRED;
    }

    public boolean requiresEmailCode() {
        return action == RiskAction.AUTHORIZATION_REQUIRED || action == RiskAction.FULL_VERIFICATION_REQUIRED;
    }

    public boolean isFullyVerified() {
        boolean passwordVerificationComplete = !requiresPassword() || passwordConfirmed;
        boolean emailVerificationComplete = !requiresEmailCode() || emailCodeConfirmed;
        return passwordVerificationComplete && emailVerificationComplete;
    }
}