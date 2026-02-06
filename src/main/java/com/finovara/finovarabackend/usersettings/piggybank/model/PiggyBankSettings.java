package com.finovara.finovarabackend.usersettings.piggybank.model;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersettings.piggybank.completion.model.GoalCompletionStrategy;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "piggy_bank_settings")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
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
    private GoalCompletionStrategy goalCompletionStrategy;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;

}
