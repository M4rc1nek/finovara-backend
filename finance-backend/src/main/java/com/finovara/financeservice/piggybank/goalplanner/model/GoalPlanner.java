package com.finovara.financeservice.piggybank.goalplanner.model;

import com.finovara.financeservice.piggybank.model.PiggyBank;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Setter
@Getter
@Table(name = "goalplanner")
public class GoalPlanner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String topic;
    private LocalDate targetDate;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private Long userId;

    @OneToOne
    @JoinColumn(name = "piggy_bank_id", unique = true)
    private PiggyBank piggyBankAssigned;
}