package com.finovara.finovarabackend.usersetting.finances.revenue.model;

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
@Table(name = "revenue_settings")
@Builder
public class RevenueSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean recurringRevenuesEnable;

    private BigDecimal recurringAmount;

    @Enumerated(EnumType.STRING)
    private RevenueCategory revenueCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurring_revenue_period")
    private PeriodType periodType;

    private LocalDate recurringStartDate;
    private LocalDate nextExecutionDate;

    @Column(nullable = false)
    private LocalDate createdAt;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;

}
