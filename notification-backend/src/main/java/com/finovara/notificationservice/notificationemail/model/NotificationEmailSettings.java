package com.finovara.notificationservice.notificationemail.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "notification_settings")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class NotificationEmailSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean notifyOnPasswordChange;

    @Column(nullable = false)
    private boolean notifyOnUsernameChange;

    @Column(nullable = false)
    private boolean notifyOnEmailChange;

    @Column(nullable = false)
    private boolean notifyOnAccountDeleted;

    @Column(nullable = false)
    private boolean notifyOnWalletLowBalance;

    private BigDecimal walletLowBalanceThreshold;

    @Column(nullable = false, unique = true)
    private Long userId;
}