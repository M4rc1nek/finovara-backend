package com.finovara.activitylogservice.activitylog.securitymonitoring.model;

import com.finovara.contracts.securitymonitoring.model.RiskRule;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "risk_rule_collection_activity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskRuleCollectionActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private RiskRule riskRule;

    private int score;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_operation_activity_id", nullable = false)
    private RiskOperationActivity riskOperationActivity;

}
