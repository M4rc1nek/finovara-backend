package com.finovara.corebackend.user.model;

import com.finovara.corebackend.expense.model.Expense;
import com.finovara.corebackend.limit.model.Limit;
import com.finovara.corebackend.notification.model.Notification;
import com.finovara.corebackend.piggybank.model.PiggyBank;
import com.finovara.corebackend.revenue.model.Revenue;
import com.finovara.corebackend.usersetting.account.model.AccountSettings;
import com.finovara.corebackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.corebackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.corebackend.usersetting.notificationemail.model.NotificationEmailSettings;
import com.finovara.corebackend.wallet.model.Wallet;
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
    private boolean passwordSet;
    private String email;
    private LocalDateTime createdAt;
    @Column(name = "profile_image_path")
    private String profileImagePath;
    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider")
    private OAuthProvider oauthProvider;
    @Column(name = "provider_user_id")
    private String providerUserId;

    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private List<Expense> expenses;

    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private List<Revenue> revenues;
    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PiggyBank> piggyBanks;

    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private List<Limit> limits;

    @OneToOne(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private Wallet wallet;

    @OneToOne(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private ExpenseSettings expenseSettings;

    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private List<RecurringSettings> recurringSettings;

    @OneToOne(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private AccountSettings accountSettings;

    @OneToOne(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private NotificationEmailSettings notificationEmailSettings;

    @OneToMany(mappedBy = "userAssigned", cascade = CascadeType.ALL)
    private List<Notification> notifications;

}
