package com.finovara.activitylogservice.kafka;

import com.finovara.activitylogservice.activitylog.accountactivity.expense.service.ExpenseActivityService;
import com.finovara.activitylogservice.activitylog.accountactivity.limit.service.LimitActivityService;
import com.finovara.activitylogservice.activitylog.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.activitylogservice.activitylog.accountactivity.revenue.service.RevenueActivityService;
import com.finovara.activitylogservice.activitylog.accountactivity.secure.accountchange.activity.service.AccountChangesActivityService;
import com.finovara.activitylogservice.activitylog.accountactivity.secure.login.activity.service.LoginActivityService;
import com.finovara.activitylogservice.activitylog.accountactivity.settings.service.SettingsActivityService;
import com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.service.SharedAccountActivityService;
import com.finovara.contracts.datadeletable.UserDataDeletable;
import com.finovara.contracts.event.activity.expense.ExpenseActivityEvent;
import com.finovara.contracts.event.activity.limit.LimitActivityEvent;
import com.finovara.contracts.event.activity.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.event.activity.piggybank.PiggyBankEditActivityEvent;
import com.finovara.contracts.event.activity.revenue.RevenueActivityEvent;
import com.finovara.contracts.event.activity.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.event.activity.secure.login.activity.LoginActivityEvent;
import com.finovara.contracts.event.activity.settings.SettingsActivityEvent;
import com.finovara.contracts.event.activity.sharedaccount.SharedAccountActivityEvent;
import com.finovara.contracts.event.user.delete.account.UserAccountDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

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
    private final SharedAccountActivityService sharedAccountActivityService;

    private final List<UserDataDeletable> deletableServices;

    @KafkaListener(topics = "activity.settings")
    public void handleSettings(SettingsActivityEvent event) {
        settingsActivityService.handleEvent(event);
    }

    @KafkaListener(topics = "activity.revenue")
    public void handleRevenue(RevenueActivityEvent event) {
        revenueActivityService.handleEvent(event);
    }

    @KafkaListener(topics = "activity.piggybank.lifecycle")
    public void handlePiggyBank(PiggyBankActivityEvent event) {
        piggyBankActivityService.handleEvent(event);
    }

    @KafkaListener(topics = "activity.piggybank.edited")
    public void handleEditPiggyBank(PiggyBankEditActivityEvent event) {
        piggyBankActivityService.handleEditEvent(event);
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

    @KafkaListener(topics = "activity.shared-account")
    public void handleSharedAccount(SharedAccountActivityEvent event) {
        sharedAccountActivityService.handleEvent(event);
    }

    @KafkaListener(topics = "user-account.deleted")
    public void handleAccountDeleted(UserAccountDeletedEvent event) {
        deletableServices.forEach(service -> service.deleteByUserId(event.userId()));
    }
}