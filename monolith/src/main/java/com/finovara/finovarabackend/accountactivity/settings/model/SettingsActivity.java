package com.finovara.finovarabackend.accountactivity.settings.model;

import com.finovara.finovarabackend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "settings_activity")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SettingsActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private SettingActivityStatus status;

    @Enumerated(EnumType.STRING)
    private SettingType settingType;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User userAssigned;

}
