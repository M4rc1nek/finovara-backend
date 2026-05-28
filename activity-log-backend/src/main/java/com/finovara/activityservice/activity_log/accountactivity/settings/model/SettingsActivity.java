package com.finovara.activityservice.activity_log.accountactivity.settings.model;

import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.model.activity.SettingType;
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

    @Column(name = "user_id", nullable = false)
    private Long userId;

}
