package com.finovara.finovarabackend.accountactivity.piggybank.scheduler;

import com.finovara.finovarabackend.accountactivity.piggybank.processor.PiggyBankActivityProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PiggyBankActivityScheduler {

    private final PiggyBankActivityProcessor piggyBankActivityProcessor;

    @Scheduled(cron = "${scheduler.user-activity.piggy-bank.delete-cron}", zone = "Europe/Warsaw")
    public void deletePiggyBankActivities() {
        piggyBankActivityProcessor.deletePiggyBankActivities();
    }

}
