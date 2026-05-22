package com.finovara.finovarabackend.accountactivity.limit.model;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.model.PeriodType;
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

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;

}
