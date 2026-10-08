package com.finovara.financeservice.sharedaccount.consumer;

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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedAccountConsumerTest {

    @Mock
    private SharedAccountSettingsFactory sharedAccountSettingsFactory;

    @Mock
    private SharedWalletService sharedWalletService;

    @Mock
    private SharedAccountDeletionHandler sharedAccountDeletionHandler;

    @InjectMocks
    private SharedAccountConsumer sharedAccountConsumer;

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
            sharedAccountConsumer.createDefault(event);

            verify(sharedWalletService).createSharedWallet(inviterUserId, inviteeUserId);
        }

        @Test
        void shouldPassInviterAndInviteeInCorrectOrderWhenEventIsReceived() {
            sharedAccountConsumer.createDefault(event);

            verify(sharedWalletService).createSharedWallet(inviterUserId, inviteeUserId);
            verify(sharedWalletService, never()).createSharedWallet(inviteeUserId, inviterUserId);
        }

        @Test
        void shouldInvokeSharedWalletServiceExactlyOnceWhenEventIsReceived() {
            sharedAccountConsumer.createDefault(event);

            verify(sharedWalletService).createSharedWallet(inviterUserId, inviteeUserId);
            verifyNoMoreInteractions(sharedWalletService);
        }

        @Test
        void shouldNotInteractWithOtherDependenciesWhenEventIsReceived() {
            sharedAccountConsumer.createDefault(event);

            verifyNoInteractions(sharedAccountSettingsFactory, sharedAccountDeletionHandler);
        }

        @Test
        void shouldThrowExceptionWhenSharedWalletServiceThrowsException() {
            doThrow(new IllegalStateException())
                    .when(sharedWalletService)
                    .createSharedWallet(inviterUserId, inviteeUserId);

            assertThrows(IllegalStateException.class, () -> sharedAccountConsumer.createDefault(event));
        }
    }

    @Nested
    class CreateDefaultWhenEventIsNull {

        @Test
        void shouldThrowExceptionWhenEventIsNull() {
            assertThrows(NullPointerException.class, () -> sharedAccountConsumer.createDefault(null));
        }

        @Test
        void shouldNotCreateSharedWalletWhenEventIsNull() {
            assertThrows(NullPointerException.class, () -> sharedAccountConsumer.createDefault(null));

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
            sharedAccountConsumer.createDefaultSettings(event);

            verify(sharedAccountSettingsFactory).createDefaultSharedAccountSettingsIfNotExist(inviterUserId, inviteeUserId);
        }

        @Test
        void shouldPassInviterAndInviteeInCorrectOrderWhenEventIsReceived() {
            sharedAccountConsumer.createDefaultSettings(event);

            verify(sharedAccountSettingsFactory).createDefaultSharedAccountSettingsIfNotExist(inviterUserId, inviteeUserId);
            verify(sharedAccountSettingsFactory, never()).createDefaultSharedAccountSettingsIfNotExist(inviteeUserId, inviterUserId);
        }

        @Test
        void shouldInvokeSettingsFactoryExactlyOnceWhenEventIsReceived() {
            sharedAccountConsumer.createDefaultSettings(event);

            verify(sharedAccountSettingsFactory).createDefaultSharedAccountSettingsIfNotExist(inviterUserId, inviteeUserId);
            verifyNoMoreInteractions(sharedAccountSettingsFactory);
        }

        @Test
        void shouldNotInteractWithOtherDependenciesWhenEventIsReceived() {
            sharedAccountConsumer.createDefaultSettings(event);

            verifyNoInteractions(sharedWalletService, sharedAccountDeletionHandler);
        }

        @Test
        void shouldThrowExceptionWhenSettingsFactoryThrowsException() {
            doThrow(new IllegalStateException())
                    .when(sharedAccountSettingsFactory)
                    .createDefaultSharedAccountSettingsIfNotExist(inviterUserId, inviteeUserId);

            assertThrows(IllegalStateException.class, () -> sharedAccountConsumer.createDefaultSettings(event));
        }
    }

    @Nested
    class CreateDefaultSettingsWhenEventIsNull {

        @Test
        void shouldThrowExceptionWhenEventIsNull() {
            assertThrows(NullPointerException.class, () -> sharedAccountConsumer.createDefaultSettings(null));
        }

        @Test
        void shouldNotCreateDefaultSettingsWhenEventIsNull() {
            assertThrows(NullPointerException.class, () -> sharedAccountConsumer.createDefaultSettings(null));

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
            sharedAccountConsumer.deleteDataFromSharedAccount(event);

            verify(sharedAccountDeletionHandler).handle(event);
        }

        @Test
        void shouldInvokeHandlerExactlyOnceWhenEventIsReceived() {
            sharedAccountConsumer.deleteDataFromSharedAccount(event);

            verify(sharedAccountDeletionHandler).handle(event);
            verifyNoMoreInteractions(sharedAccountDeletionHandler);
        }

        @Test
        void shouldNotInteractWithOtherDependenciesWhenEventIsReceived() {
            sharedAccountConsumer.deleteDataFromSharedAccount(event);

            verifyNoInteractions(sharedWalletService, sharedAccountSettingsFactory);
        }

        @Test
        void shouldThrowExceptionWhenDeletionHandlerThrowsException() {
            doThrow(new IllegalStateException()).when(sharedAccountDeletionHandler).handle(event);

            assertThrows(IllegalStateException.class, () -> sharedAccountConsumer.deleteDataFromSharedAccount(event));
        }

        @Test
        void shouldPassNullToHandlerWhenEventIsNull() {
            sharedAccountConsumer.deleteDataFromSharedAccount(null);

            verify(sharedAccountDeletionHandler).handle(null);
        }

        @Test
        void shouldThrowExceptionWhenEventIsNullAndHandlerRejectsNull() {
            doThrow(new NullPointerException()).when(sharedAccountDeletionHandler).handle(null);

            assertThrows(NullPointerException.class, () -> sharedAccountConsumer.deleteDataFromSharedAccount(null));
        }
    }
}