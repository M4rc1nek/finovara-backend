package com.finovara.activitylogservice.activitylog.consumer;

import com.finovara.activitylogservice.activitylog.mainaccount.expense.service.ExpenseActivityService;
import com.finovara.activitylogservice.activitylog.mainaccount.limit.service.LimitActivityService;
import com.finovara.activitylogservice.activitylog.mainaccount.piggybank.service.PiggyBankActivityService;
import com.finovara.activitylogservice.activitylog.mainaccount.revenue.service.RevenueActivityService;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.service.AccountChangeActivityService;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.service.LoginActivityService;
import com.finovara.activitylogservice.activitylog.mainaccount.settings.service.SettingsActivityService;
import com.finovara.activitylogservice.activitylog.mainaccount.sharedaccount.service.SharedAccountChangeHistoryActivityService;
import com.finovara.activitylogservice.activitylog.mainaccount.wallet.service.WalletStatusActivityService;
import com.finovara.activitylogservice.activitylog.util.UserSessionResolver;
import com.finovara.activitylogservice.cache.evict.ActivityCache;
import com.finovara.activitylogservice.cache.evict.EvictActivityCacheService;
import com.finovara.contracts.mainaccount.activity.event.expense.ExpenseActivityEvent;
import com.finovara.contracts.mainaccount.activity.event.limit.LimitActivityEvent;
import com.finovara.contracts.mainaccount.activity.event.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.mainaccount.activity.event.piggybank.PiggyBankEditActivityEvent;
import com.finovara.contracts.mainaccount.activity.event.revenue.RevenueActivityEvent;
import com.finovara.contracts.mainaccount.activity.event.secure.accountchange.activity.AccountChangeActivityEvent;
import com.finovara.contracts.mainaccount.activity.event.secure.login.activity.LoginActivityEvent;
import com.finovara.contracts.mainaccount.activity.event.settings.SettingsActivityEvent;
import com.finovara.contracts.mainaccount.activity.event.sharedaccount.SharedAccountChangeHistoryActivityEvent;
import com.finovara.contracts.mainaccount.activity.event.wallet.WalletStatusActivityEvent;
import com.finovara.contracts.user.datadeletable.UserDataDeletable;
import com.finovara.contracts.user.event.account.UserAccountDeletedEvent;
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
    private final AccountChangeActivityService accountChangeActivityService;
    private final SharedAccountChangeHistoryActivityService sharedAccountChangeHistoryActivityService;
    private final WalletStatusActivityService walletStatusActivityService;
    private final EvictActivityCacheService evictActivityCacheService;
    private final UserSessionResolver userSessionResolver;
    private final List<UserDataDeletable> deletableServices;

    @KafkaListener(topics = "settings.changed")
    public void handleSettings(SettingsActivityEvent event) {
        settingsActivityService.handleEvent(event);
    }

    @KafkaListener(topics = {"revenue.created", "trash.revenue.activity"})
    public void handleRevenue(RevenueActivityEvent event) {
        revenueActivityService.handleEvent(event);
        evictActivityCacheService.evict(ActivityCache.REVENUE, event.userId());
    }

    @KafkaListener(topics = {"piggybank.lifecycle.changed", "trash.piggybank.activity"})
    public void handlePiggyBank(PiggyBankActivityEvent event) {
        piggyBankActivityService.handleEvent(event);
        evictActivityCacheService.evict(ActivityCache.PIGGY_BANK, event.userId());
    }

    @KafkaListener(topics = "piggybank.updated")
    public void handleEditPiggyBank(PiggyBankEditActivityEvent event) {
        piggyBankActivityService.handleEditEvent(event);
        evictActivityCacheService.evict(ActivityCache.PIGGY_BANK, event.userId());
    }

    @KafkaListener(topics = "piggybank.transaction.created")
    public void handlePiggyBankTransaction(PiggyBankActivityEvent event) {
        piggyBankActivityService.handleEvent(event);
        evictActivityCacheService.evict(ActivityCache.PIGGY_BANK, event.userId());
    }

    @KafkaListener(topics = "user.logged-in")
    public void handleLogin(LoginActivityEvent event) {
        loginActivityService.handleEvent(event);
    }

    @KafkaListener(topics = {"limit.changed", "trash.limit.activity"})
    public void handleLimit(LimitActivityEvent event) {
        limitActivityService.handleEvent(event);
        evictActivityCacheService.evict(ActivityCache.LIMIT, event.userId());
    }

    @KafkaListener(topics = {"expense.created", "trash.expense.activity"})
    public void handleExpense(ExpenseActivityEvent event) {
        expenseActivityService.handleEvent(event);
        evictActivityCacheService.evict(ActivityCache.EXPENSE, event.userId());
    }

    @KafkaListener(topics = {"wallet-status.changed", "trash.reservation.activity"})
    public void handleWalletStatus(WalletStatusActivityEvent event) {
        walletStatusActivityService.handleEvent(event);
        evictActivityCacheService.evict(ActivityCache.WALLET_STATUS, event.userId());
    }

    @KafkaListener(topics = "account.changed")
    public void handleAccountChanges(AccountChangeActivityEvent event) {
        accountChangeActivityService.handleEvent(event);
        evictActivityCacheService.evict(ActivityCache.SHARED_ACCOUNT_ACTIVITY_LOG, event.userId());
        userSessionResolver.evictUserInfo(event.userId());
    }

    @KafkaListener(topics = "shared-account.changed")
    public void handleSharedAccount(SharedAccountChangeHistoryActivityEvent event) {
        sharedAccountChangeHistoryActivityService.handleEvent(event);
    }

    @KafkaListener(topics = "user-account.deleted")
    public void handleAccountDeleted(UserAccountDeletedEvent event) {
        deletableServices.forEach(service -> service.deleteByUserId(event.userId()));
        evictActivityCacheService.evictAllForUser(event.userId());
    }
}