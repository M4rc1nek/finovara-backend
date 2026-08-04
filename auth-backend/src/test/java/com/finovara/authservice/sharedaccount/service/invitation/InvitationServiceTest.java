package com.finovara.authservice.sharedaccount.service.invitation;

import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import com.finovara.authservice.sharedaccount.dto.InvitationResponse;
import com.finovara.authservice.sharedaccount.dto.SharedAccountStatusDto;
import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import com.finovara.authservice.sharedaccount.model.SharedAccountMember;
import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.authservice.sharedaccount.model.SharedAccount;
import com.finovara.authservice.user.dto.UserDataDto;
import com.finovara.authservice.user.mapper.UserDataMapper;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.contracts.event.activity.sharedaccount.SharedAccountActivityEvent;
import com.finovara.contracts.event.notification.sharedaccount.invitation.UserSentSharedAccountInvitationEvent;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private UserDataMapper userDataMapper;

    @Mock
    private SharedAccountMemberRepository sharedAccountMemberRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SharedAccountInvitationRepository sharedAccountInvitationRepository;

    @Mock
    private InvitationValidator invitationValidator;
    @Mock
    private AdditionalAuthorizationService additionalAuthorizationService;

    private InvitationService invitationService;

    private Long inviterUserId;

    private Long inviteeUserId;

    @BeforeEach
    void setUp() {
        invitationService = new InvitationService(userDataMapper, sharedAccountMemberRepository, outboxService,
                userRepository, sharedAccountInvitationRepository, invitationValidator, additionalAuthorizationService);

        ReflectionTestUtils.setField(invitationService, "pageSize", 10);
        ReflectionTestUtils.setField(invitationService, "invitationExpirationHours", 48);

        inviterUserId = 1L;
        inviteeUserId = 2L;
    }

    @Nested
    class SearchUser {

        @Test
        void shouldReturnMatchingUsersExcludingCurrentUser() {
            UserDataDto currentUser = mock(UserDataDto.class);
            when(currentUser.id()).thenReturn(inviterUserId);
            UserDataDto otherUser = mock(UserDataDto.class);
            when(otherUser.id()).thenReturn(inviteeUserId);

            when(userRepository.searchByUsernameOrEmail(eq("john"), any())).thenReturn(List.of(currentUser, otherUser));
            when(userDataMapper.mapToUserData(otherUser)).thenReturn(otherUser);

            List<UserDataDto> result = invitationService.searchUser("john", inviterUserId);

            assertEquals(1, result.size());
            assertEquals(otherUser, result.get(0));
        }

        @Test
        void shouldReturnEmptyListWhenNoUsersMatch() {
            when(userRepository.searchByUsernameOrEmail(eq("unknown"), any())).thenReturn(List.of());

            List<UserDataDto> result = invitationService.searchUser("unknown", inviterUserId);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class HasSharedAccount {

        @Test
        void shouldReturnTrueWithAccountIdWhenUserHasSharedAccount() {
            SharedAccountMember member = mock(SharedAccountMember.class);
            SharedAccount sharedAccount = mock(SharedAccount.class);
            when(sharedAccount.getId()).thenReturn(50L);
            when(member.getSharedAccount()).thenReturn(sharedAccount);
            when(sharedAccountMemberRepository.findByUserId(inviterUserId)).thenReturn(Optional.of(member));

            SharedAccountStatusDto result = invitationService.hasSharedAccount(inviterUserId);

            assertTrue(result.hasSharedAccount());
            assertEquals(50L, result.accountId());
        }

        @Test
        void shouldReturnFalseWithNullAccountIdWhenUserHasNoSharedAccount() {
            when(sharedAccountMemberRepository.findByUserId(inviterUserId)).thenReturn(Optional.empty());

            SharedAccountStatusDto result = invitationService.hasSharedAccount(inviterUserId);

            assertFalse(result.hasSharedAccount());
            assertEquals(null, result.accountId());
        }
    }

    @Nested
    class SendInvitation {

        @Test
        void shouldSaveInvitationWhenSendingValidInvitation() {
            UserDataDto inviter = mock(UserDataDto.class);
            when(inviter.id()).thenReturn(inviterUserId);
            when(inviter.username()).thenReturn("inviterName");
            UserDataDto invitee = mock(UserDataDto.class);
            when(invitee.id()).thenReturn(inviteeUserId);
            when(invitee.username()).thenReturn("inviteeName");
            when(invitee.email()).thenReturn("invitee@mail.com");

            when(userRepository.findBasicInfoByIds(List.of(inviterUserId, inviteeUserId)))
                    .thenReturn(List.of(inviter, invitee));

            invitationService.sendInvitation(inviterUserId, inviteeUserId, "auth");

            verify(sharedAccountInvitationRepository).save(any(SharedAccountInvitation.class));
        }

        @Test
        void shouldPublishActivityAndNotificationEventsWhenSendingValidInvitation() {
            UserDataDto inviter = mock(UserDataDto.class);
            when(inviter.id()).thenReturn(inviterUserId);
            when(inviter.username()).thenReturn("inviterName");
            UserDataDto invitee = mock(UserDataDto.class);
            when(invitee.id()).thenReturn(inviteeUserId);
            when(invitee.username()).thenReturn("inviteeName");
            when(invitee.email()).thenReturn("invitee@mail.com");

            when(userRepository.findBasicInfoByIds(List.of(inviterUserId, inviteeUserId)))
                    .thenReturn(List.of(inviter, invitee));

            invitationService.sendInvitation(inviterUserId, inviteeUserId, "auth");

            verify(outboxService).save(eq("User"), eq(inviterUserId.toString()),
                    eq("activity.shared-account"), any(SharedAccountActivityEvent.class));

            ArgumentCaptor<UserSentSharedAccountInvitationEvent> eventCaptor =
                    ArgumentCaptor.forClass(UserSentSharedAccountInvitationEvent.class);
            verify(outboxService).save(eq("User"), eq(inviterUserId.toString()),
                    eq("notification.shared-account.invitation-sent"), eventCaptor.capture());
            assertEquals(inviteeUserId, eventCaptor.getValue().userId());
        }

        @Test
        void shouldThrowExceptionWhenInviterNotFound() {
            UserDataDto invitee = mock(UserDataDto.class);
            when(invitee.id()).thenReturn(inviteeUserId);

            when(userRepository.findBasicInfoByIds(List.of(inviterUserId, inviteeUserId)))
                    .thenReturn(List.of(invitee));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationService.sendInvitation(inviterUserId, inviteeUserId, "auth"));

            verify(sharedAccountInvitationRepository, never()).save(any(SharedAccountInvitation.class));
        }

        @Test
        void shouldThrowExceptionWhenInviteeNotFound() {
            UserDataDto inviter = mock(UserDataDto.class);
            when(inviter.id()).thenReturn(inviterUserId);

            when(userRepository.findBasicInfoByIds(List.of(inviterUserId, inviteeUserId)))
                    .thenReturn(List.of(inviter));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationService.sendInvitation(inviterUserId, inviteeUserId, "auth"));

            verify(sharedAccountInvitationRepository, never()).save(any(SharedAccountInvitation.class));
        }

        @Test
        void shouldThrowExceptionWhenSendInvitationValidationFails() {
            org.mockito.Mockito.doThrow(new RequestedEntityNotFoundException("Cannot send invitation"))
                    .when(invitationValidator).validateSendInvitation(inviterUserId, inviteeUserId);

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationService.sendInvitation(inviterUserId, inviteeUserId, "auth"));

            verify(userRepository, never()).findBasicInfoByIds(any());
            verify(sharedAccountInvitationRepository, never()).save(any(SharedAccountInvitation.class));
        }
    }

    @Nested
    class GetPendingInvitations {

        @Test
        void shouldReturnPendingInvitationsWhenUserHasInvitations() {
            InvitationResponse response = mock(InvitationResponse.class);
            when(sharedAccountInvitationRepository.findInvitationWithInviterUsername(inviteeUserId))
                    .thenReturn(List.of(response));

            List<InvitationResponse> result = invitationService.getPendingInvitations(inviteeUserId);

            assertEquals(1, result.size());
            assertEquals(response, result.get(0));
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoPendingInvitations() {
            when(sharedAccountInvitationRepository.findInvitationWithInviterUsername(inviteeUserId))
                    .thenReturn(List.of());

            List<InvitationResponse> result = invitationService.getPendingInvitations(inviteeUserId);

            assertTrue(result.isEmpty());
        }
    }
}