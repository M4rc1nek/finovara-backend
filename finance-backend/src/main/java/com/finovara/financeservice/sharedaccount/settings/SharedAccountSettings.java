package com.finovara.financeservice.sharedaccount.settings.model;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Setter
@Getter
@Table(name = "shared_settings")
public class SharedAccountSettings{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean walletSpendLimitEnabled;

    private int walletSpendLimitPercentage;


    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private Long memberId;

}
