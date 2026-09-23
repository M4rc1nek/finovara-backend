package com.finovara.activitylogservice.activitylog.accountactivity.settings.processor;

import com.finovara.activitylogservice.activitylog.accountactivity.settings.repository.SettingsActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class SettingsActivityProcessor {

    private final SettingsActivityRepository settingsActivityRepository;

    @Transactional
    public void deleteSettingsActivities() {
        settingsActivityRepository.deleteAllInBatch();
        log.info("Settings Activities has been deleted.");
    }
}
