package com.finovara.activitylogservice.activitylog.mainaccountactivity.settings.mapper;

import com.finovara.activitylogservice.activitylog.mainaccountactivity.settings.dto.SettingsActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccountactivity.settings.model.SettingsActivity;
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
