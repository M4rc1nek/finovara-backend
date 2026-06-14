package com.finovara.authbackend.usersetting.account.model;

import com.finovara.authbackend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    private Integer resetPasswordCode;
    private LocalDateTime resetPasswordCodeExpiresAt;
    private int  passwordResetAttempts;
    private LocalDateTime attemptsPasswordExpiresAt;

    private Integer emailChangeCode;
    private LocalDateTime emailChangeCodeExpiresAt;
    private String pendingEmail;
    private int  emailChangeAttempts;
    private LocalDateTime attemptsEmailExpiresAt;


    @OneToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;
}
