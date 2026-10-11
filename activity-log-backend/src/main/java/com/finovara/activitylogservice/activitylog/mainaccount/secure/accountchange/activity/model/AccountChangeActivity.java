package com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.model;

import com.finovara.contracts.mainaccount.activity.model.AccountChangeActivityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_change_activity")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class AccountChangeActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private AccountChangeActivityType type;

    private LocalDateTime createdAt;

    private String browser;

    private String ipAddress;

    private String location;

    @Column(nullable = false)
    private Long userId;
}
