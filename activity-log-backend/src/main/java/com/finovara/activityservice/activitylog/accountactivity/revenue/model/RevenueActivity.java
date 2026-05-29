package com.finovara.activityservice.activitylog.accountactivity.revenue.model;

import com.finovara.contracts.model.activity.RevenueActivityType;
import com.finovara.contracts.model.transaction.RevenueCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "revenue_activity")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class RevenueActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private RevenueActivityType type;

    private BigDecimal amount;
    private BigDecimal previousAmount;

    @Enumerated(EnumType.STRING)
    private RevenueCategory category;
    @Enumerated(EnumType.STRING)
    private RevenueCategory previousCategory;

    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Long userId;

}
