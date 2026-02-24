package com.finovara.finovarabackend.user.model;

import com.finovara.finovarabackend.accountactivity.accountchange.activities.model.AccountChangesActivity;
import com.finovara.finovarabackend.accountactivity.accountchange.archive.model.AccountChangeArchive;
import com.finovara.finovarabackend.accountactivity.expense.model.ExpenseActivity;
import com.finovara.finovarabackend.accountactivity.login.activities.model.LoginActivity;
import com.finovara.finovarabackend.accountactivity.login.archive.model.LoginActivityArchive;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.usersetting.account.model.AccountSettings;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
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
    @Column(name = "profile_image_path")
    private String profileImagePath;

    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private List<Expense> expenses;

    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private List<Revenue> revenues;
    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private List<PiggyBank> piggyBanks;

    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private List<Limit> limits;

    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private List<LoginActivity> loginActivities;

    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private List<LoginActivityArchive> loginActivityArchives;

    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private List<AccountChangesActivity> accountChangeActivities;

    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private List<AccountChangeArchive> accountChangeArchives;

    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private List<ExpenseActivity> expenseActivities;

    @OneToOne(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private Wallet wallet;

    @OneToOne(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private ExpenseSettings expenseSettings;

    @OneToOne(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private PiggyBankSettings piggyBankSettings;

    @OneToOne(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private AccountSettings accountSettings;

}
