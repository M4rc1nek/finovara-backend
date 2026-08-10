package com.finovara.authservice.sharedaccount.service.invitation;

import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import com.finovara.authservice.sharedaccount.model.SharedAccount;
import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import com.finovara.authservice.sharedaccount.model.role.SharedRole;
import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountRepository;
import com.finovara.authservice.user.dto.UserDataDto;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.authorization.dto.ConfirmAuthorizationCodeDto;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
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

    @Mock
    private AdditionalAuthorizationService additionalAuthorizationService;

    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @InjectMocks
    private InvitationResponseService invitationResponseService;

    private static final Long INVITER_USER_ID = 1L;
    private static final Long INVITEE_USER_ID = 2L;
    private static final Long INVITATION_ID = 10L;
    private static final String AUTHORIZATION_CODE = "auth";

    private SharedAccountInvitation pendingInvitation;
    private ConfirmAuthorizationCodeDto resolvedAuthorizationCode;

    @BeforeEach
    void setUp() {
        pendingInvitation = mock(SharedAccountInvitation.class);
        resolvedAuthorizationCode = mock(ConfirmAuthorizationCodeDto.class);

        when(additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE)).thenReturn(resolvedAuthorizationCode);
        doNothing().when(additionalAuthorizationService)
                .confirmAdditionalAuthorizationCode(any(), eq(resolvedAuthorizationCode));
    }

    @Nested
    class AcceptInvite {

        @Test
        void shouldCreateSharedAccountWhenAcceptingValidInvitation() {
            when(pendingInvitation.getInviterUserId()).thenReturn(INVITER_USER_ID);
            when(pendingInvitation.getInviteeUserId()).thenReturn(INVITEE_USER_ID);
            when(pendingInvitation.hasExpired()).thenReturn(false);
            when(sharedAccountInvitationRepository.findById(INVITATION_ID)).thenReturn(Optional.of(pendingInvitation));

            UserDataDto inviter = mock(UserDataDto.class);
            when(inviter.username()).thenReturn("inviterName");
            when(inviter.email()).thenReturn("inviter@mail.com");
            UserDataDto invitee = mock(UserDataDto.class);
            when(invitee.username()).thenReturn("inviteeName");

            when(userManagerService.getUserDataWithProfileImg(INVITER_USER_ID)).thenReturn(inviter);
            when(userManagerService.getUserDataWithProfileImg(INVITEE_USER_ID)).thenReturn(invitee);

            SharedAccount savedAccount = SharedAccount.builder().id(100L).createdAt(LocalDateTime.now()).build();
            when(sharedAccountRepository.save(any(SharedAccount.class))).thenReturn(savedAccount);

            invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

            verify(sharedAccountInvitationRepository).delete(pendingInvitation);
            verify(sharedAccountMemberService).createMember(savedAccount, INVITER_USER_ID, SharedRole.OWNER);
            verify(sharedAccountMemberService).createMember(savedAccount, INVITEE_USER_ID, SharedRole.MEMBER);
        }

        @Test
        void shouldConfirmAdditionalAuthorizationCodeWhenAcceptingValidInvitation() {
            when(pendingInvitation.getInviterUserId()).thenReturn(INVITER_USER_ID);
            when(pendingInvitation.getInviteeUserId()).thenReturn(INVITEE_USER_ID);
            when(pendingInvitation.hasExpired()).thenReturn(false);
            when(sharedAccountInvitationRepository.findById(INVITATION_ID)).thenReturn(Optional.of(pendingInvitation));

            UserDataDto inviter = mock(UserDataDto.class);
            when(inviter.username()).thenReturn("inviterName");
            when(inviter.email()).thenReturn("inviter@mail.com");
            UserDataDto invitee = mock(UserDataDto.class);
            when(invitee.username()).thenReturn("inviteeName");

            when(userManagerService.getUserDataWithProfileImg(INVITER_USER_ID)).thenReturn(inviter);
            when(userManagerService.getUserDataWithProfileImg(INVITEE_USER_ID)).thenReturn(invitee);

            SharedAccount savedAccount = SharedAccount.builder().id(100L).createdAt(LocalDateTime.now()).build();
            when(sharedAccountRepository.save(any(SharedAccount.class))).thenReturn(savedAccount);

            invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

            verify(additionalAuthorizationCodeResolver).resolve(AUTHORIZATION_CODE);
            verify(additionalAuthorizationService).confirmAdditionalAuthorizationCode(INVITEE_USER_ID, resolvedAuthorizationCode);
        }

        @Test
        void shouldPublishAllOutboxEventsWhenAcceptingValidInvitation() {
            when(pendingInvitation.getInviterUserId()).thenReturn(INVITER_USER_ID);
            when(pendingInvitation.getInviteeUserId()).thenReturn(INVITEE_USER_ID);
            when(pendingInvitation.hasExpired()).thenReturn(false);
            when(sharedAccountInvitationRepository.findById(INVITATION_ID)).thenReturn(Optional.of(pendingInvitation));

            UserDataDto inviter = mock(UserDataDto.class);
            when(inviter.username()).thenReturn("inviterName");
            when(inviter.email()).thenReturn("inviter@mail.com");
            UserDataDto invitee = mock(UserDataDto.class);
            when(invitee.username()).thenReturn("inviteeName");

            when(userManagerService.getUserDataWithProfileImg(INVITER_USER_ID)).thenReturn(inviter);
            when(userManagerService.getUserDataWithProfileImg(INVITEE_USER_ID)).thenReturn(invitee);

            SharedAccount savedAccount = SharedAccount.builder().id(100L).createdAt(LocalDateTime.now()).build();
            when(sharedAccountRepository.save(any(SharedAccount.class))).thenReturn(savedAccount);

            invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

            ArgumentCaptor<UsersCreatedSharedAccountEvent> createdCaptor =
                    ArgumentCaptor.forClass(UsersCreatedSharedAccountEvent.class);
            verify(outboxService).save(eq("User"), eq(INVITER_USER_ID.toString()),
                    eq("finance.shared-account.invitation-accepted"), createdCaptor.capture());
            assertThat(createdCaptor.getValue().inviterUserId()).isEqualTo(INVITER_USER_ID);

            verify(outboxService).save(eq("User"), eq(INVITEE_USER_ID.toString()),
                    eq("finance.shared-account.create-default-settings"), any(SharedAccountCreateDefaultSettingsEvent.class));

            verify(outboxService).save(eq("User"), eq(INVITEE_USER_ID.toString()),
                    eq("activity.shared-account"), any(SharedAccountActivityEvent.class));

            ArgumentCaptor<UserAcceptSharedAccountInvitationEvent> acceptedCaptor =
                    ArgumentCaptor.forClass(UserAcceptSharedAccountInvitationEvent.class);
            verify(outboxService).save(eq("User"), eq(INVITER_USER_ID.toString()),
                    eq("notification.shared-account.invitation-accepted"), acceptedCaptor.capture());
            assertThat(acceptedCaptor.getValue().inviteeUsername()).isEqualTo("inviteeName");
        }

        @Test
        void shouldThrowExceptionWhenInvitationNotFound() {
            when(sharedAccountInvitationRepository.findById(INVITATION_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

            verify(sharedAccountRepository, never()).save(any(SharedAccount.class));
        }

        @Test
        void shouldThrowExceptionWhenInvitationOwnershipInvalid() {
            when(pendingInvitation.getInviteeUserId()).thenReturn(INVITEE_USER_ID);
            when(sharedAccountInvitationRepository.findById(INVITATION_ID)).thenReturn(Optional.of(pendingInvitation));
            doThrow(new RequestedEntityNotFoundException("Invitation ownership invalid"))
                    .when(invitationValidator).validateInvitationOwnership(INVITEE_USER_ID, INVITEE_USER_ID, INVITATION_ID);

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

            verify(sharedAccountInvitationRepository, never()).delete(any(SharedAccountInvitation.class));
        }

        @Test
        void shouldExpireInvitationAndThrowExceptionWhenInvitationExpired() {
            when(pendingInvitation.getInviteeUserId()).thenReturn(INVITEE_USER_ID);
            when(pendingInvitation.hasExpired()).thenReturn(true);
            when(sharedAccountInvitationRepository.findById(INVITATION_ID)).thenReturn(Optional.of(pendingInvitation));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

            verify(invitationExpirationService).expireInvitation(pendingInvitation);
            verify(sharedAccountRepository, never()).save(any(SharedAccount.class));
        }

        @Test
        void shouldThrowExceptionWhenAcceptValidationFails() {
            when(pendingInvitation.getInviterUserId()).thenReturn(INVITER_USER_ID);
            when(pendingInvitation.getInviteeUserId()).thenReturn(INVITEE_USER_ID);
            when(pendingInvitation.hasExpired()).thenReturn(false);
            when(sharedAccountInvitationRepository.findById(INVITATION_ID)).thenReturn(Optional.of(pendingInvitation));
            doThrow(new RequestedEntityNotFoundException("Cannot accept invitation"))
                    .when(invitationValidator).validateAcceptInvite(INVITER_USER_ID, INVITEE_USER_ID, INVITATION_ID);

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

            verify(sharedAccountInvitationRepository, never()).delete(any(SharedAccountInvitation.class));
            verify(outboxService, never()).save(anyString(), anyString(), anyString(), any());
        }
    }

    @Nested
    class RejectInvite {

        @Test
        void shouldDeleteInvitationWhenRejectingValidInvitation() {
            when(pendingInvitation.getInviterUserId()).thenReturn(INVITER_USER_ID);
            when(pendingInvitation.getInviteeUserId()).thenReturn(INVITEE_USER_ID);
            when(pendingInvitation.hasExpired()).thenReturn(false);
            when(sharedAccountInvitationRepository.findById(INVITATION_ID)).thenReturn(Optional.of(pendingInvitation));

            UserDataDto invitee = mock(UserDataDto.class);
            when(invitee.username()).thenReturn("inviteeName");
            UserDataDto inviter = mock(UserDataDto.class);
            when(inviter.username()).thenReturn("inviterName");
            when(inviter.email()).thenReturn("inviter@mail.com");

            when(userManagerService.getUserDataWithProfileImg(INVITEE_USER_ID)).thenReturn(invitee);
            when(userManagerService.getUserDataWithProfileImg(INVITER_USER_ID)).thenReturn(inviter);

            invitationResponseService.rejectInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

            verify(sharedAccountInvitationRepository).delete(pendingInvitation);
        }

        @Test
        void shouldConfirmAdditionalAuthorizationCodeWhenRejectingValidInvitation() {
            when(pendingInvitation.getInviterUserId()).thenReturn(INVITER_USER_ID);
            when(pendingInvitation.getInviteeUserId()).thenReturn(INVITEE_USER_ID);
            when(pendingInvitation.hasExpired()).thenReturn(false);
            when(sharedAccountInvitationRepository.findById(INVITATION_ID)).thenReturn(Optional.of(pendingInvitation));

            UserDataDto invitee = mock(UserDataDto.class);
            when(invitee.username()).thenReturn("inviteeName");
            UserDataDto inviter = mock(UserDataDto.class);
            when(inviter.username()).thenReturn("inviterName");
            when(inviter.email()).thenReturn("inviter@mail.com");

            when(userManagerService.getUserDataWithProfileImg(INVITEE_USER_ID)).thenReturn(invitee);
            when(userManagerService.getUserDataWithProfileImg(INVITER_USER_ID)).thenReturn(inviter);

            invitationResponseService.rejectInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

            verify(additionalAuthorizationCodeResolver).resolve(AUTHORIZATION_CODE);
            verify(additionalAuthorizationService).confirmAdditionalAuthorizationCode(INVITEE_USER_ID, resolvedAuthorizationCode);
        }

        @Test
        void shouldPublishRejectionEventsWhenRejectingValidInvitation() {
            when(pendingInvitation.getInviterUserId()).thenReturn(INVITER_USER_ID);
            when(pendingInvitation.getInviteeUserId()).thenReturn(INVITEE_USER_ID);
            when(pendingInvitation.hasExpired()).thenReturn(false);
            when(sharedAccountInvitationRepository.findById(INVITATION_ID)).thenReturn(Optional.of(pendingInvitation));

            UserDataDto invitee = mock(UserDataDto.class);
            when(invitee.username()).thenReturn("inviteeName");
            UserDataDto inviter = mock(UserDataDto.class);
            when(inviter.username()).thenReturn("inviterName");
            when(inviter.email()).thenReturn("inviter@mail.com");

            when(userManagerService.getUserDataWithProfileImg(INVITEE_USER_ID)).thenReturn(invitee);
            when(userManagerService.getUserDataWithProfileImg(INVITER_USER_ID)).thenReturn(inviter);

            invitationResponseService.rejectInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

            ArgumentCaptor<UserRejectSharedAccountInvitationEvent> rejectCaptor =
                    ArgumentCaptor.forClass(UserRejectSharedAccountInvitationEvent.class);
            verify(outboxService).save(eq("User"), eq(INVITER_USER_ID.toString()),
                    eq("user.shared-account.reject-invitation"), rejectCaptor.capture());
            assertThat(rejectCaptor.getValue().inviteeUsername()).isEqualTo("inviteeName");

            verify(outboxService).save(eq("User"), eq(INVITEE_USER_ID.toString()),
                    eq("activity.shared-account"), any(SharedAccountActivityEvent.class));
        }

        @Test
        void shouldThrowExceptionWhenInvitationToRejectNotFound() {
            when(sharedAccountInvitationRepository.findById(INVITATION_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationResponseService.rejectInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

            verify(sharedAccountInvitationRepository, never()).delete(any(SharedAccountInvitation.class));
        }

        @Test
        void shouldExpireInvitationAndThrowExceptionWhenRejectingExpiredInvitation() {
            when(pendingInvitation.getInviteeUserId()).thenReturn(INVITEE_USER_ID);
            when(pendingInvitation.hasExpired()).thenReturn(true);
            when(sharedAccountInvitationRepository.findById(INVITATION_ID)).thenReturn(Optional.of(pendingInvitation));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationResponseService.rejectInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

            verify(invitationExpirationService).expireInvitation(pendingInvitation);
            verify(outboxService, never()).save(anyString(), anyString(), anyString(), any());
        }

        @Test
        void shouldThrowExceptionWhenInvitationOwnershipInvalidOnReject() {
            when(pendingInvitation.getInviteeUserId()).thenReturn(INVITEE_USER_ID);
            when(sharedAccountInvitationRepository.findById(INVITATION_ID)).thenReturn(Optional.of(pendingInvitation));
            doThrow(new RequestedEntityNotFoundException("Invitation ownership invalid"))
                    .when(invitationValidator).validateInvitationOwnership(INVITEE_USER_ID, INVITEE_USER_ID, INVITATION_ID);

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationResponseService.rejectInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

            verify(sharedAccountInvitationRepository, never()).delete(any(SharedAccountInvitation.class));
        }
    }
}