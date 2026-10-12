package com.finovara.financeservice.consumer;

import com.finovara.contracts.user.datadeletable.UserDataDeletable;
import com.finovara.contracts.user.event.account.UserAccountDeletedEvent;
import com.finovara.contracts.user.event.account.UserCreatedEvent;
import com.finovara.financeservice.history.cache.evict.EvictFinanceDataHistoryService;
import com.finovara.financeservice.settings.factory.FinanceSettingsFactory;
import com.finovara.financeservice.wallet.reservation.service.FundReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MainAccountFinanceConsumer {

    private final List<UserDataDeletable> deletableServices;
    private final FinanceSettingsFactory financeSettingsFactory;
    private final FundReservationService fundReservationService;
    private final EvictFinanceDataHistoryService evictFinanceDataHistoryService;

    @KafkaListener(topics = "user.created", groupId = "finance.create-default-settings")
    public void handleUserCreated(UserCreatedEvent event) {
        financeSettingsFactory.createDefaultSettingsIfNotExist(event.userId());
    }

    @KafkaListener(topics = "user-account.deleted")
    public void handleAccountDeleted(UserAccountDeletedEvent event) {
        deletableServices.forEach(service -> service.deleteByUserId(event.userId()));
        financeSettingsFactory.deleteSettingsIfExist(event.userId());
        evictFinanceDataHistoryService.evictFinanceDataHistory(event.userId());
    }
}
