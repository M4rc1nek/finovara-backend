package com.finovara.financeservice.sharedaccount.limit.model;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "shared_limits")
public class SharedLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PeriodType periodType;

    @Enumerated(EnumType.STRING)
    private ExpenseCategory category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private Long memberId;

}
