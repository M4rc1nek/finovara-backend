package com.finovara.financeservice.sharedaccount.consumer;

import com.finovara.contracts.finance.event.sharedaccount.UsersCreatedSharedAccountEvent;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvitationConsumer {
    private final SharedWalletService sharedWalletService;


    @KafkaListener(topics = "finance.shared-account.invitation-accepted")
    public void createDefault(UsersCreatedSharedAccountEvent event){
        sharedWalletService.createSharedWallet(event.inviterUserId(), event.inviteeUserId());
    }


}
