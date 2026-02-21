package com.finovara.finovarabackend.accountactivity.accountchanges.activities.model;

import com.finovara.finovarabackend.user.model.User;
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

    private LocalDateTime date;

    private String browser;

    private String ipAddress;

    private String location;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;
}
