package com.finovara.authservice.sharedaccount.model;

import com.finovara.authservice.sharedaccount.model.status.SharedRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "shared_account_members")
public class SharedAccountMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "shared_account_id")
    private SharedAccount sharedAccount;

    private Long userId;

    @Enumerated(EnumType.STRING)
    private SharedRole role;

    private LocalDateTime joinedAt;
}