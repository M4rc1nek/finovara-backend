package com.finovara.finovarabackend.usersettings.finances.expense.model;

import com.finovara.finovarabackend.user.model.User;
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

    @OneToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;
}
