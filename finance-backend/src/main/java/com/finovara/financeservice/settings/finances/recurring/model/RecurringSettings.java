package com.finovara.financeservice.settings.finances.recurring.model;

import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.RecurringType;
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
    private LocalDate endDate;
    private LocalDate nextExecutionDate;

    @Column(nullable = false)
    private boolean skippedNotificationSent;

    @Column(nullable = false)
    private LocalDate createdAt;

    @Column(nullable = false)
    private Long userId;

}
