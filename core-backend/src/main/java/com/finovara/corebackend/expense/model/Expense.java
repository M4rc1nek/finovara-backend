package com.finovara.corebackend.expense.model;

import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.corebackend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "expenses")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private ExpenseCategory category;

    private LocalDate createdAt;
    private String description;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;

}
