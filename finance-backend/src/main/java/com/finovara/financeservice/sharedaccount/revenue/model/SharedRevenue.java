package com.finovara.financeservice.sharedaccount.revenue.model;

import com.finovara.contracts.util.model.RevenueCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shared_revenues")
public class SharedRevenue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RevenueCategory category;

    @Column(nullable = false)
    private LocalDate createdAt;

    private String description;

    @Version
    private Long version;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private Long createdByUserId;
}