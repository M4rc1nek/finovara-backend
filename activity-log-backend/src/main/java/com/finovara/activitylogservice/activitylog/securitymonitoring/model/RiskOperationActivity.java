package com.finovara.activitylogservice.activitylog.securitymonitoring.model;

import com.finovara.contracts.securitymonitoring.model.RiskAction;
import com.finovara.contracts.securitymonitoring.model.RiskTriggerType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "risk_operation_activity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskOperationActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String sourceEventId;

    @Enumerated(EnumType.STRING)
    private RiskTriggerType triggerType;

    private int score;

    @Enumerated(EnumType.STRING)
    private RiskAction action;

    @Column(nullable = false)
    private LocalDate operationDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "riskOperationActivity", cascade = CascadeType.PERSIST)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @BatchSize(size = 100)
    private List<RiskRuleCollectionActivity> riskRuleCollectionActivities = new ArrayList<>();

}