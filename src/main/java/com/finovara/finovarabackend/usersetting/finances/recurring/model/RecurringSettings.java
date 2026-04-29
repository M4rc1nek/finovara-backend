package com.finovara.finovarabackend.usersetting.finances.recurring.model;

import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.model.PeriodType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "recurring_settings")
@Builder
public class RecurringSettings  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean enable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurringType type;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private RevenueCategory revenueCategory;

    @Enumerated(EnumType.STRING)
    private ExpenseCategory expenseCategory;

    private Long piggyBankId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurring_period")
    private PeriodType periodType;

    private LocalDate startDate;
    private LocalDate nextExecutionDate;

    @Column(nullable = false)
    private LocalDate createdAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;

}
