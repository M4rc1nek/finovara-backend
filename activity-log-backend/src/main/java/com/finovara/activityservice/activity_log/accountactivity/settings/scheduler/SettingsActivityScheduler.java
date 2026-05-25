package com.finovara.activityservice.activity_log.accountactivity.settings.scheduler;

import com.finovara.activityservice.activity_log.accountactivity.settings.processor.SettingsActivityProcessor;
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
