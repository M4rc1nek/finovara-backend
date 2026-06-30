package com.finovara.authservice.sharedaccount.model;

import com.finovara.authservice.sharedaccount.model.status.InvitationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shared_account_invitations")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class SharedAccountInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long inviterUserId;

    private Long inviteeUserId;

    @Enumerated(EnumType.STRING)
    private InvitationStatus status;

    private LocalDateTime createdAt;
}