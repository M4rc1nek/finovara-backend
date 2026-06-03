package com.finovara.activityservice.kafka;

import com.finovara.activityservice.activitylog.accountactivity.expense.service.ExpenseActivityService;
import com.finovara.activityservice.activitylog.accountactivity.limit.service.LimitActivityService;
import com.finovara.activityservice.activitylog.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.activityservice.activitylog.accountactivity.revenue.service.RevenueActivityService;
import com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.activity.service.AccountChangesActivityService;
import com.finovara.activityservice.activitylog.accountactivity.secure.login.activity.service.LoginActivityService;
import com.finovara.activityservice.activitylog.accountactivity.settings.service.SettingsActivityService;
import com.finovara.contracts.event.activity.expense.ExpenseActivityEvent;
import com.finovara.contracts.event.activity.limit.LimitActivityEvent;
import com.finovara.contracts.event.activity.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.event.activity.revenue.RevenueActivityEvent;
import com.finovara.contracts.event.activity.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.event.activity.secure.login.activity.LoginActivityEvent;
import com.finovara.contracts.event.activity.settings.SettingsActivityEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivityConsumers {

    private final SettingsActivityService settingsActivityService;
    private final RevenueActivityService revenueActivityService;
    private final PiggyBankActivityService piggyBankActivityService;
    private final LoginActivityService loginActivityService;
    private final LimitActivityService limitActivityService;
    private final ExpenseActivityService expenseActivityService;
    private final AccountChangesActivityService accountChangesActivityService;

    @KafkaListener(topics = "activity.settings")
    public void handleSettings(SettingsActivityEvent event) {
        settingsActivityService.handleEvent(event);
    }

    @KafkaListener(topics = "activity.revenue")
    public void handleRevenue(RevenueActivityEvent event) {
        revenueActivityService.handleEvent(event);
    }

    @KafkaListener(topics = "activity.piggybank")
    public void handlePiggyBank(PiggyBankActivityEvent event) {
        piggyBankActivityService.handleEvent(event);
    }

    @KafkaListener(topics = "activity.login")
    public void handleLogin(LoginActivityEvent event) {
        loginActivityService.handleEvent(event);
    }

    @KafkaListener(topics = "activity.limit")
    public void handleLimit(LimitActivityEvent event) {
        limitActivityService.handleEvent(event);
    }

    @KafkaListener(topics = "activity.expense")
    public void handleExpense(ExpenseActivityEvent event) {
        expenseActivityService.handleEvent(event);
    }

    @KafkaListener(topics = "activity.account-changes")
    public void handleAccountChanges(AccountChangesActivityEvent event) {
        accountChangesActivityService.handleEvent(event);
    }
}