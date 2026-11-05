package com.finovara.activitylogservice.activitylog.mainaccount.wallet.processor;

import com.finovara.activitylogservice.activitylog.mainaccount.wallet.repository.WalletStatusActivityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WalletStatusActivityProcessorTest {

    @Mock
    private WalletStatusActivityRepository walletStatusActivityRepository;

    @InjectMocks
    private WalletStatusActivityProcessor processor;

    @Test
    void shouldDeleteAllWalletStatusActivitiesInBatch() {
        processor.deleteWalletStatusActivities();

        verify(walletStatusActivityRepository).deleteAllInBatch();
    }
}
