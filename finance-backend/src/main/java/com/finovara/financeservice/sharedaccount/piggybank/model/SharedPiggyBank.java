package com.finovara.financeservice.sharedaccount.model.piggybank;

import com.finovara.contracts.model.transaction.PiggyBankGoalType;
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
    @Column(nullable = false)
    private BigDecimal amount;

    private LocalDate createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PiggyBankGoalType goalType;

    private BigDecimal goalAmount;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private Long memberId;
}
