package com.finovara.securitymonitoring.consumer;

import com.finovara.contracts.user.event.UserCreatedEvent;
import com.finovara.contracts.user.event.account.delete.UserAccountDeletedEvent;
import com.finovara.securitymonitoring.accountchange.factory.AccountChangeProfileFactory;
import com.finovara.securitymonitoring.accountchange.repository.AccountChangeProfileRepository;
import com.finovara.securitymonitoring.accountchange.service.AccountChangeProfileUpdateService;
import com.finovara.securitymonitoring.clientdata.factory.ClientDataFactory;
import com.finovara.securitymonitoring.clientdata.repository.ClientDataRepository;
import com.finovara.securitymonitoring.login.service.LoginProfileUpdateService;
import com.finovara.securitymonitoring.transaction.factory.TransactionProfileFactory;
import com.finovara.securitymonitoring.transaction.repository.TransactionProfileRepository;
import com.finovara.securitymonitoring.transaction.service.TransactionProfileUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityMonitoringConsumer {

    private final ClientDataFactory clientDataFactory;
    private final TransactionProfileFactory transactionProfileFactory;
    private final AccountChangeProfileFactory accountChangeProfileFactory;
    private final TransactionProfileUpdateService transactionProfileUpdateService;
    private final AccountChangeProfileUpdateService  accountChangeProfileUpdateService;
    private final LoginProfileUpdateService loginProfileUpdateService;

    @KafkaListener(topics = "user.created")
    public void createDefaultProfiles(UserCreatedEvent event) {
        clientDataFactory.createDefaultDataIfNotExist(event.userId());
        transactionProfileFactory.createDefaultDataIfNotExist(event.userId());
        accountChangeProfileFactory.createDefaultDataIfNotExist(event.userId());
    }

    @KafkaListener(topics = "user-account.deleted")
    public void deleteProfilesData(UserAccountDeletedEvent event) {
        loginProfileUpdateService.deleteByUserId(event.userId());
        accountChangeProfileUpdateService.deleteByUserId(event.userId());
        transactionProfileUpdateService.deleteByUserId(event.userId());
    }


}