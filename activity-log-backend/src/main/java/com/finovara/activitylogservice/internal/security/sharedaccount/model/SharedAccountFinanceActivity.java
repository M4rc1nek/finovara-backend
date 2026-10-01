package com.finovara.activitylogservice.internal.security.sharedaccount.model;

import com.finovara.contracts.activity.event.sharedaccount.SharedFinanceActivityType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shared_account_finance_activity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedAccountFinanceActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private SharedFinanceActivityType activityType;

    private BigDecimal amount;

    private LocalDateTime createdAt;

    private Long ownerId;
    private Long memberId;
    private Long transactionId;
    private Long userId;
}
