package com.finovara.securitymonitoring.riskengine.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private LocalDate operationDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "riskOperation", cascade = CascadeType.ALL)
    private List<TriggeredRule> triggeredRules;

    @Column(nullable = false)
    private Long userId;
}