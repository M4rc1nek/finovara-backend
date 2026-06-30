package com.finovara.authservice.sharedaccount.model;

import com.finovara.authservice.sharedaccount.model.status.SharedAccountStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Table(name = "shared_accounts")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
public class SharedAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private SharedAccountStatus status;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "sharedAccount", cascade = CascadeType.ALL)
    private List<SharedAccountMember> members;
}

