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
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.activity.*;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.PiggyBankGoalType;
import com.finovara.contracts.model.transaction.RevenueCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ActivityConsumersTest {

    private static final Long USER_ID = 1L;
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 5, 25, 17, 0);

    @Mock
    private SettingsActivityService settingsActivityService;

    @Mock
    private RevenueActivityService revenueActivityService;

    @Mock
    private PiggyBankActivityService piggyBankActivityService;

    @Mock
    private LoginActivityService loginActivityService;

    @Mock
    private LimitActivityService limitActivityService;

    @Mock
    private ExpenseActivityService expenseActivityService;

    @Mock
    private AccountChangesActivityService accountChangesActivityService;

    @InjectMocks
    private ActivityConsumers consumer;

    @Test
    void shouldDelegateExpenseEventToService() {
        ExpenseActivityEvent event = new ExpenseActivityEvent(USER_ID, ExpenseActivityType.ADDED_EXPENSE, new BigDecimal("10.00"), ExpenseCategory.FOOD, null, null, OCCURRED_AT);

        consumer.handleExpense(event);

        verify(expenseActivityService).handleEvent(event);
    }

    @Test
    void shouldDelegateRevenueEventToService() {
        RevenueActivityEvent event = new RevenueActivityEvent(USER_ID, RevenueActivityType.ADDED_REVENUE, new BigDecimal("10.00"), RevenueCategory.SALARY, null, null, OCCURRED_AT);

        consumer.handleRevenue(event);

        verify(revenueActivityService).handleEvent(event);
    }

    @Test
    void shouldDelegateLimitEventToService() {
        LimitActivityEvent event = new LimitActivityEvent(USER_ID, LimitActivityType.ADDED_LIMIT, PeriodType.DAILY.name(), new BigDecimal("10.00"), null, OCCURRED_AT);

        consumer.handleLimit(event);

        verify(limitActivityService).handleEvent(event);
    }

    @Test
    void shouldDelegatePiggyBankEventToService() {
        PiggyBankActivityEvent event = new PiggyBankActivityEvent(USER_ID, PiggyBankActivityType.ADDED_PIGGY_BANK, "Gift fund", PiggyBankGoalType.GIFTS, new BigDecimal("100.00"), null, OCCURRED_AT);

        consumer.handlePiggyBank(event);

        verify(piggyBankActivityService).handleEvent(event);
    }

    @Test
    void shouldDelegateSettingsEventToService() {
        SettingsActivityEvent event = new SettingsActivityEvent(USER_ID, SettingType.PIGGY_BANK_ROUND_UP, SettingActivityStatus.ENABLED, OCCURRED_AT);

        consumer.handleSettings(event);

        verify(settingsActivityService).handleEvent(event);
    }

    @Test
    void shouldDelegateLoginEventToService() {
        LoginActivityEvent event = new LoginActivityEvent(USER_ID, LoginActivityStatus.SUCCESSFUL, "Firefox", "127.0.0.1", "Localhost", OCCURRED_AT);

        consumer.handleLogin(event);

        verify(loginActivityService).handleEvent(event);
    }

    @Test
    void shouldDelegateAccountChangesEventToService() {
        AccountChangesActivityEvent event = new AccountChangesActivityEvent(USER_ID, AccountChangesActivityType.PASSWORD_CHANGED, "Firefox", "127.0.0.1", "Localhost", OCCURRED_AT);

        consumer.handleAccountChanges(event);

        verify(accountChangesActivityService).handleEvent(event);
    }
}