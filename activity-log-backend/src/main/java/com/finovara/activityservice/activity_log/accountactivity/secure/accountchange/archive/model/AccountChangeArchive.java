package com.finovara.activityservice.activity_log.accountactivity.secure.accountchange.archive.model;

import com.finovara.contracts.model.activity.AccountChangesActivityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "account_change_archive")
@Builder
public class AccountChangeArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private AccountChangesActivityType type;

    private LocalDateTime moveToArchiveDate;

    private LocalDateTime activityAccountChangesDate;

    private String browser;

    private String ipAddress;

    private String location;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}
