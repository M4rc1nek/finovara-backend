package com.finovara.corebackend.revenue.model;

import com.finovara.activityservice.contracts.model.transaction.RevenueCategory;
import com.finovara.corebackend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "revenues")
public class Revenue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private RevenueCategory category;
    private LocalDate createdAt;
    private String description;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;
}
