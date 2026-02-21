package com.finovara.finovarabackend.accountactivity.accountchanges.model;

import com.finovara.finovarabackend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_activity_account_changes")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class UserActivityAccountChanges {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private UserActivityAccountChangesType type;

    private LocalDateTime date;

    private String browser;

    private String ipAddress;

    private String location;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;
}
