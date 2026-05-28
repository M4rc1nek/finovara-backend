package com.finovara.activityservice.kafka;

import com.finovara.activityservice.activity_log.accountactivity.expense.service.ExpenseActivityService;
import com.finovara.activityservice.activity_log.accountactivity.limit.service.LimitActivityService;
import com.finovara.activityservice.activity_log.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.activityservice.activity_log.accountactivity.revenue.service.RevenueActivityService;
import com.finovara.activityservice.activity_log.accountactivity.secure.accountchange.activity.service.AccountChangesActivityService;
import com.finovara.activityservice.activity_log.accountactivity.secure.login.activity.service.LoginActivityService;
import com.finovara.activityservice.activity_log.accountactivity.settings.service.SettingsActivityService;
import com.finovara.contracts.event.expense.ExpenseActivityEvent;
import com.finovara.contracts.event.limit.LimitActivityEvent;
import com.finovara.contracts.event.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.event.revenue.RevenueActivityEvent;
import com.finovara.contracts.event.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.event.secure.login.activity.LoginActivityEvent;
import com.finovara.contracts.event.settings.SettingsActivityEvent;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.activity.AccountChangesActivityType;
import com.finovara.contracts.model.activity.ExpenseActivityType;
import com.finovara.contracts.model.activity.LimitActivityType;
import com.finovara.contracts.model.activity.LoginActivityStatus;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.contracts.model.activity.RevenueActivityType;
import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.model.activity.SettingType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.PiggyBankGoalType;
import com.finovara.contracts.model.transaction.RevenueCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ActivityConsumersTest {

    private static final Long USER_ID = 1L;
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 5, 25, 17, 0);

    @Test
    void expenseConsumerShouldDelegateEventToService() {
        ExpenseActivityService service = mock(ExpenseActivityService.class);
        ExpenseActivityEvent event = new ExpenseActivityEvent(
                USER_ID,
                ExpenseActivityType.ADDED_EXPENSE,
                new BigDecimal("10.00"),
                ExpenseCategory.FOOD,
                null,
                null,
                OCCURRED_AT
        );

        new ExpenseActivityConsumer(service).handle(event);

        verify(service).handleEvent(event);
    }

    @Test
    void revenueConsumerShouldDelegateEventToService() {
        RevenueActivityService service = mock(RevenueActivityService.class);
        RevenueActivityEvent event = new RevenueActivityEvent(
                USER_ID,
                RevenueActivityType.ADDED_REVENUE,
                new BigDecimal("10.00"),
                RevenueCategory.SALARY,
                null,
                null,
                OCCURRED_AT
        );

        new RevenueActivityConsumer(service).handle(event);

        verify(service).handleEvent(event);
    }

    @Test
    void limitConsumerShouldDelegateEventToService() {
        LimitActivityService service = mock(LimitActivityService.class);
        LimitActivityEvent event = new LimitActivityEvent(
                USER_ID,
                LimitActivityType.ADDED_LIMIT,
                PeriodType.DAILY.name(),
                new BigDecimal("10.00"),
                null,
                OCCURRED_AT
        );

        new LimitActivityConsumer(service).handle(event);

        verify(service).handleEvent(event);
    }

    @Test
    void piggyBankConsumerShouldDelegateEventToService() {
        PiggyBankActivityService service = mock(PiggyBankActivityService.class);
        PiggyBankActivityEvent event = new PiggyBankActivityEvent(
                USER_ID,
                PiggyBankActivityType.ADDED_PIGGY_BANK,
                "Gift fund",
                PiggyBankGoalType.GIFTS,
                new BigDecimal("100.00"),
                null,
                OCCURRED_AT
        );

        new PiggyBankActivityConsumer(service).handle(event);

        verify(service).handleEvent(event);
    }

    @Test
    void settingsConsumerShouldDelegateEventToService() {
        SettingsActivityService service = mock(SettingsActivityService.class);
        SettingsActivityEvent event = new SettingsActivityEvent(
                USER_ID,
                SettingType.PIGGY_BANK_ROUND_UP,
                SettingActivityStatus.ENABLED,
                OCCURRED_AT
        );

        new SettingsActivityConsumer(service).handle(event);

        verify(service).handleEvent(event);
    }

    @Test
    void loginConsumerShouldDelegateEventToService() {
        LoginActivityService service = mock(LoginActivityService.class);
        LoginActivityEvent event = new LoginActivityEvent(
                USER_ID,
                LoginActivityStatus.SUCCESSFUL,
                "Firefox",
                "127.0.0.1",
                "Localhost",
                OCCURRED_AT
        );

        new LoginActivityConsumer(service).handle(event);

        verify(service).handleEvent(event);
    }

    @Test
    void accountChangesConsumerShouldDelegateEventToService() {
        AccountChangesActivityService service = mock(AccountChangesActivityService.class);
        AccountChangesActivityEvent event = new AccountChangesActivityEvent(
                USER_ID,
                AccountChangesActivityType.PASSWORD_CHANGED,
                "Firefox",
                "127.0.0.1",
                "Localhost",
                OCCURRED_AT
        );

        new AccountChangesActivityConsumer(service).handle(event);

        verify(service).handleEvent(event);
    }
}
