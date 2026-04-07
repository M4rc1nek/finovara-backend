package com.finovara.finovarabackend.usersetting.notificationemail.model;

import com.finovara.finovarabackend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_settings")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class NotificationEmailSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean  notifyOnPasswordChange;
    private boolean  notifyOnUsernameChange;
    private boolean  notifyOnAccountDeleted;


    @OneToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;
}
