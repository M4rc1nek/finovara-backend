package com.finovara.finovarabackend.user.model;

import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.wallet.model.Wallet;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;
    private String email;
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.REMOVE)
    private List<Expense> expenses;

    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.REMOVE)
    private List<Revenue> revenues;

    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private List<PiggyBank> piggyBanks;

    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private List<Limit> limits;

    @OneToOne(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private Wallet wallet;
}
