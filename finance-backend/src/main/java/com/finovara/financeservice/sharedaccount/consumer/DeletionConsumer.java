package com.finovara.financeservice.sharedaccount.consumer;

import com.finovara.contracts.event.finance.sharedaccount.SharedAccountDeletedEvent;
import com.finovara.financeservice.sharedaccount.deletion.SharedAccountDeletionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeletionConsumer {

    private final SharedAccountDeletionHandler sharedAccountDeletionHandler;

    @KafkaListener(topics = "shared-account.deleted")
    public void deleteDataFromSharedAccount(SharedAccountDeletedEvent event) {
        sharedAccountDeletionHandler.handle(event);
    }
}