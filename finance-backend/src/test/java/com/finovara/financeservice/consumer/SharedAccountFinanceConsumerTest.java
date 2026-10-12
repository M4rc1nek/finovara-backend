package com.finovara.financeservice.consumer;

import com.finovara.contracts.sharedaccount.event.UsersCreatedSharedAccountEvent;
import com.finovara.contracts.sharedaccount.event.deletion.SharedAccountDeletedEvent;
import com.finovara.contracts.sharedaccount.event.settings.SharedAccountCreateDefaultSettingsEvent;
import com.finovara.financeservice.sharedaccount.deletion.SharedAccountDeletionHandler;
import com.finovara.financeservice.sharedaccount.settings.factory.SharedAccountSettingsFactory;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SharedAccountFinanceConsumerTest {

    @Mock
    private SharedAccountSettingsFactory sharedAccountSettingsFactory;

    @Mock
    private SharedWalletService sharedWalletService;

    @Mock
    private SharedAccountDeletionHandler sharedAccountDeletionHandler;

    @InjectMocks
    private SharedAccountFinanceConsumer sharedAccountFinanceConsumer;

    @Nested
    class CreateDefault {

        private Long inviterUserId;
        private Long inviteeUserId;
        private UsersCreatedSharedAccountEvent event;

        @BeforeEach
        void setUp() {
            inviterUserId = 1L;
            inviteeUserId = 2L;
            event = mock(UsersCreatedSharedAccountEvent.class);
            when(event.inviterUserId()).thenReturn(inviterUserId);
            when(event.inviteeUserId()).thenReturn(inviteeUserId);
        }

        @Test
        void shouldCreateSharedWalletWhenEventIsReceived() {
            sharedAccountFinanceConsumer.createDefaultSharedAccountWallet(event);

            verify(sharedWalletService).createSharedWallet(inviterUserId, inviteeUserId);
        }

        @Test
        void shouldPassInviterAndInviteeInCorrectOrderWhenEventIsReceived() {
            sharedAccountFinanceConsumer.createDefaultSharedAccountWallet(event);

            verify(sharedWalletService).createSharedWallet(inviterUserId, inviteeUserId);
            verify(sharedWalletService, never()).createSharedWallet(inviteeUserId, inviterUserId);
        }

        @Test
        void shouldInvokeSharedWalletServiceExactlyOnceWhenEventIsReceived() {
            sharedAccountFinanceConsumer.createDefaultSharedAccountWallet(event);

            verify(sharedWalletService).createSharedWallet(inviterUserId, inviteeUserId);
            verifyNoMoreInteractions(sharedWalletService);
        }

        @Test
        void shouldNotInteractWithOtherDependenciesWhenEventIsReceived() {
            sharedAccountFinanceConsumer.createDefaultSharedAccountWallet(event);

            verifyNoInteractions(sharedAccountSettingsFactory, sharedAccountDeletionHandler);
        }

        @Test
        void shouldThrowExceptionWhenSharedWalletServiceThrowsException() {
            doThrow(new IllegalStateException())
                    .when(sharedWalletService)
                    .createSharedWallet(inviterUserId, inviteeUserId);

            assertThrows(IllegalStateException.class, () -> sharedAccountFinanceConsumer.createDefaultSharedAccountWallet(event));
        }
    }

    @Nested
    class CreateDefaultWhenEventIsNull {

        @Test
        void shouldThrowExceptionWhenEventIsNull() {
            assertThrows(NullPointerException.class, () -> sharedAccountFinanceConsumer.createDefaultSharedAccountWallet(null));
        }

        @Test
        void shouldNotCreateSharedWalletWhenEventIsNull() {
            assertThrows(NullPointerException.class, () -> sharedAccountFinanceConsumer.createDefaultSharedAccountWallet(null));

            verify(sharedWalletService, never()).createSharedWallet(any(), any());
        }
    }

    @Nested
    class CreateDefaultSettings {

        private Long inviterUserId;
        private Long inviteeUserId;
        private SharedAccountCreateDefaultSettingsEvent event;

        @BeforeEach
        void setUp() {
            inviterUserId = 1L;
            inviteeUserId = 2L;
            event = mock(SharedAccountCreateDefaultSettingsEvent.class);
            when(event.inviterUserId()).thenReturn(inviterUserId);
            when(event.inviteeUserId()).thenReturn(inviteeUserId);
        }

        @Test
        void shouldCreateDefaultSettingsWhenEventIsReceived() {
            sharedAccountFinanceConsumer.createDefaultSharedAccountSettings(event);

            verify(sharedAccountSettingsFactory).createDefaultSharedAccountSettingsIfNotExist(inviterUserId, inviteeUserId);
        }

        @Test
        void shouldPassInviterAndInviteeInCorrectOrderWhenEventIsReceived() {
            sharedAccountFinanceConsumer.createDefaultSharedAccountSettings(event);

            verify(sharedAccountSettingsFactory).createDefaultSharedAccountSettingsIfNotExist(inviterUserId, inviteeUserId);
            verify(sharedAccountSettingsFactory, never()).createDefaultSharedAccountSettingsIfNotExist(inviteeUserId, inviterUserId);
        }

        @Test
        void shouldInvokeSettingsFactoryExactlyOnceWhenEventIsReceived() {
            sharedAccountFinanceConsumer.createDefaultSharedAccountSettings(event);

            verify(sharedAccountSettingsFactory).createDefaultSharedAccountSettingsIfNotExist(inviterUserId, inviteeUserId);
            verifyNoMoreInteractions(sharedAccountSettingsFactory);
        }

        @Test
        void shouldNotInteractWithOtherDependenciesWhenEventIsReceived() {
            sharedAccountFinanceConsumer.createDefaultSharedAccountSettings(event);

            verifyNoInteractions(sharedWalletService, sharedAccountDeletionHandler);
        }

        @Test
        void shouldThrowExceptionWhenSettingsFactoryThrowsException() {
            doThrow(new IllegalStateException())
                    .when(sharedAccountSettingsFactory)
                    .createDefaultSharedAccountSettingsIfNotExist(inviterUserId, inviteeUserId);

            assertThrows(IllegalStateException.class, () -> sharedAccountFinanceConsumer.createDefaultSharedAccountSettings(event));
        }
    }

    @Nested
    class CreateDefaultSettingsWhenEventIsNull {

        @Test
        void shouldThrowExceptionWhenEventIsNull() {
            assertThrows(NullPointerException.class, () -> sharedAccountFinanceConsumer.createDefaultSharedAccountSettings(null));
        }

        @Test
        void shouldNotCreateDefaultSettingsWhenEventIsNull() {
            assertThrows(NullPointerException.class, () -> sharedAccountFinanceConsumer.createDefaultSharedAccountSettings(null));

            verify(sharedAccountSettingsFactory, never()).createDefaultSharedAccountSettingsIfNotExist(any(), any());
        }
    }

    @Nested
    class DeleteDataFromSharedAccount {

        private SharedAccountDeletedEvent event;

        @BeforeEach
        void setUp() {
            event = mock(SharedAccountDeletedEvent.class);
        }

        @Test
        void shouldDelegateToDeletionHandlerWhenEventIsReceived() {
            sharedAccountFinanceConsumer.deleteDataFromSharedAccount(event);

            verify(sharedAccountDeletionHandler).handle(event);
        }

        @Test
        void shouldInvokeHandlerExactlyOnceWhenEventIsReceived() {
            sharedAccountFinanceConsumer.deleteDataFromSharedAccount(event);

            verify(sharedAccountDeletionHandler).handle(event);
            verifyNoMoreInteractions(sharedAccountDeletionHandler);
        }

        @Test
        void shouldNotInteractWithOtherDependenciesWhenEventIsReceived() {
            sharedAccountFinanceConsumer.deleteDataFromSharedAccount(event);

            verifyNoInteractions(sharedWalletService, sharedAccountSettingsFactory);
        }

        @Test
        void shouldThrowExceptionWhenDeletionHandlerThrowsException() {
            doThrow(new IllegalStateException()).when(sharedAccountDeletionHandler).handle(event);

            assertThrows(IllegalStateException.class, () -> sharedAccountFinanceConsumer.deleteDataFromSharedAccount(event));
        }

        @Test
        void shouldPassNullToHandlerWhenEventIsNull() {
            sharedAccountFinanceConsumer.deleteDataFromSharedAccount(null);

            verify(sharedAccountDeletionHandler).handle(null);
        }

        @Test
        void shouldThrowExceptionWhenEventIsNullAndHandlerRejectsNull() {
            doThrow(new NullPointerException()).when(sharedAccountDeletionHandler).handle(null);

            assertThrows(NullPointerException.class, () -> sharedAccountFinanceConsumer.deleteDataFromSharedAccount(null));
        }
    }
}