package com.finovara.activitylogservice.activitylog.mainaccount.wallet.scheduler;

import com.finovara.activitylogservice.activitylog.mainaccount.wallet.processor.WalletStatusActivityProcessor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalletStatusActivityScheduler {

    private final WalletStatusActivityProcessor walletStatusActivityProcessor;

    @Scheduled(cron = "${scheduler.user-activity.wallet-status.delete-cron}", zone = "Europe/Warsaw")
    @SchedulerLock(name = "deleteWalletStatusActivities", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public void deleteWalletStatusActivities() {
        walletStatusActivityProcessor.deleteWalletStatusActivities();
    }
}
