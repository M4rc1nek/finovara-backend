package com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.model;

import com.finovara.contracts.model.activity.SharedAccountActivityType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shared_account_activity")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class SharedAccountActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private SharedAccountActivityType type;

    private BigDecimal refundedBalance;
    private String coFounderUsername;
    private String coFounderEmail;

    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Long userId;

}
