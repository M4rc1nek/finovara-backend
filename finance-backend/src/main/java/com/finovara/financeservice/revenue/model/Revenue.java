package com.finovara.financeservice.revenue.model;

import com.finovara.contracts.model.transaction.RevenueCategory;
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
@Table(name = "revenues")
public class Revenue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private RevenueCategory category;
    private LocalDate createdAt;
    private String description;

    @Column(nullable = false)
    private Long userId;
}
