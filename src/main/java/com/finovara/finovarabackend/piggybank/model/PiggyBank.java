package com.finovara.finovarabackend.piggybank.model;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PiggyBankGoalType goalType;

    private BigDecimal goalAmount;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;

    @OneToOne(mappedBy = "piggyBankAssigned", cascade = CascadeType.ALL)
    private PiggyBankSettings settings;

}
