package com.finovara.activityservice.activity_log.accountactivity.expense.model;

import com.finovara.contracts.model.activity.ExpenseActivityType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
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

    @Column(name = "user_id", nullable = false)
    private Long userId;

}