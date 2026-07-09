package com.finovara.authservice.sharedaccount.service.invitation;

import com.finovara.authservice.sharedaccount.dto.InvitationDetailsDto;
import com.finovara.authservice.sharedaccount.dto.InvitationResponse;
import com.finovara.authservice.sharedaccount.dto.SharedAccountMemberDto;
import com.finovara.authservice.sharedaccount.dto.SharedAccountStatusDto;
import com.finovara.authservice.sharedaccount.model.SharedAccount;
import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import com.finovara.authservice.sharedaccount.model.SharedAccountMember;
import com.finovara.authservice.sharedaccount.model.role.SharedRole;
import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountRepository;
import com.finovara.authservice.user.dto.UserDataDto;
import com.finovara.authservice.user.mapper.UserDataMapper;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.contracts.event.activity.sharedaccount.SharedAccountActivityEvent;
import com.finovara.contracts.event.finance.sharedaccount.UsersCreatedSharedAccountEvent;
import com.finovara.contracts.event.notification.sharedaccount.invitation.UserAcceptSharedAccountInvitationEvent;
import com.finovara.contracts.event.notification.sharedaccount.invitation.UserRejectSharedAccountInvitationEvent;
import com.finovara.contracts.event.notification.sharedaccount.invitation.UserSentSharedAccountInvitationEvent;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.exception.forbidden.AccessDeniedException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    private static final Long INVITER_ID = 1L;
    private static final Long INVITEE_ID = 2L;
    private static final Long INVITATION_ID = 100L;
    private static final Long ACCOUNT_ID = 10L;

    @Mock
    private UserDataMapper userDataMapper;
    @Mock
    private SharedAccountMemberRepository sharedAccountMemberRepository;
    @Mock
    private OutboxService outboxService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SharedAccountRepository sharedAccountRepository;
    @Mock
    private SharedAccountInvitationRepository sharedAccountInvitationRepository;
    @Mock
    private InvitationValidator invitationValidator;

    private InvitationService invitationService;

    @BeforeEach
    void setUp() {
        invitationService = new InvitationService(
                userDataMapper,
                sharedAccountMemberRepository,
                outboxService,
                userRepository,
                sharedAccountRepository,
                sharedAccountInvitationRepository,
                invitationValidator
        );
        ReflectionTestUtils.setField(invitationService, "pageSize", 10);
    }

    @Nested
    class SearchUserTests {

        @Test
        void returnsUsersExcludingCurrentUser() {
            UserDataDto self = new UserDataDto(INVITER_ID, "me", "me@test.com", null);
            UserDataDto match = new UserDataDto(5L, "john", "john@test.com", null);

            when(userRepository.searchByUsernameOrEmail(eq("john"), any(PageRequest.class)))
                    .thenReturn(List.of(self, match));
            when(userDataMapper.mapToUserData(match)).thenReturn(match);

            List<UserDataDto> result = invitationService.searchUser("john", INVITER_ID);

            assertEquals(1, result.size());
            assertEquals(match, result.get(0));
        }

        @Test
        void returnsEmptyListWhenNoMatches() {
            when(userRepository.searchByUsernameOrEmail(eq("nobody"), any(PageRequest.class)))
                    .thenReturn(List.of());

            List<UserDataDto> result = invitationService.searchUser("nobody", INVITER_ID);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class HasSharedAccountTests {

        @Test
        void returnsTrueAndAccountIdWhenMemberExists() {
            SharedAccountMember member = mock(SharedAccountMember.class);
            SharedAccount account = mock(SharedAccount.class);
            when(account.getId()).thenReturn(ACCOUNT_ID);
            when(member.getSharedAccount()).thenReturn(account);
            when(sharedAccountMemberRepository.findByUserId(INVITER_ID)).thenReturn(Optional.of(member));

            SharedAccountStatusDto result = invitationService.hasSharedAccount(INVITER_ID);

            assertTrue(result.hasSharedAccount());
            assertEquals(ACCOUNT_ID, result.accountId());
        }

        @Test
        void returnsFalseAndNullAccountIdWhenNoMembership() {
            when(sharedAccountMemberRepository.findByUserId(INVITER_ID)).thenReturn(Optional.empty());

            SharedAccountStatusDto result = invitationService.hasSharedAccount(INVITER_ID);

            assertFalse(result.hasSharedAccount());
            assertNull(result.accountId());
        }
    }

    @Nested
    class SendInvitationTests {

        @Test
        void sendsInvitationSuccessfully() {
            UserDataDto inviter = new UserDataDto(INVITER_ID, "inviterName", "inviter@test.com", null);
            UserDataDto invitee = new UserDataDto(INVITEE_ID, "inviteeName", "invitee@test.com", null);

            when(userRepository.findBasicInfoByIds(List.of(INVITER_ID, INVITEE_ID)))
                    .thenReturn(List.of(inviter, invitee));

            invitationService.sendInvitation(INVITER_ID, INVITEE_ID);

            verify(invitationValidator).validateSendInvitation(INVITER_ID, INVITEE_ID);
            verify(sharedAccountInvitationRepository).save(any(SharedAccountInvitation.class));
            verify(outboxService).save(eq("User"), eq(INVITER_ID.toString()),
                    eq("activity.shared-account"), any(SharedAccountActivityEvent.class));
            verify(outboxService).save(eq("User"), eq(INVITER_ID.toString()),
                    eq("notification.shared-account.invitation-sent"), any(UserSentSharedAccountInvitationEvent.class));
        }

        @Test
        void throwsWhenInviterNotFoundInBatchResult() {
            when(userRepository.findBasicInfoByIds(List.of(INVITER_ID, INVITEE_ID)))
                    .thenReturn(List.of(new UserDataDto(INVITEE_ID, "inviteeName", "invitee@test.com", null)));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationService.sendInvitation(INVITER_ID, INVITEE_ID));

            verify(sharedAccountInvitationRepository, never()).save(any());
        }

        @Test
        void throwsWhenInviteeNotFoundInBatchResult() {
            when(userRepository.findBasicInfoByIds(List.of(INVITER_ID, INVITEE_ID)))
                    .thenReturn(List.of(new UserDataDto(INVITER_ID, "inviterName", "inviter@test.com", null)));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationService.sendInvitation(INVITER_ID, INVITEE_ID));

            verify(sharedAccountInvitationRepository, never()).save(any());
        }

        @Test
        void propagatesExceptionFromValidator() {
            doThrow(new InvalidInputException("You cannot invite yourself"))
                    .when(invitationValidator).validateSendInvitation(INVITER_ID, INVITER_ID);

            assertThrows(InvalidInputException.class,
                    () -> invitationService.sendInvitation(INVITER_ID, INVITER_ID));

            verifyNoInteractions(userRepository);
            verify(sharedAccountInvitationRepository, never()).save(any());
        }
    }

    @Nested
    class GetPendingInvitationsTests {

        @Test
        void delegatesToRepository() {
            InvitationResponse response = new InvitationResponse(INVITATION_ID, INVITER_ID, "inviterName");
            when(sharedAccountInvitationRepository.findInvitationWithInviterUsername(INVITEE_ID))
                    .thenReturn(List.of(response));

            List<InvitationResponse> result = invitationService.getPendingInvitations(INVITEE_ID);

            assertEquals(1, result.size());
            assertEquals(response, result.get(0));
        }
    }

    @Nested
    class AcceptInviteTests {

        private InvitationDetailsDto details;

        @BeforeEach
        void setUp() {
            details = new InvitationDetailsDto(
                    INVITATION_ID, INVITER_ID, "inviterName", "inviter@test.com",
                    INVITEE_ID, "inviteeName");
        }

        @Test
        void acceptsInvitationSuccessfully() {
            when(sharedAccountInvitationRepository.findInvitationDetailsById(INVITATION_ID))
                    .thenReturn(Optional.of(details));

            SharedAccount savedAccount = SharedAccount.builder().createdAt(LocalDateTime.now()).build();
            when(sharedAccountRepository.save(any(SharedAccount.class))).thenReturn(savedAccount);

            User inviterUser = mock(User.class);
            User inviteeUser = mock(User.class);
            when(userRepository.getReferenceById(INVITER_ID)).thenReturn(inviterUser);
            when(userRepository.getReferenceById(INVITEE_ID)).thenReturn(inviteeUser);

            invitationService.acceptInvite(INVITEE_ID, INVITATION_ID);

            verify(invitationValidator).validateInvitationOwnership(INVITEE_ID, INVITEE_ID, INVITATION_ID);
            verify(invitationValidator).validateAcceptInvite(INVITER_ID, INVITEE_ID, INVITATION_ID);
            verify(sharedAccountInvitationRepository).deleteInvitationById(INVITATION_ID);
            verify(sharedAccountMemberRepository, times(2)).save(any(SharedAccountMember.class));
            verify(inviterUser).setHasSharedAccount(true);
            verify(inviteeUser).setHasSharedAccount(true);
            verify(outboxService).save(eq("User"), eq(INVITER_ID.toString()),
                    eq("finance.shared-account.invitation-accepted"), any(UsersCreatedSharedAccountEvent.class));
            verify(outboxService).save(eq("User"), eq(INVITEE_ID.toString()),
                    eq("activity.shared-account"), any(SharedAccountActivityEvent.class));
            verify(outboxService).save(eq("User"), eq(INVITER_ID.toString()),
                    eq("notification.shared-account.invitation-accepted"), any(UserAcceptSharedAccountInvitationEvent.class));
        }

        @Test
        void throwsWhenInvitationNotFound() {
            when(sharedAccountInvitationRepository.findInvitationDetailsById(INVITATION_ID))
                    .thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationService.acceptInvite(INVITEE_ID, INVITATION_ID));

            verify(sharedAccountInvitationRepository, never()).deleteInvitationById(any());
        }

        @Test
        void throwsWhenCallerIsNotTheInvitee() {
            when(sharedAccountInvitationRepository.findInvitationDetailsById(INVITATION_ID))
                    .thenReturn(Optional.of(details));

            doThrow(new AccessDeniedException("This invitation does not belong to the current user"))
                    .when(invitationValidator).validateInvitationOwnership(INVITEE_ID, 999L, INVITATION_ID);

            assertThrows(AccessDeniedException.class,
                    () -> invitationService.acceptInvite(999L, INVITATION_ID));

            verify(sharedAccountInvitationRepository, never()).deleteInvitationById(any());
        }

        @Test
        void throwsWhenSomeoneAlreadyHasSharedAccount() {
            when(sharedAccountInvitationRepository.findInvitationDetailsById(INVITATION_ID))
                    .thenReturn(Optional.of(details));

            doThrow(new EntityAlreadyExistsException("One of the users already belongs to a shared account"))
                    .when(invitationValidator).validateAcceptInvite(INVITER_ID, INVITEE_ID, INVITATION_ID);

            assertThrows(EntityAlreadyExistsException.class,
                    () -> invitationService.acceptInvite(INVITEE_ID, INVITATION_ID));

            verify(sharedAccountInvitationRepository, never()).deleteInvitationById(any());
        }
    }

    @Nested
    class RejectInviteTests {

        private InvitationDetailsDto details;

        @BeforeEach
        void setUp() {
            details = new InvitationDetailsDto(
                    INVITATION_ID, INVITER_ID, "inviterName", "inviter@test.com",
                    INVITEE_ID, "inviteeName");
        }

        @Test
        void rejectsInvitationSuccessfully() {
            when(sharedAccountInvitationRepository.findInvitationDetailsById(INVITATION_ID))
                    .thenReturn(Optional.of(details));

            invitationService.rejectInvite(INVITEE_ID, INVITATION_ID);

            verify(invitationValidator).validateInvitationOwnership(INVITEE_ID, INVITEE_ID, INVITATION_ID);
            verify(sharedAccountInvitationRepository).deleteInvitationById(INVITATION_ID);
            verify(outboxService).save(eq("User"), eq(INVITER_ID.toString()),
                    eq("user.shared-account.reject-invitation"), any(UserRejectSharedAccountInvitationEvent.class));
            verify(outboxService).save(eq("User"), eq(INVITEE_ID.toString()),
                    eq("activity.shared-account"), any(SharedAccountActivityEvent.class));
        }

        @Test
        void throwsWhenInvitationNotFound() {
            when(sharedAccountInvitationRepository.findInvitationDetailsById(INVITATION_ID))
                    .thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationService.rejectInvite(INVITEE_ID, INVITATION_ID));

            verify(sharedAccountInvitationRepository, never()).deleteInvitationById(any());
        }

        @Test
        void throwsWhenCallerIsNotTheInvitee() {
            when(sharedAccountInvitationRepository.findInvitationDetailsById(INVITATION_ID))
                    .thenReturn(Optional.of(details));

            doThrow(new AccessDeniedException("This invitation does not belong to the current user"))
                    .when(invitationValidator).validateInvitationOwnership(INVITEE_ID, 999L, INVITATION_ID);

            assertThrows(AccessDeniedException.class,
                    () -> invitationService.rejectInvite(999L, INVITATION_ID));

            verify(sharedAccountInvitationRepository, never()).deleteInvitationById(any());
            verifyNoInteractions(outboxService);
        }
    }

    @Nested
    class GetMemberDetailsTests {

        @Test
        void returnsMappedMembers() {
            SharedAccountMember member = mock(SharedAccountMember.class);
            when(member.getUserId()).thenReturn(INVITEE_ID);

            User user = mock(User.class);
            when(userRepository.getReferenceById(INVITEE_ID)).thenReturn(user);

            SharedAccountMemberDto dto = new SharedAccountMemberDto(
                    INVITEE_ID, "inviteeName", null, SharedRole.MEMBER, LocalDateTime.now());

            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID)).thenReturn(List.of(member));
            when(userDataMapper.toSharedAccountMemberDto(member, user)).thenReturn(dto);

            List<SharedAccountMemberDto> result = invitationService.getMemberDetails(ACCOUNT_ID, INVITER_ID);

            verify(invitationValidator).validateMembership(ACCOUNT_ID, INVITER_ID);
            assertEquals(1, result.size());
            assertEquals(dto, result.get(0));
        }

        @Test
        void throwsWhenCallerIsNotAMember() {
            doThrow(new AccessDeniedException("You are not a member of this shared account"))
                    .when(invitationValidator).validateMembership(ACCOUNT_ID, INVITER_ID);

            assertThrows(AccessDeniedException.class,
                    () -> invitationService.getMemberDetails(ACCOUNT_ID, INVITER_ID));

            verifyNoInteractions(sharedAccountMemberRepository);
        }
    }
}