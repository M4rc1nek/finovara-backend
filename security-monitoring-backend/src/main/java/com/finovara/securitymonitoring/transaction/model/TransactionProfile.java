package com.finovara.securitymonitoring.transaction.model;

import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.RevenueCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(precision = 19, scale = 2)
    private BigDecimal averageExpenseAmount;

    @Column(nullable = false)
    private Long expenseCount = 0L;

    @Column(precision = 19, scale = 2)
    private BigDecimal largestExpenseAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal lastExpenseAmount;

    @Enumerated(EnumType.STRING)
    private ExpenseCategory lastExpenseCategory;

    private LocalDateTime lastExpenseAt;

    @Column(precision = 19, scale = 2)
    private BigDecimal averageRevenueAmount;

    @Column(nullable = false)
    private Long revenueCount = 0L;

    @Column(precision = 19, scale = 2)
    private BigDecimal lastRevenueAmount;

    @Enumerated(EnumType.STRING)
    private RevenueCategory lastRevenueCategory;

    private LocalDateTime lastRevenueAt;

    @Column(precision = 19, scale = 2)
    private BigDecimal largestPiggyBankDeposit;

    @Column(precision = 19, scale = 2)
    private BigDecimal lastPiggyBankDepositAmount;

    private LocalDateTime lastPiggyBankDepositAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false, unique = true)
    private Long userId;
}
