package com.finovara.financeservice.revenue.model;

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
@Table(name = "revenues")
public class Revenue {

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

    @Column(length = 500)
    private String description;

    @Version
    private Long version;

    @Column(nullable = false)
    private Long userId;

}