package com.finovara.financeservice.sharedaccount.consumer;

import com.finovara.contracts.sharedaccount.event.UsersCreatedSharedAccountEvent;
import com.finovara.contracts.sharedaccount.event.deletion.SharedAccountDeletedEvent;
import com.finovara.contracts.sharedaccount.event.settings.SharedAccountCreateDefaultSettingsEvent;
import com.finovara.financeservice.sharedaccount.deletion.SharedAccountDeletionHandler;
import com.finovara.financeservice.sharedaccount.settings.factory.SharedAccountSettingsFactory;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SharedAccountConsumer {

    private final SharedAccountSettingsFactory sharedAccountSettingsFactory;
    private final SharedWalletService sharedWalletService;
    private final SharedAccountDeletionHandler sharedAccountDeletionHandler;

    @KafkaListener(topics = "finance.shared-account.invitation-accepted")
    public void createDefault(UsersCreatedSharedAccountEvent event){
        sharedWalletService.createSharedWallet(event.inviterUserId(), event.inviteeUserId());
    }

    @KafkaListener(topics = "finance.shared-account.create-default-settings")
    public void createDefaultSettings(SharedAccountCreateDefaultSettingsEvent event){
        sharedAccountSettingsFactory.createDefaultSharedAccountSettingsIfNotExist(event.inviterUserId(), event.inviteeUserId());
    }

    @KafkaListener(topics = "shared-account.deleted")
    public void deleteDataFromSharedAccount(SharedAccountDeletedEvent event) {
        sharedAccountDeletionHandler.handle(event);
    }

}
