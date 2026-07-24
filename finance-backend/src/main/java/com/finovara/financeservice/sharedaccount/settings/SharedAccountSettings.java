package com.finovara.financeservice.sharedaccount.settings;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Setter
@Getter
@Table(name = "shared_settings")
public class SharedAccountSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean spendControlEnabled;

    private BigDecimal spendControlPercentage;

    @Column(nullable = false)
    private boolean expenseAnalysisEnabled;

    @Column(nullable = false)
    private boolean largeExpenseNotificationEnabled;

    private BigDecimal largeExpenseNotificationThreshold;

    @Column(nullable = false)
    private boolean piggyBankGoalAchievedNotificationEnabled;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private Long memberId;

}
