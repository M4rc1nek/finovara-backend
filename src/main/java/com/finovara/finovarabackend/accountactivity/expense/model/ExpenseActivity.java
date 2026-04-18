package com.finovara.finovarabackend.accountactivity.expense.model;

import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expense_activity")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class ExpenseActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ExpenseActivityType type;
    private BigDecimal amount;
    private BigDecimal previousAmount;

    @Enumerated(EnumType.STRING)
    private ExpenseCategory category;
    @Enumerated(EnumType.STRING)
    private ExpenseCategory previousCategory;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;

}