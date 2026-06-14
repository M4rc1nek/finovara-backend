package com.finovara.authbackend.usersetting.finances.expense.model;

import com.finovara.contracts.model.PeriodType;
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

    @Column(nullable = false)
    private boolean amountThresholdEnabled;

    @Column(nullable = false)
    private BigDecimal blockedAmount;

    @Column(nullable = false)
    private boolean smartScanEnabled;

    @Column(nullable = false)
    private boolean countQuantityLimitEnabled;

    @Column(nullable = false)
    private int numberOfQuantityLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "count_quantity_limit_period")
    private PeriodType periodType;

    private boolean quantityLimitEmergencyModeEnabled;

    private boolean quantityLimitEmergencyModeUsed;

    @Column(nullable = false)
    private Long userId;
}
