package com.finovara.financeservice.piggybank.model;

import com.finovara.contracts.util.model.PiggyBankGoalType;
import com.finovara.financeservice.piggybank.goalplanner.model.GoalPlanner;
import com.finovara.financeservice.settings.piggybank.model.PiggyBankSettings;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Setter
@Getter
@Table(name = "piggybanks")
public class PiggyBank {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    private LocalDate createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PiggyBankGoalType goalType;

    @Column(precision = 19, scale = 2)
    private BigDecimal goalAmount;

    @Column(nullable = false)
    private Long userId;

    @Version
    private Long version;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "piggy_bank_settings_id", unique = true)
    private PiggyBankSettings settings;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_planner_id", unique = true)
    private GoalPlanner goalPlanner;

}