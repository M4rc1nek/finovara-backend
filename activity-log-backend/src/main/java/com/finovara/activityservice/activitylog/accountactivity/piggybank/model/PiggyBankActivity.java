package com.finovara.activityservice.activitylog.accountactivity.piggybank.model;

import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.contracts.model.transaction.PiggyBankGoalType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "piggy_bank_activity")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class PiggyBankActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String piggyBankName;

    @Enumerated(EnumType.STRING)
    private PiggyBankActivityType activityType;
    @Enumerated(EnumType.STRING)
    private PiggyBankGoalType goalType;
    @Enumerated(EnumType.STRING)
    private PiggyBankGoalType previousGoalType;

    private BigDecimal goalAmount;
    private BigDecimal amountPaid;
    private BigDecimal amountPaidOut;

    private BigDecimal previousGoalAmount;
    private String previousPiggyBankName;

    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Long userId;
}
