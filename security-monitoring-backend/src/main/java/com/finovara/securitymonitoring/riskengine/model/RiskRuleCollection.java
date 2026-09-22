package com.finovara.securitymonitoring.riskengine.model;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "risk_rule_collection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskRuleCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private RiskRule riskRule;

    private int scorePerRule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_operation_id", nullable = false)
    private RiskOperation riskOperation;

}
