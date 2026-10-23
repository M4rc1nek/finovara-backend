package com.finovara.financeservice.sharedaccount.expense.model;

import com.finovara.contracts.util.model.ExpenseCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "shared_expenses")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SharedExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory category;

    @Column(nullable = false)
    private LocalDate createdAt;

    private String description;

    @Version
    private Long version;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long createdByUserId;
}