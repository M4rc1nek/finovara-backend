package com.finovara.activitylogservice.activitylog.mainaccount.wallet.scheduler;

import com.finovara.activitylogservice.activitylog.mainaccount.wallet.processor.WalletStatusActivityProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WalletStatusActivitySchedulerTest {

    @Mock
    private WalletStatusActivityProcessor walletStatusActivityProcessor;

    @InjectMocks
    private WalletStatusActivityScheduler scheduler;

    @Test
    void shouldDelegateDeletionToProcessor() {
        scheduler.deleteWalletStatusActivities();

        verify(walletStatusActivityProcessor).deleteWalletStatusActivities();
    }
}
