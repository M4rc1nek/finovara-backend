package com.finovara.securitymonitoring.consumer;

import com.finovara.contracts.user.event.UserCreatedEvent;
import com.finovara.contracts.user.event.account.delete.UserAccountDeletedEvent;
import com.finovara.securitymonitoring.accountchange.factory.AccountChangeProfileFactory;
import com.finovara.securitymonitoring.accountchange.service.AccountChangeProfileUpdateService;
import com.finovara.securitymonitoring.clientdata.factory.ClientDataFactory;
import com.finovara.securitymonitoring.login.service.LoginProfileUpdateService;
import com.finovara.securitymonitoring.transaction.factory.TransactionProfileFactory;
import com.finovara.securitymonitoring.transaction.service.TransactionProfileUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityMonitoringConsumerTest {

    @Mock
    private ClientDataFactory clientDataFactory;

    @Mock
    private TransactionProfileFactory transactionProfileFactory;

    @Mock
    private AccountChangeProfileFactory accountChangeProfileFactory;

    @Mock
    private TransactionProfileUpdateService transactionProfileUpdateService;

    @Mock
    private AccountChangeProfileUpdateService accountChangeProfileUpdateService;

    @Mock
    private LoginProfileUpdateService loginProfileUpdateService;

    @InjectMocks
    private SecurityMonitoringConsumer securityMonitoringConsumer;

    @Nested
    class CreateDefaultProfiles {

        private UserCreatedEvent event;

        @BeforeEach
        void setUp() {
            event = new UserCreatedEvent(123L, "john.doe", "john.doe@example.com", LocalDateTime.of(2026, 1, 1, 12, 0));
        }

        @Test
        void shouldCreateDefaultProfilesWhenUserCreatedEventIsValid() {
            securityMonitoringConsumer.createDefaultProfiles(event);

            verify(clientDataFactory).createDefaultDataIfNotExist(123L);
            verify(transactionProfileFactory).createDefaultDataIfNotExist(123L);
            verify(accountChangeProfileFactory).createDefaultDataIfNotExist(123L);
            verifyNoMoreInteractions(clientDataFactory, transactionProfileFactory, accountChangeProfileFactory);
            verifyNoInteractions(transactionProfileUpdateService, accountChangeProfileUpdateService, loginProfileUpdateService);
        }

        @Test
        void shouldCreateDefaultProfilesWhenUserIdIsZero() {
            UserCreatedEvent eventWithZeroUserId = new UserCreatedEvent(0L, "john.doe", "john.doe@example.com", LocalDateTime.of(2026, 1, 1, 12, 0));

            securityMonitoringConsumer.createDefaultProfiles(eventWithZeroUserId);

            verify(clientDataFactory).createDefaultDataIfNotExist(0L);
            verify(transactionProfileFactory).createDefaultDataIfNotExist(0L);
            verify(accountChangeProfileFactory).createDefaultDataIfNotExist(0L);
            verifyNoInteractions(transactionProfileUpdateService, accountChangeProfileUpdateService, loginProfileUpdateService);
        }

        @Test
        void shouldCreateDefaultProfilesWhenUserIdIsNull() {
            UserCreatedEvent eventWithNullUserId = new UserCreatedEvent(null, "john.doe", "john.doe@example.com", LocalDateTime.of(2026, 1, 1, 12, 0));

            securityMonitoringConsumer.createDefaultProfiles(eventWithNullUserId);

            verify(clientDataFactory).createDefaultDataIfNotExist(null);
            verify(transactionProfileFactory).createDefaultDataIfNotExist(null);
            verify(accountChangeProfileFactory).createDefaultDataIfNotExist(null);
            verifyNoInteractions(transactionProfileUpdateService, accountChangeProfileUpdateService, loginProfileUpdateService);
        }

        @Test
        void shouldCreateDefaultProfilesWhenOptionalEventFieldsAreNull() {
            UserCreatedEvent eventWithNullFields = new UserCreatedEvent(123L, null, null, null);

            securityMonitoringConsumer.createDefaultProfiles(eventWithNullFields);

            verify(clientDataFactory).createDefaultDataIfNotExist(123L);
            verify(transactionProfileFactory).createDefaultDataIfNotExist(123L);
            verify(accountChangeProfileFactory).createDefaultDataIfNotExist(123L);
            verifyNoInteractions(transactionProfileUpdateService, accountChangeProfileUpdateService, loginProfileUpdateService);
        }

        @Test
        void shouldThrowExceptionWhenClientDataFactoryThrowsException() {
            doThrow(new IllegalStateException()).when(clientDataFactory).createDefaultDataIfNotExist(event.userId());

            assertThrows(IllegalStateException.class, () -> securityMonitoringConsumer.createDefaultProfiles(event));

            verify(clientDataFactory).createDefaultDataIfNotExist(event.userId());
            verifyNoInteractions(transactionProfileFactory, accountChangeProfileFactory, transactionProfileUpdateService, accountChangeProfileUpdateService, loginProfileUpdateService);
        }

        @Test
        void shouldThrowExceptionWhenTransactionProfileFactoryThrowsException() {
            doThrow(new IllegalStateException()).when(transactionProfileFactory).createDefaultDataIfNotExist(event.userId());

            assertThrows(IllegalStateException.class, () -> securityMonitoringConsumer.createDefaultProfiles(event));

            verify(clientDataFactory).createDefaultDataIfNotExist(event.userId());
            verify(transactionProfileFactory).createDefaultDataIfNotExist(event.userId());
            verifyNoInteractions(accountChangeProfileFactory, transactionProfileUpdateService, accountChangeProfileUpdateService, loginProfileUpdateService);
        }

        @Test
        void shouldThrowExceptionWhenAccountChangeProfileFactoryThrowsException() {
            doThrow(new IllegalStateException()).when(accountChangeProfileFactory).createDefaultDataIfNotExist(event.userId());

            assertThrows(IllegalStateException.class, () -> securityMonitoringConsumer.createDefaultProfiles(event));

            verify(clientDataFactory).createDefaultDataIfNotExist(event.userId());
            verify(transactionProfileFactory).createDefaultDataIfNotExist(event.userId());
            verify(accountChangeProfileFactory).createDefaultDataIfNotExist(event.userId());
            verifyNoInteractions(transactionProfileUpdateService, accountChangeProfileUpdateService, loginProfileUpdateService);
        }
    }

    @Nested
    class DeleteProfilesData {

        private UserAccountDeletedEvent event;

        @BeforeEach
        void setUp() {
            event = new UserAccountDeletedEvent(123L);
        }

        @Test
        void shouldDeleteAllProfilesDataWhenUserAccountDeletedEventIsValid() {
            securityMonitoringConsumer.deleteProfilesData(event);

            verify(loginProfileUpdateService).deleteByUserId(123L);
            verify(accountChangeProfileUpdateService).deleteByUserId(123L);
            verify(transactionProfileUpdateService).deleteByUserId(123L);
            verifyNoMoreInteractions(loginProfileUpdateService, accountChangeProfileUpdateService, transactionProfileUpdateService);
            verifyNoInteractions(clientDataFactory, transactionProfileFactory, accountChangeProfileFactory);
        }

        @Test
        void shouldDeleteAllProfilesDataWhenUserIdIsZero() {
            UserAccountDeletedEvent eventWithZeroUserId = new UserAccountDeletedEvent(0L);

            securityMonitoringConsumer.deleteProfilesData(eventWithZeroUserId);

            verify(loginProfileUpdateService).deleteByUserId(0L);
            verify(accountChangeProfileUpdateService).deleteByUserId(0L);
            verify(transactionProfileUpdateService).deleteByUserId(0L);
            verifyNoInteractions(clientDataFactory, transactionProfileFactory, accountChangeProfileFactory);
        }

        @Test
        void shouldDeleteAllProfilesDataWhenUserIdIsNull() {
            UserAccountDeletedEvent eventWithNullUserId = new UserAccountDeletedEvent(null);

            securityMonitoringConsumer.deleteProfilesData(eventWithNullUserId);

            verify(loginProfileUpdateService).deleteByUserId(null);
            verify(accountChangeProfileUpdateService).deleteByUserId(null);
            verify(transactionProfileUpdateService).deleteByUserId(null);
            verifyNoInteractions(clientDataFactory, transactionProfileFactory, accountChangeProfileFactory);
        }

        @Test
        void shouldThrowExceptionWhenLoginProfileServiceThrowsException() {
            doThrow(new IllegalStateException()).when(loginProfileUpdateService).deleteByUserId(event.userId());

            assertThrows(IllegalStateException.class, () -> securityMonitoringConsumer.deleteProfilesData(event));

            verify(loginProfileUpdateService).deleteByUserId(event.userId());
            verifyNoInteractions(accountChangeProfileUpdateService, transactionProfileUpdateService, clientDataFactory, transactionProfileFactory, accountChangeProfileFactory);
        }

        @Test
        void shouldThrowExceptionWhenAccountChangeProfileServiceThrowsException() {
            doThrow(new IllegalStateException()).when(accountChangeProfileUpdateService).deleteByUserId(event.userId());

            assertThrows(IllegalStateException.class, () -> securityMonitoringConsumer.deleteProfilesData(event));

            verify(loginProfileUpdateService).deleteByUserId(event.userId());
            verify(accountChangeProfileUpdateService).deleteByUserId(event.userId());
            verifyNoInteractions(transactionProfileUpdateService, clientDataFactory, transactionProfileFactory, accountChangeProfileFactory);
        }

        @Test
        void shouldThrowExceptionWhenTransactionProfileServiceThrowsException() {
            doThrow(new IllegalStateException()).when(transactionProfileUpdateService).deleteByUserId(event.userId());

            assertThrows(IllegalStateException.class, () -> securityMonitoringConsumer.deleteProfilesData(event));

            verify(loginProfileUpdateService).deleteByUserId(event.userId());
            verify(accountChangeProfileUpdateService).deleteByUserId(event.userId());
            verify(transactionProfileUpdateService).deleteByUserId(event.userId());
            verifyNoInteractions(clientDataFactory, transactionProfileFactory, accountChangeProfileFactory);
        }
    }
}
