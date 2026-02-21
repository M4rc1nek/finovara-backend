package com.finovara.finovarabackend.accountactivity.accountchanges.archive.model;

import com.finovara.finovarabackend.accountactivity.accountchanges.activities.model.UserActivityAccountChangesType;
import com.finovara.finovarabackend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "archive_user_account_changes_activity")
@Builder
public class ArchiveAccountChangesActivities {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private UserActivityAccountChangesType type;

    private LocalDateTime moveToArchiveDate;

    private LocalDateTime activityAccountChangesDate;

    private String browser;

    private String ipAddress;

    private String location;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;
}
