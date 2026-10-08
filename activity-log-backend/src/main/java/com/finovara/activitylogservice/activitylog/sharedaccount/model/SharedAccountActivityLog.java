package com.finovara.activitylogservice.internal.security.sharedaccount.model;

import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shared_account_activity_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedAccountActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private SharedAccountActivityLogType activityType;

    private LocalDateTime createdAt;

    private Long ownerId;
    private Long memberId;
    private Long userId;
}
