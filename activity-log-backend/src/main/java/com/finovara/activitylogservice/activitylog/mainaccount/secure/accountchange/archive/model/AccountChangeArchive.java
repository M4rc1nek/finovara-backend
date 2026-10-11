package com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.model;

import com.finovara.contracts.mainaccount.activity.model.AccountChangeActivityType;
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
    private AccountChangeActivityType type;

    private String browser;

    private String ipAddress;

    private String location;

    private LocalDateTime moveToArchiveDate;

    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Long userId;
}
