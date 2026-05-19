package com.finovara.finovarabackend.accountactivity.settings.processor;

import com.finovara.finovarabackend.accountactivity.settings.repository.SettingsActivityRepository;
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
