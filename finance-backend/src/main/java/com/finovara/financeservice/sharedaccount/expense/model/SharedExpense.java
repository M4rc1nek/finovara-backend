package com.finovara.financeservice.sharedaccount.expense.model;

import com.finovara.contracts.model.transaction.ExpenseCategory;
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

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private ExpenseCategory category;

    private LocalDate createdAt;
    private String description;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long createdByUserId;

}
