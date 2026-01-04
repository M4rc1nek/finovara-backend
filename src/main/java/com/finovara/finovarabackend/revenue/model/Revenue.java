package com.finovara.finovarabackend.revenue.model;

import com.finovara.finovarabackend.user.model.User;
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
    private RevenueCategory category;
    private LocalDate createdAt;
    private String description;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;
}
