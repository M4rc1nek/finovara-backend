package com.finovara.financeservice.sharedaccount.consumer;

import com.finovara.contracts.event.finance.sharedaccount.UsersCreatedSharedAccountEvent;
import com.finovara.financeservice.sharedaccount.service.wallet.SharedWalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class InvitationConsumerTest {

    @Mock
    private SharedWalletService sharedWalletService;

    private InvitationConsumer invitationConsumer;

    @BeforeEach
    void setUp() {
        invitationConsumer = new InvitationConsumer(sharedWalletService);
    }

    @Nested
    class CreateDefault {

        @Test
        void shouldCreateSharedWalletWhenInvitationAccepted() {
            Long inviterUserId = 1L;
            Long inviteeUserId = 2L;
            UsersCreatedSharedAccountEvent event = new UsersCreatedSharedAccountEvent(inviterUserId, inviteeUserId);

            invitationConsumer.createDefault(event);

            verify(sharedWalletService).createSharedWallet(inviterUserId, inviteeUserId);
            verifyNoMoreInteractions(sharedWalletService);
        }
    }
}