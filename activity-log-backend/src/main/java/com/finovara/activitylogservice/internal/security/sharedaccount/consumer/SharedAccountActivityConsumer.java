package com.finovara.activitylogservice.internal.security.sharedaccount.consumer;

import com.finovara.activitylogservice.internal.security.sharedaccount.consumer.factory.SharedAccountActivityFactory;
import com.finovara.activitylogservice.internal.security.sharedaccount.repository.SharedAccountFinanceActivityRepository;
import com.finovara.contracts.finance.event.sharedaccount.finance.SharedAccountExpenseActivityEvent;
import com.finovara.contracts.finance.event.sharedaccount.finance.SharedAccountPiggyBankDepositActivityEvent;
import com.finovara.contracts.finance.event.sharedaccount.finance.SharedAccountRevenueActivityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedAccountActivityConsumer {

    private final SharedAccountActivityFactory sharedAccountActivityFactory;
    private final SharedAccountActivityDataDeleter sharedAccountActivityDataDeleter;

    @KafkaListener(topics = "shared-account.expense.created")
    public void consumeExpenseCreated(SharedAccountExpenseActivityEvent event) {
        sharedAccountActivityFactory.createExpenseCreated(event);
    }

    @KafkaListener(topics = "shared-account.revenue.created")
    public void consumeRevenueCreated(SharedAccountRevenueActivityEvent event) {
        sharedAccountActivityFactory.createRevenueCreated(event);
    }

    @KafkaListener(topics = "shared-account.piggybank.deposit.added")
    public void consumePiggyBankDepositAdded(SharedAccountPiggyBankDepositActivityEvent event) {
        sharedAccountActivityFactory.createPiggyBankDepositAdded(event);
    }

    @KafkaListener(topics = "shared-account.deleted")
    public void deleteDataFromSharedAccount() {
        sharedAccountActivityDataDeleter.deleteAllFinanceActivityData();
    }
}
