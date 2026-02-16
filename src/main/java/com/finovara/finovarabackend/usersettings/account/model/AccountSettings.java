package com.finovara.finovarabackend.usersettings.account.model;

import com.finovara.finovarabackend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "account_settings")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class AccountSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer forgotPasswordCode;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;
}
