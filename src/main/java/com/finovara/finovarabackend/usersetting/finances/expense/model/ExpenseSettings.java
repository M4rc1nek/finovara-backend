package com.finovara.finovarabackend.usersetting.finances.expense.model;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.model.CountQuantityLimitStrategy;
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
    private boolean expenseAmountThresholdEnabled;

    @Column(nullable = false)
    private BigDecimal blockedAmount;

    @Column(nullable = false)
    private boolean smartScanEnabled;

    @Column(nullable = false)
    private boolean expenseCountQuantityLimitEnabled;

    private int numberOfQuantityLimit;

    @Enumerated(EnumType.STRING)
    private CountQuantityLimitStrategy countQuantityLimitStrategy;

    private boolean expenseQuantityLimitEmergencyModeEnabled;
    private boolean expenseQuantityLimitEmergencyModeUsed;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;
}
