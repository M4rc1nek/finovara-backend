package com.finovara.finovarabackend.accountactivity.piggybank.model;

import com.finovara.finovarabackend.piggybank.model.PiggyBankGoalType;
import com.finovara.finovarabackend.user.model.User;
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

    private BigDecimal goalAmount;
    private BigDecimal amountPaid;
    private BigDecimal amountPaidOut;

    private LocalDateTime date;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;

}
