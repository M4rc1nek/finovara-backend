package com.finovara.authservice.sharedaccount.processor.invitation;

import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.authservice.sharedaccount.service.invitation.InvitationExpirationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationExpirationProcessorTest {

    @Mock
    private SharedAccountInvitationRepository sharedAccountInvitationRepository;

    @Mock
    private InvitationExpirationService invitationExpirationService;

    private InvitationExpirationProcessor invitationExpirationProcessor;

    @BeforeEach
    void setUp() {
        invitationExpirationProcessor = new InvitationExpirationProcessor(
                sharedAccountInvitationRepository, invitationExpirationService);
    }

    @Nested
    class ExpireOverdueInvitations {

        @Test
        void shouldExpireEachOverdueInvitationWhenInvitationsExist() {
            SharedAccountInvitation invitationOne = mock(SharedAccountInvitation.class);
            SharedAccountInvitation invitationTwo = mock(SharedAccountInvitation.class);
            when(sharedAccountInvitationRepository.findAllExpired(any(LocalDateTime.class)))
                    .thenReturn(List.of(invitationOne, invitationTwo));

            invitationExpirationProcessor.expireOverdueInvitations();

            verify(invitationExpirationService, times(1)).expireInvitation(invitationOne);
            verify(invitationExpirationService, times(1)).expireInvitation(invitationTwo);
        }

        @Test
        void shouldNotCallExpireInvitationWhenNoOverdueInvitationsExist() {
            when(sharedAccountInvitationRepository.findAllExpired(any(LocalDateTime.class))).thenReturn(List.of());

            invitationExpirationProcessor.expireOverdueInvitations();

            verify(invitationExpirationService, never()).expireInvitation(any(SharedAccountInvitation.class));
        }

        @Test
        void shouldProcessAllInvitationsExactlyOnceWhenMultipleInvitationsExist() {
            SharedAccountInvitation invitationOne = mock(SharedAccountInvitation.class);
            SharedAccountInvitation invitationTwo = mock(SharedAccountInvitation.class);
            SharedAccountInvitation invitationThree = mock(SharedAccountInvitation.class);
            when(sharedAccountInvitationRepository.findAllExpired(any(LocalDateTime.class)))
                    .thenReturn(List.of(invitationOne, invitationTwo, invitationThree));

            invitationExpirationProcessor.expireOverdueInvitations();

            verify(invitationExpirationService, times(3)).expireInvitation(any(SharedAccountInvitation.class));
        }
    }
}