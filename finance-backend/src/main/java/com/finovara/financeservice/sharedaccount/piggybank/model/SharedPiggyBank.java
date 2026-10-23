package com.finovara.financeservice.sharedaccount.piggybank.model;

import com.finovara.contracts.util.model.PiggyBankGoalType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Setter
@Getter
@Table(name = "shared_piggybanks")
public class SharedPiggyBank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    private LocalDate createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PiggyBankGoalType goalType;

    @Column(precision = 19, scale = 2)
    private BigDecimal goalAmount;

    @Column(nullable = false)
    private boolean goalAchievedNotified;

    @Version
    private Long version;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private Long memberId;
}