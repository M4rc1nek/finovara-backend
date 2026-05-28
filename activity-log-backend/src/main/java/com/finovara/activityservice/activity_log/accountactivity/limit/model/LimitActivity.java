package com.finovara.activityservice.activity_log.accountactivity.limit.model;

import com.finovara.contracts.model.activity.LimitActivityType;
import com.finovara.contracts.model.PeriodType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Entity
@Table(name = "limit_activity")
public class LimitActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private LimitActivityType limitActivityType;

    @Enumerated(EnumType.STRING)
    private PeriodType periodType;

    private BigDecimal amount;
    private BigDecimal previousAmount;

    private LocalDateTime createdAt;

    @Column(name = "user_id", nullable = false)
    private Long userId;

}
