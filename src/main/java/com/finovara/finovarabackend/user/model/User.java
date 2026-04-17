package com.finovara.finovarabackend.user.model;

import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.model.AccountChangesActivity;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.archive.model.AccountChangeArchive;
import com.finovara.finovarabackend.accountactivity.expense.model.ExpenseActivity;
import com.finovara.finovarabackend.accountactivity.limit.model.LimitActivity;
import com.finovara.finovarabackend.accountactivity.secure.login.activity.model.LoginActivity;
import com.finovara.finovarabackend.accountactivity.secure.login.archive.model.LoginActivityArchive;
import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivity;
import com.finovara.finovarabackend.accountactivity.revenue.model.RevenueActivity;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingsActivity;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.notification.model.Notification;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.usersetting.account.model.AccountSettings;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.usersetting.finances.revenue.model.RevenueSettings;
import com.finovara.finovarabackend.usersetting.notificationemail.model.NotificationEmailSettings;
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
    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.ALL, orphanRemoval = true)
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

    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private List<RevenueActivity> revenueActivities;

    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private List<LimitActivity> limitActivities;

    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private List<PiggyBankActivity> piggyBankActivities;

    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private List<SettingsActivity> settingsActivities;

    @OneToOne(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private Wallet wallet;

    @OneToOne(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private ExpenseSettings expenseSettings;

    @OneToOne(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private RevenueSettings revenueSettings;

    @OneToOne(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private AccountSettings accountSettings;

    @OneToOne(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private NotificationEmailSettings notificationEmailSettings;

    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private List<Notification> notifications;

}
