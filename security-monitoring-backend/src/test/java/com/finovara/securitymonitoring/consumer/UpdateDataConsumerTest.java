package com.finovara.securitymonitoring.consumer;

import com.finovara.contracts.activity.event.expense.ExpenseActivityEvent;
import com.finovara.contracts.activity.event.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.activity.event.revenue.RevenueActivityEvent;
import com.finovara.contracts.activity.event.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.activity.event.secure.login.activity.LoginActivityEvent;
import com.finovara.securitymonitoring.accountchange.service.AccountChangeProfileUpdateService;
import com.finovara.securitymonitoring.login.service.LoginProfileUpdateService;
import com.finovara.securitymonitoring.transaction.service.TransactionProfileUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateDataConsumerTest {

    @Mock
    private LoginProfileUpdateService loginProfileUpdateService;

    @Mock
    private AccountChangeProfileUpdateService accountChangeProfileUpdateService;

    @Mock
    private TransactionProfileUpdateService transactionProfileUpdateService;

    @InjectMocks
    private UpdateDataConsumer updateDataConsumer;

    @Nested
    class HandleLogin {

        private LoginActivityEvent event;

        @BeforeEach
        void setUp() {
            event = new LoginActivityEvent(123L, null, "Chrome", "192.168.1.1", "Warsaw", LocalDateTime.of(2026, 1, 1, 12, 0));
        }

        @Test
        void shouldHandleLoginEventWhenEventIsValid() {
            updateDataConsumer.handleLogin(event);

            verify(loginProfileUpdateService).handleLoginEvent(event);
            verifyNoMoreInteractions(loginProfileUpdateService);
            verifyNoInteractions(accountChangeProfileUpdateService, transactionProfileUpdateService);
        }

        @Test
        void shouldHandleLoginEventWhenEventIsNull() {
            updateDataConsumer.handleLogin(null);

            verify(loginProfileUpdateService).handleLoginEvent(null);
            verifyNoMoreInteractions(loginProfileUpdateService);
            verifyNoInteractions(accountChangeProfileUpdateService, transactionProfileUpdateService);
        }

        @Test
        void shouldThrowExceptionWhenLoginServiceThrowsException() {
            doThrow(new IllegalStateException()).when(loginProfileUpdateService).handleLoginEvent(event);

            assertThrows(IllegalStateException.class, () -> updateDataConsumer.handleLogin(event));

            verify(loginProfileUpdateService).handleLoginEvent(event);
            verifyNoInteractions(accountChangeProfileUpdateService, transactionProfileUpdateService);
        }
    }

    @Nested
    class HandleExpense {

        private ExpenseActivityEvent event;

        @BeforeEach
        void setUp() {
            event = new ExpenseActivityEvent(123L, null, new BigDecimal("100.50"), null, new BigDecimal("75.25"), null, LocalDateTime.of(2026, 1, 1, 12, 0));
        }

        @Test
        void shouldHandleExpenseEventWhenEventIsValid() {
            updateDataConsumer.handleExpense(event);

            verify(transactionProfileUpdateService).handleExpenseEvent(event);
            verifyNoMoreInteractions(transactionProfileUpdateService);
            verifyNoInteractions(loginProfileUpdateService, accountChangeProfileUpdateService);
        }

        @Test
        void shouldHandleExpenseEventWhenEventIsNull() {
            updateDataConsumer.handleExpense(null);

            verify(transactionProfileUpdateService).handleExpenseEvent(null);
            verifyNoMoreInteractions(transactionProfileUpdateService);
            verifyNoInteractions(loginProfileUpdateService, accountChangeProfileUpdateService);
        }

        @Test
        void shouldHandleExpenseEventWhenAmountsAreZero() {
            ExpenseActivityEvent zeroAmountEvent = new ExpenseActivityEvent(0L, null, BigDecimal.ZERO, null, BigDecimal.ZERO, null, LocalDateTime.MIN);

            updateDataConsumer.handleExpense(zeroAmountEvent);

            verify(transactionProfileUpdateService).handleExpenseEvent(zeroAmountEvent);
            verifyNoInteractions(loginProfileUpdateService, accountChangeProfileUpdateService);
        }

        @Test
        void shouldHandleExpenseEventWhenOptionalValuesAreNull() {
            ExpenseActivityEvent eventWithNullValues = new ExpenseActivityEvent(null, null, null, null, null, null, null);

            updateDataConsumer.handleExpense(eventWithNullValues);

            verify(transactionProfileUpdateService).handleExpenseEvent(eventWithNullValues);
            verifyNoInteractions(loginProfileUpdateService, accountChangeProfileUpdateService);
        }

        @Test
        void shouldThrowExceptionWhenExpenseServiceThrowsException() {
            doThrow(new IllegalStateException()).when(transactionProfileUpdateService).handleExpenseEvent(event);

            assertThrows(IllegalStateException.class, () -> updateDataConsumer.handleExpense(event));

            verify(transactionProfileUpdateService).handleExpenseEvent(event);
            verifyNoInteractions(loginProfileUpdateService, accountChangeProfileUpdateService);
        }
    }

    @Nested
    class HandleRevenue {

        private RevenueActivityEvent event;

        @BeforeEach
        void setUp() {
            event = new RevenueActivityEvent(123L, null, new BigDecimal("1000.50"), null, new BigDecimal("800.25"), null, LocalDateTime.of(2026, 1, 1, 12, 0));
        }

        @Test
        void shouldHandleRevenueEventWhenEventIsValid() {
            updateDataConsumer.handleRevenue(event);

            verify(transactionProfileUpdateService).handleRevenueEvent(event);
            verifyNoMoreInteractions(transactionProfileUpdateService);
            verifyNoInteractions(loginProfileUpdateService, accountChangeProfileUpdateService);
        }

        @Test
        void shouldHandleRevenueEventWhenEventIsNull() {
            updateDataConsumer.handleRevenue(null);

            verify(transactionProfileUpdateService).handleRevenueEvent(null);
            verifyNoMoreInteractions(transactionProfileUpdateService);
            verifyNoInteractions(loginProfileUpdateService, accountChangeProfileUpdateService);
        }

        @Test
        void shouldHandleRevenueEventWhenAmountsAreZero() {
            RevenueActivityEvent zeroAmountEvent = new RevenueActivityEvent(0L, null, BigDecimal.ZERO, null, BigDecimal.ZERO, null, LocalDateTime.MIN);

            updateDataConsumer.handleRevenue(zeroAmountEvent);

            verify(transactionProfileUpdateService).handleRevenueEvent(zeroAmountEvent);
            verifyNoInteractions(loginProfileUpdateService, accountChangeProfileUpdateService);
        }

        @Test
        void shouldHandleRevenueEventWhenOptionalValuesAreNull() {
            RevenueActivityEvent eventWithNullValues = new RevenueActivityEvent(null, null, null, null, null, null, null);

            updateDataConsumer.handleRevenue(eventWithNullValues);

            verify(transactionProfileUpdateService).handleRevenueEvent(eventWithNullValues);
            verifyNoInteractions(loginProfileUpdateService, accountChangeProfileUpdateService);
        }

        @Test
        void shouldThrowExceptionWhenRevenueServiceThrowsException() {
            doThrow(new IllegalStateException()).when(transactionProfileUpdateService).handleRevenueEvent(event);

            assertThrows(IllegalStateException.class, () -> updateDataConsumer.handleRevenue(event));

            verify(transactionProfileUpdateService).handleRevenueEvent(event);
            verifyNoInteractions(loginProfileUpdateService, accountChangeProfileUpdateService);
        }
    }

    @Nested
    class HandleAccountChanges {

        private AccountChangesActivityEvent event;

        @BeforeEach
        void setUp() {
            event = new AccountChangesActivityEvent(123L, null, "Chrome", "192.168.1.1", "Warsaw", LocalDateTime.of(2026, 1, 1, 12, 0));
        }

        @Test
        void shouldHandleAccountChangeEventWhenEventIsValid() {
            updateDataConsumer.handleAccountChanges(event);

            verify(accountChangeProfileUpdateService).handleAccountChangeEvent(event);
            verifyNoMoreInteractions(accountChangeProfileUpdateService);
            verifyNoInteractions(loginProfileUpdateService, transactionProfileUpdateService);
        }

        @Test
        void shouldHandleAccountChangeEventWhenEventIsNull() {
            updateDataConsumer.handleAccountChanges(null);

            verify(accountChangeProfileUpdateService).handleAccountChangeEvent(null);
            verifyNoMoreInteractions(accountChangeProfileUpdateService);
            verifyNoInteractions(loginProfileUpdateService, transactionProfileUpdateService);
        }

        @Test
        void shouldHandleAccountChangeEventWhenOptionalValuesAreNull() {
            AccountChangesActivityEvent eventWithNullValues = new AccountChangesActivityEvent(null, null, null, null, null, null);

            updateDataConsumer.handleAccountChanges(eventWithNullValues);

            verify(accountChangeProfileUpdateService).handleAccountChangeEvent(eventWithNullValues);
            verifyNoInteractions(loginProfileUpdateService, transactionProfileUpdateService);
        }

        @Test
        void shouldThrowExceptionWhenAccountChangeServiceThrowsException() {
            doThrow(new IllegalStateException()).when(accountChangeProfileUpdateService).handleAccountChangeEvent(event);

            assertThrows(IllegalStateException.class, () -> updateDataConsumer.handleAccountChanges(event));

            verify(accountChangeProfileUpdateService).handleAccountChangeEvent(event);
            verifyNoInteractions(loginProfileUpdateService, transactionProfileUpdateService);
        }
    }

    @Nested
    class HandlePiggyBankTransaction {

        private PiggyBankActivityEvent event;

        @BeforeEach
        void setUp() {
            event = new PiggyBankActivityEvent(123L, null, "Holiday", null, new BigDecimal("5000.00"), new BigDecimal("250.00"), LocalDateTime.of(2026, 1, 1, 12, 0));
        }

        @Test
        void shouldHandlePiggyBankEventWhenEventIsValid() {
            updateDataConsumer.handlePiggyBankTransaction(event);

            verify(transactionProfileUpdateService).handlePiggyBankEvent(event);
            verifyNoMoreInteractions(transactionProfileUpdateService);
            verifyNoInteractions(loginProfileUpdateService, accountChangeProfileUpdateService);
        }

        @Test
        void shouldHandlePiggyBankEventWhenEventIsNull() {
            updateDataConsumer.handlePiggyBankTransaction(null);

            verify(transactionProfileUpdateService).handlePiggyBankEvent(null);
            verifyNoMoreInteractions(transactionProfileUpdateService);
            verifyNoInteractions(loginProfileUpdateService, accountChangeProfileUpdateService);
        }

        @Test
        void shouldHandlePiggyBankEventWhenAmountsAreZero() {
            PiggyBankActivityEvent zeroAmountEvent = new PiggyBankActivityEvent(0L, null, "", null, BigDecimal.ZERO, BigDecimal.ZERO, LocalDateTime.MIN);

            updateDataConsumer.handlePiggyBankTransaction(zeroAmountEvent);

            verify(transactionProfileUpdateService).handlePiggyBankEvent(zeroAmountEvent);
            verifyNoInteractions(loginProfileUpdateService, accountChangeProfileUpdateService);
        }

        @Test
        void shouldHandlePiggyBankEventWhenOptionalValuesAreNull() {
            PiggyBankActivityEvent eventWithNullValues = new PiggyBankActivityEvent(null, null, null, null, null, null, null);

            updateDataConsumer.handlePiggyBankTransaction(eventWithNullValues);

            verify(transactionProfileUpdateService).handlePiggyBankEvent(eventWithNullValues);
            verifyNoInteractions(loginProfileUpdateService, accountChangeProfileUpdateService);
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankServiceThrowsException() {
            doThrow(new IllegalStateException()).when(transactionProfileUpdateService).handlePiggyBankEvent(event);

            assertThrows(IllegalStateException.class, () -> updateDataConsumer.handlePiggyBankTransaction(event));

            verify(transactionProfileUpdateService).handlePiggyBankEvent(event);
            verifyNoInteractions(loginProfileUpdateService, accountChangeProfileUpdateService);
        }
    }
}
