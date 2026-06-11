package com.finovara.activitylogservice.activitylog.accountactivity.settings.mapper;

import com.finovara.activitylogservice.activitylog.accountactivity.settings.dto.SettingsActivityDto;
import com.finovara.activitylogservice.activitylog.accountactivity.settings.model.SettingsActivity;
import org.springframework.stereotype.Component;

@Component
public class SettingsActivityMapper {
    public SettingsActivityDto mapToSettingActivity(SettingsActivity activity) {
        return new SettingsActivityDto(
                activity.getStatus(),
                activity.getSettingType(),
                activity.getCreatedAt()
        );
    }
}
