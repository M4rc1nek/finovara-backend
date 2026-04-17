package com.finovara.finovarabackend.accountactivity.secure.accountchange.archive.model;

import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.model.AccountChangesActivityType;
import com.finovara.finovarabackend.user.model.User;
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

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;
}
