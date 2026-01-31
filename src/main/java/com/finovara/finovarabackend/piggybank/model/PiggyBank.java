package com.finovara.finovarabackend.piggybank.model;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersettings.piggybank.completion.model.GoalCompletionStrategy;
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
@Table(name = "piggyBanks")
public class PiggyBank {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    @Column(nullable = false)
    private BigDecimal amount;
    private LocalDate createdAt;

    private GoalType goalType;

    private BigDecimal goalAmount;

    private boolean automationActive;
    private BigDecimal automationPercentage;

    private boolean roundUpActive;

    private GoalCompletionStrategy goalCompletionStrategy;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;

}
