package com.finovara.authservice.sharedaccount.service.invitation;

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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
    private SharedAccountRepository sharedAccountRepository;
    @Mock
    private SharedAccountInvitationRepository sharedAccountInvitationRepository;
    @Mock
    private InvitationValidator invitationValidator;

    @InjectMocks
    private InvitationService invitationService;

    private static final Long CURRENT_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(invitationService, "pageSize", 10);
    }

    @Nested
    class SearchUser {

        @Test
        void shouldReturnMappedUsersExcludingCurrentUser() {
            UserDataDto currentUserRaw = new UserDataDto(CURRENT_USER_ID, "me", "me@finovara.com", "raw-path");
            UserDataDto otherUserRaw = new UserDataDto(OTHER_USER_ID, "other", "other@finovara.com", "raw-path-2");
            UserDataDto otherUserMapped = new UserDataDto(OTHER_USER_ID, "other", "other@finovara.com", "/profile-images/other.png");

            when(userRepository.searchByUsernameOrEmail(eq("query"), any(PageRequest.class)))
                    .thenReturn(List.of(currentUserRaw, otherUserRaw));
            when(userDataMapper.mapToUserData(otherUserRaw)).thenReturn(otherUserMapped);

            List<UserDataDto> result = invitationService.searchUser("query", CURRENT_USER_ID);

            assertThat(result).containsExactly(otherUserMapped);
            verify(userDataMapper, never()).mapToUserData(currentUserRaw);
        }

        @Test
        void shouldReturnEmptyListWhenNoUsersMatch() {
            when(userRepository.searchByUsernameOrEmail(eq("nomatch"), any(PageRequest.class)))
                    .thenReturn(List.of());

            List<UserDataDto> result = invitationService.searchUser("nomatch", CURRENT_USER_ID);

            assertThat(result).isEmpty();
            verifyNoInteractions(userDataMapper);
        }
    }

    @Nested
    class HasSharedAccount {

        @Test
        void shouldReturnTrueWithAccountIdWhenUserIsMember() {
            SharedAccount sharedAccount = SharedAccount.builder().id(99L).build();
            SharedAccountMember member = SharedAccountMember.builder()
                    .sharedAccount(sharedAccount)
                    .userId(CURRENT_USER_ID)
                    .build();

            when(sharedAccountMemberRepository.findByUserId(CURRENT_USER_ID))
                    .thenReturn(Optional.of(member));

            SharedAccountStatusDto result = invitationService.hasSharedAccount(CURRENT_USER_ID);

            assertThat(result.hasSharedAccount()).isTrue();
            assertThat(result.accountId()).isEqualTo(99L);
        }

        @Test
        void shouldReturnFalseWithNullAccountIdWhenUserIsNotMember() {
            when(sharedAccountMemberRepository.findByUserId(CURRENT_USER_ID))
                    .thenReturn(Optional.empty());

            SharedAccountStatusDto result = invitationService.hasSharedAccount(CURRENT_USER_ID);

            assertThat(result.hasSharedAccount()).isFalse();
            assertThat(result.accountId()).isNull();
        }
    }

    @Nested
    class SendInvitation {

        @Test
        void shouldSaveInvitationAfterValidation() {

            when(userRepository.findUsernameById(CURRENT_USER_ID)).thenReturn(Optional.of("inviter-username"));

            invitationService.sendInvitation(CURRENT_USER_ID, OTHER_USER_ID);

            verify(invitationValidator).validateSendInvitation(CURRENT_USER_ID, OTHER_USER_ID);

            ArgumentCaptor<SharedAccountInvitation> captor = ArgumentCaptor.forClass(SharedAccountInvitation.class);
            verify(sharedAccountInvitationRepository).save(captor.capture());

            SharedAccountInvitation saved = captor.getValue();
            assertThat(saved.getInviterUserId()).isEqualTo(CURRENT_USER_ID);
            assertThat(saved.getInviteeUserId()).isEqualTo(OTHER_USER_ID);
            assertThat(saved.getCreatedAt()).isNotNull();

            verify(outboxService).save(eq("User"), eq(CURRENT_USER_ID.toString()),
                    eq("notification.shared-account.invitation-sent"), any());
        }

        @Test
        void shouldThrowExceptionWhenValidatorRejectsInvitation() {
            doThrow(new RuntimeException("rejected"))
                    .when(invitationValidator).validateSendInvitation(CURRENT_USER_ID, OTHER_USER_ID);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> invitationService.sendInvitation(CURRENT_USER_ID, OTHER_USER_ID));

            assertEquals("rejected", exception.getMessage());
            verify(sharedAccountInvitationRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenInviterUsernameNotFound() {
            when(userRepository.findUsernameById(CURRENT_USER_ID)).thenReturn(Optional.empty());

            RequestedEntityNotFoundException exception = assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationService.sendInvitation(CURRENT_USER_ID, OTHER_USER_ID));

            assertEquals("Invitee username not found", exception.getMessage());

            verify(invitationValidator).validateSendInvitation(CURRENT_USER_ID, OTHER_USER_ID);
            verify(sharedAccountInvitationRepository, never()).save(any());
            verifyNoInteractions(outboxService);
        }
    }
    @Nested
    class GetPendingInvitations {

        @Test
        void shouldReturnInvitationsFromRepository() {
            InvitationResponse response = new InvitationResponse(1L, OTHER_USER_ID, "inviter");
            when(sharedAccountInvitationRepository.findInvitationWithInviterUsername(CURRENT_USER_ID))
                    .thenReturn(List.of(response));

            List<InvitationResponse> result = invitationService.getPendingInvitations(CURRENT_USER_ID);

            assertThat(result).containsExactly(response);
        }
    }

    @Nested
    class AcceptInvite {

        private static final Long INVITATION_ID = 5L;

        @Test
        void shouldCreateSharedAccountWithTwoMembersAndPublishEvents() {
            SharedAccountInvitation invitation = SharedAccountInvitation.builder()
                    .id(INVITATION_ID)
                    .inviterUserId(OTHER_USER_ID)
                    .inviteeUserId(CURRENT_USER_ID)
                    .createdAt(LocalDateTime.now())
                    .build();

            SharedAccount savedAccount = SharedAccount.builder().id(42L).createdAt(LocalDateTime.now()).build();

            User inviterUser = User.builder().id(OTHER_USER_ID).build();
            User inviteeUser = User.builder().id(CURRENT_USER_ID).build();

            when(sharedAccountInvitationRepository.findInvitationForInviteeUser(INVITATION_ID))
                    .thenReturn(Optional.of(invitation));
            when(sharedAccountInvitationRepository.findInviteeUsernameByInvitationId(INVITATION_ID))
                    .thenReturn(Optional.of("invitee-username"));
            when(sharedAccountRepository.save(any(SharedAccount.class))).thenReturn(savedAccount);
            when(userRepository.getReferenceById(OTHER_USER_ID)).thenReturn(inviterUser);
            when(userRepository.getReferenceById(CURRENT_USER_ID)).thenReturn(inviteeUser);

            invitationService.acceptInvite(CURRENT_USER_ID, INVITATION_ID);

            verify(invitationValidator).validateInvitationOwnership(invitation, CURRENT_USER_ID);
            verify(invitationValidator).validateAcceptInvite(OTHER_USER_ID, CURRENT_USER_ID, INVITATION_ID);
            verify(sharedAccountInvitationRepository).delete(invitation);

            ArgumentCaptor<SharedAccountMember> memberCaptor = ArgumentCaptor.forClass(SharedAccountMember.class);
            verify(sharedAccountMemberRepository, times(2)).save(memberCaptor.capture());

            List<SharedAccountMember> savedMembers = memberCaptor.getAllValues();
            assertThat(savedMembers)
                    .extracting(SharedAccountMember::getUserId, SharedAccountMember::getRole)
                    .containsExactlyInAnyOrder(
                            tuple(OTHER_USER_ID, SharedRole.OWNER),
                            tuple(CURRENT_USER_ID, SharedRole.MEMBER));

            assertThat(inviterUser.isHasSharedAccount()).isTrue();
            assertThat(inviteeUser.isHasSharedAccount()).isTrue();

            verify(outboxService).save(eq("User"), eq(OTHER_USER_ID.toString()),
                    eq("finance.shared-account.invitation-accepted"), any());
            verify(outboxService).save(eq("User"), eq(OTHER_USER_ID.toString()),
                    eq("notification.shared-account.invitation-accepted"), any());
        }

        @Test
        void shouldThrowExceptionWhenInvitationNotFound() {
            when(sharedAccountInvitationRepository.findInvitationForInviteeUser(INVITATION_ID))
                    .thenReturn(Optional.empty());

            RequestedEntityNotFoundException exception = assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationService.acceptInvite(CURRENT_USER_ID, INVITATION_ID));

            assertEquals("Pending invitation not found", exception.getMessage());
            verifyNoInteractions(sharedAccountRepository);
        }

        @Test
        void shouldThrowExceptionWhenInviteeUsernameNotFound() {
            SharedAccountInvitation invitation = SharedAccountInvitation.builder()
                    .id(INVITATION_ID)
                    .inviterUserId(OTHER_USER_ID)
                    .inviteeUserId(CURRENT_USER_ID)
                    .build();

            when(sharedAccountInvitationRepository.findInvitationForInviteeUser(INVITATION_ID))
                    .thenReturn(Optional.of(invitation));
            when(sharedAccountInvitationRepository.findInviteeUsernameByInvitationId(INVITATION_ID))
                    .thenReturn(Optional.empty());

            RequestedEntityNotFoundException exception = assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationService.acceptInvite(CURRENT_USER_ID, INVITATION_ID));

            assertEquals("Invitee username not found", exception.getMessage());
            verifyNoInteractions(sharedAccountRepository);
        }

        @Test
        void shouldThrowExceptionWhenValidatorRejectsAccept() {
            SharedAccountInvitation invitation = SharedAccountInvitation.builder()
                    .id(INVITATION_ID)
                    .inviterUserId(OTHER_USER_ID)
                    .inviteeUserId(CURRENT_USER_ID)
                    .build();

            when(sharedAccountInvitationRepository.findInvitationForInviteeUser(INVITATION_ID))
                    .thenReturn(Optional.of(invitation));
            when(sharedAccountInvitationRepository.findInviteeUsernameByInvitationId(INVITATION_ID))
                    .thenReturn(Optional.of("invitee-username"));
            doThrow(new RuntimeException("already has account"))
                    .when(invitationValidator).validateAcceptInvite(OTHER_USER_ID, CURRENT_USER_ID, INVITATION_ID);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> invitationService.acceptInvite(CURRENT_USER_ID, INVITATION_ID));

            assertEquals("already has account", exception.getMessage());
            verify(sharedAccountInvitationRepository, never()).delete(any());
            verifyNoInteractions(sharedAccountRepository);
        }
    }

    @Nested
    class RejectInvite {

        private static final Long INVITATION_ID = 3L;

        @Test
        void shouldDeleteInvitationAndPublishRejectEvent() {
            SharedAccountInvitation invitation = SharedAccountInvitation.builder()
                    .id(INVITATION_ID)
                    .inviterUserId(OTHER_USER_ID)
                    .inviteeUserId(CURRENT_USER_ID)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(sharedAccountInvitationRepository.findInvitationForInviteeUser(INVITATION_ID))
                    .thenReturn(Optional.of(invitation));
            when(sharedAccountInvitationRepository.findInviteeUsernameByInvitationId(INVITATION_ID))
                    .thenReturn(Optional.of("invitee-username"));

            invitationService.rejectInvite(CURRENT_USER_ID, INVITATION_ID);

            verify(invitationValidator).validateInvitationOwnership(invitation, CURRENT_USER_ID);
            verify(sharedAccountInvitationRepository).delete(invitation);

            verify(outboxService).save(eq("User"), eq(OTHER_USER_ID.toString()),
                    eq("user.shared-account.reject-invitation"), any());
        }

        @Test
        void shouldThrowExceptionWhenInvitationNotFound() {
            when(sharedAccountInvitationRepository.findInvitationForInviteeUser(INVITATION_ID))
                    .thenReturn(Optional.empty());

            RequestedEntityNotFoundException exception = assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationService.rejectInvite(CURRENT_USER_ID, INVITATION_ID));

            assertEquals("Pending invitation not found", exception.getMessage());
            verify(sharedAccountInvitationRepository, never()).delete(any());
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenInviteeUsernameNotFound() {
            SharedAccountInvitation invitation = SharedAccountInvitation.builder()
                    .id(INVITATION_ID)
                    .inviterUserId(OTHER_USER_ID)
                    .inviteeUserId(CURRENT_USER_ID)
                    .build();

            when(sharedAccountInvitationRepository.findInvitationForInviteeUser(INVITATION_ID))
                    .thenReturn(Optional.of(invitation));
            when(sharedAccountInvitationRepository.findInviteeUsernameByInvitationId(INVITATION_ID))
                    .thenReturn(Optional.empty());

            RequestedEntityNotFoundException exception = assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationService.rejectInvite(CURRENT_USER_ID, INVITATION_ID));

            assertEquals("Invitee username not found", exception.getMessage());
            verify(sharedAccountInvitationRepository, never()).delete(any());
        }
    }

    @Nested
    class GetMemberDetails {

        private static final Long ACCOUNT_ID = 77L;

        @Test
        void shouldReturnMappedMembersWhenCallerIsMember() {
            SharedAccountMember memberOne = SharedAccountMember.builder().userId(CURRENT_USER_ID).role(SharedRole.OWNER).build();
            SharedAccountMember memberTwo = SharedAccountMember.builder().userId(OTHER_USER_ID).role(SharedRole.MEMBER).build();

            User userOne = User.builder().id(CURRENT_USER_ID).username("me").build();
            User userTwo = User.builder().id(OTHER_USER_ID).username("other").build();

            SharedAccountMemberDto dtoOne = new SharedAccountMemberDto(CURRENT_USER_ID, "me", "/profile-images/default/UserProf.png", SharedRole.OWNER, LocalDateTime.now());
            SharedAccountMemberDto dtoTwo = new SharedAccountMemberDto(OTHER_USER_ID, "other", "/profile-images/other.png", SharedRole.MEMBER, LocalDateTime.now());

            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID))
                    .thenReturn(List.of(memberOne, memberTwo));
            when(userRepository.getReferenceById(CURRENT_USER_ID)).thenReturn(userOne);
            when(userRepository.getReferenceById(OTHER_USER_ID)).thenReturn(userTwo);
            when(userDataMapper.toSharedAccountMemberDto(memberOne, userOne)).thenReturn(dtoOne);
            when(userDataMapper.toSharedAccountMemberDto(memberTwo, userTwo)).thenReturn(dtoTwo);

            List<SharedAccountMemberDto> result = invitationService.getMemberDetails(ACCOUNT_ID, CURRENT_USER_ID);

            assertThat(result).containsExactly(dtoOne, dtoTwo);
            verify(invitationValidator).validateMembership(ACCOUNT_ID, CURRENT_USER_ID);
        }

        @Test
        void shouldThrowExceptionWhenCallerIsNotMember() {
            doThrow(new RuntimeException("not a member"))
                    .when(invitationValidator).validateMembership(ACCOUNT_ID, CURRENT_USER_ID);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> invitationService.getMemberDetails(ACCOUNT_ID, CURRENT_USER_ID));

            assertEquals("not a member", exception.getMessage());
            verifyNoInteractions(sharedAccountMemberRepository);
        }

        @Test
        void shouldReturnEmptyListWhenAccountHasNoMembers() {
            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID))
                    .thenReturn(List.of());

            List<SharedAccountMemberDto> result = invitationService.getMemberDetails(ACCOUNT_ID, CURRENT_USER_ID);

            assertThat(result).isEmpty();
        }
    }
}