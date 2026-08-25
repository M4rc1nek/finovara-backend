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

    private boolean notifyOnPasswordChange;
    private boolean notifyOnUsernameChange;
    private boolean notifyOnEmailChange;
    private boolean notifyOnAccountDeleted;
    private boolean notifyOnWalletLowBalance;
    private BigDecimal walletLowBalanceThreshold;

    @Column(nullable = false)
    private Long userId;
}
