package com.finovara.activityservice.kafka;

import com.finovara.activityservice.activity_log.accountactivity.settings.service.SettingsActivityService;
import com.finovara.activityservice.contracts.event.settings.SettingsActivityEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettingsActivityConsumer {

    private final SettingsActivityService settingsActivityService;

    @KafkaListener(topics = "activity.settings", groupId = "activity-service")
    public void handle(SettingsActivityEvent event) {
        settingsActivityService.handleEvent(event);
    }
}
