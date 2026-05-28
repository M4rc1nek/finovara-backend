package com.finovara.activityservice.activity_log.accountactivity.secure.accountchange.activity.model;

import com.finovara.contracts.model.activity.AccountChangesActivityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_changes_activity")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class AccountChangesActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private AccountChangesActivityType type;

    private LocalDateTime createdAt;

    private String browser;

    private String ipAddress;

    private String location;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}
