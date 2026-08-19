package com.finovara.financeservice.sharedaccount.consumer;

import com.finovara.contracts.finance.event.sharedaccount.SharedAccountCreateDefaultSettingsEvent;
import com.finovara.financeservice.sharedaccount.settings.factory.SharedAccountSettingsFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettingsConsumer {
    private final SharedAccountSettingsFactory sharedAccountSettingsFactory;


    @KafkaListener(topics = "finance.shared-account.create-default-settings")
    public void createDefaultSettings(SharedAccountCreateDefaultSettingsEvent event){
        sharedAccountSettingsFactory.createDefaultSharedAccountSettingsIfNotExist(event.inviterUserId(), event.inviteeUserId());
    }

}
