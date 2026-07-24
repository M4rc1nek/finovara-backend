package com.finovara.financeservice.sharedaccount.deletion;

import com.finovara.contracts.event.notification.sharedaccount.deletion.SharedAccountDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SharedAccountDeletionHandler {

    private final SharedAccountDeletionFinanceDataService financeDataService;
    private final WalletCacheEvictionService cacheEvictionService;

    public void handle(SharedAccountDeletedEvent event) {
        boolean wasDeleted = financeDataService.deleteData(event);
        if (wasDeleted) {
            cacheEvictionService.evictAfterSharedAccountDeletion(event.ownerId(), event.memberId());
        }
    }
}