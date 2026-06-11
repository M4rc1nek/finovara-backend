package com.finovara.activityservice.activitylog.accountactivity.settings.mapper;

import com.finovara.activityservice.activitylog.accountactivity.settings.dto.SettingsActivityDto;
import com.finovara.activityservice.activitylog.accountactivity.settings.model.SettingsActivity;
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
