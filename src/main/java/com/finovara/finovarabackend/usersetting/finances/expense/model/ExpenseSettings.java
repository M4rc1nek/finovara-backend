package com.finovara.finovarabackend.usersetting.finances.expense.model;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.model.PeriodType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "expense_settings")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class ExpenseSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "amount_threshold_enabled",nullable = false)
    private boolean expenseAmountThresholdEnabled;

    @Column(nullable = false)
    private BigDecimal blockedAmount;

    @Column(nullable = false)
    private boolean smartScanEnabled;

    @Column(name = "count_quantity_limit_enabled",nullable = false)
    private boolean expenseCountQuantityLimitEnabled;

    private int numberOfQuantityLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "count_quantity_limit_period")
    private PeriodType periodType;

    @Column(name = "quantity_limit_emergency_mode_enabled")
    private boolean expenseQuantityLimitEmergencyModeEnabled;

    @Column(name = "quantity_limit_emergency_mode_used")
    private boolean expenseQuantityLimitEmergencyModeUsed;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;
}
