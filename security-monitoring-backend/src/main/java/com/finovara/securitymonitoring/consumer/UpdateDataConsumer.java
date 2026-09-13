package com.finovara.securitymonitoring.consumer;

import com.finovara.contracts.activity.event.expense.ExpenseActivityEvent;
import com.finovara.contracts.activity.event.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.activity.event.revenue.RevenueActivityEvent;
import com.finovara.contracts.activity.event.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.activity.event.secure.login.activity.LoginActivityEvent;
import com.finovara.securitymonitoring.accountchange.service.AccountChangeProfileUpdateService;
import com.finovara.securitymonitoring.login.service.LoginProfileUpdateService;
import com.finovara.securitymonitoring.transaction.service.TransactionProfileUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateDataConsumer {

    private final LoginProfileUpdateService loginProfileUpdateService;
    private final AccountChangeProfileUpdateService accountChangeProfileUpdateService;
    private final TransactionProfileUpdateService transactionProfileUpdateService;

    @KafkaListener(topics = "user.logged-in")
    public void handleLogin(LoginActivityEvent event) {
        loginProfileUpdateService.handleLoginEvent(event);
    }

    @KafkaListener(topics = "expense.created")
    public void handleExpense(ExpenseActivityEvent event) {
        transactionProfileUpdateService.handleExpenseEvent(event);
    }

    @KafkaListener(topics = "revenue.created")
    public void handleRevenue(RevenueActivityEvent event) {
        transactionProfileUpdateService.handleRevenueEvent(event);
    }

    @KafkaListener(topics = "account.changed")
    public void handleAccountChanges(AccountChangesActivityEvent event) {
        accountChangeProfileUpdateService.handleAccountChangeEvent(event);
    }

    @KafkaListener(topics = "piggybank.transaction.created")
    public void handlePiggyBankTransaction(PiggyBankActivityEvent event) {
        transactionProfileUpdateService.handlePiggyBankEvent(event);
    }
}
