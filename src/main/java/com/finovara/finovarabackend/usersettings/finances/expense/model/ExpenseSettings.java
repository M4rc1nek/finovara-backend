package com.finovara.finovarabackend.usersettings.finances.expense.model;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersettings.finances.expense.countlimit.model.CountQuantityLimitStrategy;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "expense_settings")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
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

    @OneToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;
}
