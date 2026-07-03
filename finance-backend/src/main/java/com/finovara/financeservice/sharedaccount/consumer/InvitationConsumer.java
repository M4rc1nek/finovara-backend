package com.finovara.financeservice.sharedaccount.consumer;

import com.finovara.contracts.event.finance.sharedaccount.UsersCreatedSharedAccountEvent;
import com.finovara.financeservice.sharedaccount.service.wallet.SharedWalletService;
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
