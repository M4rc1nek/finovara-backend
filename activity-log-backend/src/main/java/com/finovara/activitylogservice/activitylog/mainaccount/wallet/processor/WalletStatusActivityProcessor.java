package com.finovara.activitylogservice.activitylog.mainaccount.wallet.processor;

import com.finovara.activitylogservice.activitylog.mainaccount.wallet.repository.WalletStatusActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class WalletStatusActivityProcessor {

    private final WalletStatusActivityRepository walletStatusActivityRepository;

    @Transactional
    public void deleteWalletStatusActivities() {
        walletStatusActivityRepository.deleteAllInBatch();
        log.info("Wallet status activities have been deleted.");
    }
}
