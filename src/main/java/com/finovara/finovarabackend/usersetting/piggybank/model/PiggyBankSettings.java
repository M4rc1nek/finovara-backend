package com.finovara.finovarabackend.usersetting.piggybank.model;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.completion.model.GoalCompletionStrategy;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "piggy_bank_settings")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class PiggyBankSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean automationActive;

    @Column(nullable = false)
    private BigDecimal automationPercentage;

    @Column(nullable = false)
    private boolean roundUpActive;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GoalCompletionStrategy goalCompletionStrategy;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;

}
