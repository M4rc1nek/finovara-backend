package com.finovara.authservice.sharedaccount.service.invitation;

import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.authservice.user.dto.UserDataDto;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.notification.event.sharedaccount.invitation.SharedAccountInvitationExpiredEvent;
import com.finovara.contracts.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationExpirationServiceTest {

    @Mock
    private SharedAccountInvitationRepository sharedAccountInvitationRepository;

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private OutboxService outboxService;

    private InvitationExpirationService invitationExpirationService;

    private Long inviterUserId;

    private Long inviteeUserId;

    private Long invitationId;

    private SharedAccountInvitation invitation;

    @BeforeEach
    void setUp() {
        invitationExpirationService = new InvitationExpirationService(
                sharedAccountInvitationRepository, userManagerService, outboxService);

        inviterUserId = 1L;
        inviteeUserId = 2L;
        invitationId = 10L;

        invitation = SharedAccountInvitation.builder()
                .id(invitationId)
                .inviterUserId(inviterUserId)
                .inviteeUserId(inviteeUserId)
                .build();
    }

    @Nested
    class ExpireInvitation {

        @Test
        void shouldDeleteInvitationWhenExpiringInvitation() {
            UserDataDto invitee = mock(UserDataDto.class);
            when(invitee.username()).thenReturn("invitee");
            when(userManagerService.getUserDataWithProfileImg(inviteeUserId)).thenReturn(invitee);

            invitationExpirationService.expireInvitation(invitation);

            verify(sharedAccountInvitationRepository).delete(invitation);
        }

        @Test
        void shouldPublishExpiredEventWithInviterAndInviteeUsernameWhenExpiringInvitation() {
            UserDataDto invitee = mock(UserDataDto.class);
            when(invitee.username()).thenReturn("inviteeUsername");
            when(userManagerService.getUserDataWithProfileImg(inviteeUserId)).thenReturn(invitee);

            invitationExpirationService.expireInvitation(invitation);

            ArgumentCaptor<SharedAccountInvitationExpiredEvent> eventCaptor =
                    ArgumentCaptor.forClass(SharedAccountInvitationExpiredEvent.class);
            verify(outboxService).save(eq("User"), eq(inviterUserId.toString()),
                    eq("notification.shared-account.invitation-expired"), eventCaptor.capture());

            assertEquals(inviterUserId, eventCaptor.getValue().userId());
            assertEquals("inviteeUsername", eventCaptor.getValue().inviteeUsername());
        }

        @Test
        void shouldFetchInviteeDataBeforeDeletingInvitation() {
            UserDataDto invitee = mock(UserDataDto.class);
            when(invitee.username()).thenReturn("invitee");
            when(userManagerService.getUserDataWithProfileImg(inviteeUserId)).thenReturn(invitee);

            invitationExpirationService.expireInvitation(invitation);

            verify(userManagerService, times(1)).getUserDataWithProfileImg(inviteeUserId);
            verify(sharedAccountInvitationRepository, times(1)).delete(any(SharedAccountInvitation.class));
        }
    }
}