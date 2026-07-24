package com.finovara.authservice.sharedaccount.service.invitation;

import com.finovara.authservice.sharedaccount.model.SharedAccount;
import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import com.finovara.authservice.sharedaccount.model.role.SharedRole;
import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountRepository;
import com.finovara.authservice.user.dto.UserDataDto;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.event.activity.sharedaccount.SharedAccountActivityEvent;
import com.finovara.contracts.event.finance.sharedaccount.SharedAccountCreateDefaultSettingsEvent;
import com.finovara.contracts.event.finance.sharedaccount.UsersCreatedSharedAccountEvent;
import com.finovara.contracts.event.notification.sharedaccount.invitation.UserAcceptSharedAccountInvitationEvent;
import com.finovara.contracts.event.notification.sharedaccount.invitation.UserRejectSharedAccountInvitationEvent;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationResponseServiceTest {

    @Mock
    private SharedAccountInvitationRepository sharedAccountInvitationRepository;

    @Mock
    private SharedAccountRepository sharedAccountRepository;

    @Mock
    private InvitationValidator invitationValidator;

    @Mock
    private InvitationExpirationService invitationExpirationService;

    @Mock
    private SharedAccountMemberService sharedAccountMemberService;

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private InvitationResponseService invitationResponseService;

    private Long inviterUserId;
    private Long inviteeUserId;
    private Long invitationId;
    private SharedAccountInvitation pendingInvitation;

    @BeforeEach
    void setUp() {
        inviterUserId = 1L;
        inviteeUserId = 2L;
        invitationId = 10L;
        pendingInvitation = mock(SharedAccountInvitation.class);
    }

    @Nested
    class AcceptInvite {

        @Test
        void shouldCreateSharedAccountWhenAcceptingValidInvitation() {
            when(pendingInvitation.getInviterUserId()).thenReturn(inviterUserId);
            when(pendingInvitation.getInviteeUserId()).thenReturn(inviteeUserId);
            when(pendingInvitation.hasExpired()).thenReturn(false);
            when(sharedAccountInvitationRepository.findById(invitationId)).thenReturn(Optional.of(pendingInvitation));

            UserDataDto inviter = mock(UserDataDto.class);
            when(inviter.username()).thenReturn("inviterName");
            when(inviter.email()).thenReturn("inviter@mail.com");
            UserDataDto invitee = mock(UserDataDto.class);
            when(invitee.username()).thenReturn("inviteeName");

            when(userManagerService.getUserDataWithProfileImg(inviterUserId)).thenReturn(inviter);
            when(userManagerService.getUserDataWithProfileImg(inviteeUserId)).thenReturn(invitee);

            SharedAccount savedAccount = SharedAccount.builder().id(100L).createdAt(LocalDateTime.now()).build();
            when(sharedAccountRepository.save(any(SharedAccount.class))).thenReturn(savedAccount);

            invitationResponseService.acceptInvite(inviteeUserId, invitationId);

            verify(sharedAccountInvitationRepository).delete(pendingInvitation);
            verify(sharedAccountMemberService).createMember(savedAccount, inviterUserId, SharedRole.OWNER);
            verify(sharedAccountMemberService).createMember(savedAccount, inviteeUserId, SharedRole.MEMBER);
        }

        @Test
        void shouldPublishAllOutboxEventsWhenAcceptingValidInvitation() {
            when(pendingInvitation.getInviterUserId()).thenReturn(inviterUserId);
            when(pendingInvitation.getInviteeUserId()).thenReturn(inviteeUserId);
            when(pendingInvitation.hasExpired()).thenReturn(false);
            when(sharedAccountInvitationRepository.findById(invitationId)).thenReturn(Optional.of(pendingInvitation));

            UserDataDto inviter = mock(UserDataDto.class);
            when(inviter.username()).thenReturn("inviterName");
            when(inviter.email()).thenReturn("inviter@mail.com");
            UserDataDto invitee = mock(UserDataDto.class);
            when(invitee.username()).thenReturn("inviteeName");

            when(userManagerService.getUserDataWithProfileImg(inviterUserId)).thenReturn(inviter);
            when(userManagerService.getUserDataWithProfileImg(inviteeUserId)).thenReturn(invitee);

            SharedAccount savedAccount = SharedAccount.builder().id(100L).createdAt(LocalDateTime.now()).build();
            when(sharedAccountRepository.save(any(SharedAccount.class))).thenReturn(savedAccount);

            invitationResponseService.acceptInvite(inviteeUserId, invitationId);

            ArgumentCaptor<UsersCreatedSharedAccountEvent> createdCaptor =
                    ArgumentCaptor.forClass(UsersCreatedSharedAccountEvent.class);
            verify(outboxService).save(eq("User"), eq(inviterUserId.toString()),
                    eq("finance.shared-account.invitation-accepted"), createdCaptor.capture());
            assertEquals(inviterUserId, createdCaptor.getValue().inviterUserId());

            verify(outboxService).save(eq("User"), eq(inviteeUserId.toString()),
                    eq("finance.shared-account.create-default-settings"), any(SharedAccountCreateDefaultSettingsEvent.class));

            verify(outboxService).save(eq("User"), eq(inviteeUserId.toString()),
                    eq("activity.shared-account"), any(SharedAccountActivityEvent.class));

            ArgumentCaptor<UserAcceptSharedAccountInvitationEvent> acceptedCaptor =
                    ArgumentCaptor.forClass(UserAcceptSharedAccountInvitationEvent.class);
            verify(outboxService).save(eq("User"), eq(inviterUserId.toString()),
                    eq("notification.shared-account.invitation-accepted"), acceptedCaptor.capture());
            assertEquals("inviteeName", acceptedCaptor.getValue().inviteeUsername());
        }

        @Test
        void shouldThrowExceptionWhenInvitationNotFound() {
            when(sharedAccountInvitationRepository.findById(invitationId)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationResponseService.acceptInvite(inviteeUserId, invitationId));

            verify(sharedAccountRepository, never()).save(any(SharedAccount.class));
        }

        @Test
        void shouldThrowExceptionWhenInvitationOwnershipInvalid() {
            when(pendingInvitation.getInviteeUserId()).thenReturn(inviteeUserId);
            when(sharedAccountInvitationRepository.findById(invitationId)).thenReturn(Optional.of(pendingInvitation));
            doThrow(new RequestedEntityNotFoundException("Invitation ownership invalid"))
                    .when(invitationValidator).validateInvitationOwnership(inviteeUserId, inviteeUserId, invitationId);

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationResponseService.acceptInvite(inviteeUserId, invitationId));

            verify(sharedAccountInvitationRepository, never()).delete(any(SharedAccountInvitation.class));
        }

        @Test
        void shouldExpireInvitationAndThrowExceptionWhenInvitationExpired() {
            when(pendingInvitation.getInviteeUserId()).thenReturn(inviteeUserId);
            when(pendingInvitation.hasExpired()).thenReturn(true);
            when(sharedAccountInvitationRepository.findById(invitationId)).thenReturn(Optional.of(pendingInvitation));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationResponseService.acceptInvite(inviteeUserId, invitationId));

            verify(invitationExpirationService).expireInvitation(pendingInvitation);
            verify(sharedAccountRepository, never()).save(any(SharedAccount.class));
        }

        @Test
        void shouldThrowExceptionWhenAcceptValidationFails() {
            when(pendingInvitation.getInviterUserId()).thenReturn(inviterUserId);
            when(pendingInvitation.getInviteeUserId()).thenReturn(inviteeUserId);
            when(pendingInvitation.hasExpired()).thenReturn(false);
            when(sharedAccountInvitationRepository.findById(invitationId)).thenReturn(Optional.of(pendingInvitation));
            doThrow(new RequestedEntityNotFoundException("Cannot accept invitation"))
                    .when(invitationValidator).validateAcceptInvite(inviterUserId, inviteeUserId, invitationId);

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationResponseService.acceptInvite(inviteeUserId, invitationId));

            verify(sharedAccountInvitationRepository, never()).delete(any(SharedAccountInvitation.class));
            verify(outboxService, never()).save(anyString(), anyString(), anyString(), any());
        }
    }

    @Nested
    class RejectInvite {

        @Test
        void shouldDeleteInvitationWhenRejectingValidInvitation() {
            when(pendingInvitation.getInviterUserId()).thenReturn(inviterUserId);
            when(pendingInvitation.getInviteeUserId()).thenReturn(inviteeUserId);
            when(pendingInvitation.hasExpired()).thenReturn(false);
            when(sharedAccountInvitationRepository.findById(invitationId)).thenReturn(Optional.of(pendingInvitation));

            UserDataDto invitee = mock(UserDataDto.class);
            when(invitee.username()).thenReturn("inviteeName");
            UserDataDto inviter = mock(UserDataDto.class);
            when(inviter.username()).thenReturn("inviterName");
            when(inviter.email()).thenReturn("inviter@mail.com");

            when(userManagerService.getUserDataWithProfileImg(inviteeUserId)).thenReturn(invitee);
            when(userManagerService.getUserDataWithProfileImg(inviterUserId)).thenReturn(inviter);

            invitationResponseService.rejectInvite(inviteeUserId, invitationId);

            verify(sharedAccountInvitationRepository).delete(pendingInvitation);
        }

        @Test
        void shouldPublishRejectionEventsWhenRejectingValidInvitation() {
            when(pendingInvitation.getInviterUserId()).thenReturn(inviterUserId);
            when(pendingInvitation.getInviteeUserId()).thenReturn(inviteeUserId);
            when(pendingInvitation.hasExpired()).thenReturn(false);
            when(sharedAccountInvitationRepository.findById(invitationId)).thenReturn(Optional.of(pendingInvitation));

            UserDataDto invitee = mock(UserDataDto.class);
            when(invitee.username()).thenReturn("inviteeName");
            UserDataDto inviter = mock(UserDataDto.class);
            when(inviter.username()).thenReturn("inviterName");
            when(inviter.email()).thenReturn("inviter@mail.com");

            when(userManagerService.getUserDataWithProfileImg(inviteeUserId)).thenReturn(invitee);
            when(userManagerService.getUserDataWithProfileImg(inviterUserId)).thenReturn(inviter);

            invitationResponseService.rejectInvite(inviteeUserId, invitationId);

            ArgumentCaptor<UserRejectSharedAccountInvitationEvent> rejectCaptor =
                    ArgumentCaptor.forClass(UserRejectSharedAccountInvitationEvent.class);
            verify(outboxService).save(eq("User"), eq(inviterUserId.toString()),
                    eq("user.shared-account.reject-invitation"), rejectCaptor.capture());
            assertEquals("inviteeName", rejectCaptor.getValue().inviteeUsername());

            verify(outboxService).save(eq("User"), eq(inviteeUserId.toString()),
                    eq("activity.shared-account"), any(SharedAccountActivityEvent.class));
        }

        @Test
        void shouldThrowExceptionWhenInvitationToRejectNotFound() {
            when(sharedAccountInvitationRepository.findById(invitationId)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationResponseService.rejectInvite(inviteeUserId, invitationId));

            verify(sharedAccountInvitationRepository, never()).delete(any(SharedAccountInvitation.class));
        }

        @Test
        void shouldExpireInvitationAndThrowExceptionWhenRejectingExpiredInvitation() {
            when(pendingInvitation.getInviteeUserId()).thenReturn(inviteeUserId);
            when(pendingInvitation.hasExpired()).thenReturn(true);
            when(sharedAccountInvitationRepository.findById(invitationId)).thenReturn(Optional.of(pendingInvitation));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationResponseService.rejectInvite(inviteeUserId, invitationId));

            verify(invitationExpirationService).expireInvitation(pendingInvitation);
            verify(outboxService, never()).save(anyString(), anyString(), anyString(), any());
        }

        @Test
        void shouldThrowExceptionWhenInvitationOwnershipInvalidOnReject() {
            when(pendingInvitation.getInviteeUserId()).thenReturn(inviteeUserId);
            when(sharedAccountInvitationRepository.findById(invitationId)).thenReturn(Optional.of(pendingInvitation));
            doThrow(new RequestedEntityNotFoundException("Invitation ownership invalid"))
                    .when(invitationValidator).validateInvitationOwnership(inviteeUserId, inviteeUserId, invitationId);

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationResponseService.rejectInvite(inviteeUserId, invitationId));

            verify(sharedAccountInvitationRepository, never()).delete(any(SharedAccountInvitation.class));
        }
    }
}