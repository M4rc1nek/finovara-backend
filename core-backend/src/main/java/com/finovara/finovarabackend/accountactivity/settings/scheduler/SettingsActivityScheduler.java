package com.finovara.finovarabackend.accountactivity.settings.scheduler;

import com.finovara.finovarabackend.accountactivity.settings.processor.SettingsActivityProcessor;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettingsActivityScheduler {

    private final SettingsActivityProcessor settingsActivityProcessor;

    @Scheduled(cron = "${scheduler.user-activity.settings.delete-cron}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "deleteSettingsActivities", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void deleteSettingsActivities(){
        settingsActivityProcessor.deleteSettingsActivities();
    }

}
