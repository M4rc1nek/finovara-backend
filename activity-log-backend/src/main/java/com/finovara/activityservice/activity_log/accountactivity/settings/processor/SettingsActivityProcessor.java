package com.finovara.activityservice.activity_log.accountactivity.settings.processor;

import com.finovara.activityservice.activity_log.accountactivity.settings.repository.SettingsActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettingsActivityProcessor {

    private final SettingsActivityRepository settingsActivityRepository;

    public void deleteSettingsActivities() {
        settingsActivityRepository.deleteAllInBatch();
    }
}
