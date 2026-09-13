package com.finovara.securitymonitoring.riskengine.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "triggered_rule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TriggeredRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ruleCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_operation_id", nullable = false)
    private RiskOperation riskOperation;
}