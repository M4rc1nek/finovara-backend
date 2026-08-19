package com.finovara.authservice.sharedaccount.service.invitation;

import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import com.finovara.authservice.sharedaccount.dto.InvitationResponse;
import com.finovara.authservice.sharedaccount.dto.SharedAccountStatusDto;
import com.finovara.authservice.sharedaccount.model.SharedAccount;
import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import com.finovara.authservice.sharedaccount.model.SharedAccountMember;
import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.authservice.user.dto.UserDataDto;
import com.finovara.authservice.user.mapper.UserDataMapper;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.authorization.dto.ConfirmAuthorizationCodeDto;
import com.finovara.contracts.activity.event.sharedaccount.SharedAccountActivityEvent;
import com.finovara.contracts.notification.event.sharedaccount.invitation.UserSentSharedAccountInvitationEvent;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    private SharedAccountInvitationRepository sharedAccountInvitationRepository;

    @Mock
    private InvitationValidator invitationValidator;

    @Mock
    private AdditionalAuthorizationService additionalAuthorizationService;

    private InvitationService invitationService;

    private static final Long INVITER_USER_ID = 1L;
    private static final Long INVITEE_USER_ID = 2L;
    private static final int PAGE_SIZE = 10;
    private static final int EXPIRATION_HOURS = 48;
    private static final String AUTHORIZATION_CODE = "auth-code";

    @BeforeEach
    void setUp() {
        invitationService = new InvitationService(userDataMapper, sharedAccountMemberRepository, outboxService, userRepository, sharedAccountInvitationRepository, invitationValidator, additionalAuthorizationService, new AdditionalAuthorizationCodeResolver());

        ReflectionTestUtils.setField(invitationService, "pageSize", PAGE_SIZE);
        ReflectionTestUtils.setField(invitationService, "invitationExpirationHours", EXPIRATION_HOURS);
    }

    @Nested
    class SearchUser {

        @Test
        void shouldReturnMatchingUsersExcludingCurrentUserWhenSearching() {
            UserDataDto currentUser = mock(UserDataDto.class);
            when(currentUser.id()).thenReturn(INVITER_USER_ID);
            UserDataDto otherUser = mock(UserDataDto.class);
            when(otherUser.id()).thenReturn(INVITEE_USER_ID);

            when(userRepository.searchByUsernameOrEmail(eq("john"), any(PageRequest.class))).thenReturn(List.of(currentUser, otherUser));
            when(userDataMapper.mapToUserData(otherUser)).thenReturn(otherUser);

            List<UserDataDto> result = invitationService.searchUser("john", INVITER_USER_ID);

            assertThat(result).containsExactly(otherUser);
        }

        @Test
        void shouldReturnEmptyListWhenNoUsersMatchQuery() {
            when(userRepository.searchByUsernameOrEmail(eq("unknown"), any(PageRequest.class))).thenReturn(List.of());

            List<UserDataDto> result = invitationService.searchUser("unknown", INVITER_USER_ID);

            assertThat(result).isEmpty();
            verifyNoInteractions(userDataMapper);
        }

        @Test
        void shouldReturnEmptyListWhenOnlyCurrentUserMatches() {
            UserDataDto currentUser = mock(UserDataDto.class);
            when(currentUser.id()).thenReturn(INVITER_USER_ID);

            when(userRepository.searchByUsernameOrEmail(eq("me"), any(PageRequest.class))).thenReturn(List.of(currentUser));

            List<UserDataDto> result = invitationService.searchUser("me", INVITER_USER_ID);

            assertThat(result).isEmpty();
            verifyNoInteractions(userDataMapper);
        }

        @Test
        void shouldUseConfiguredPageSizeWhenSearching() {
            when(userRepository.searchByUsernameOrEmail(eq("john"), any(PageRequest.class))).thenReturn(List.of());

            invitationService.searchUser("john", INVITER_USER_ID);

            ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
            verify(userRepository).searchByUsernameOrEmail(eq("john"), pageRequestCaptor.capture());
            assertThat(pageRequestCaptor.getValue().getPageSize()).isEqualTo(PAGE_SIZE);
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
            when(sharedAccountMemberRepository.findByUserId(INVITER_USER_ID)).thenReturn(Optional.of(member));

            SharedAccountStatusDto result = invitationService.hasSharedAccount(INVITER_USER_ID);

            assertThat(result.hasSharedAccount()).isTrue();
            assertThat(result.accountId()).isEqualTo(50L);
        }

        @Test
        void shouldReturnFalseWithNullAccountIdWhenUserHasNoSharedAccount() {
            when(sharedAccountMemberRepository.findByUserId(INVITER_USER_ID)).thenReturn(Optional.empty());

            SharedAccountStatusDto result = invitationService.hasSharedAccount(INVITER_USER_ID);

            assertThat(result.hasSharedAccount()).isFalse();
            assertThat(result.accountId()).isNull();
        }
    }

    @Nested
    class SendInvitation {

        private UserDataDto inviter;
        private UserDataDto invitee;

        @BeforeEach
        void setUp() {
            inviter = mock(UserDataDto.class);
            invitee = mock(UserDataDto.class);
        }

        @Test
        void shouldConfirmAdditionalAuthorizationCodeWhenSendingInvitation() {
            when(inviter.id()).thenReturn(INVITER_USER_ID);
            when(invitee.id()).thenReturn(INVITEE_USER_ID);
            when(invitee.username()).thenReturn("inviteeName");
            when(invitee.email()).thenReturn("invitee@mail.com");
            when(inviter.username()).thenReturn("inviterName");
            when(userRepository.findBasicInfoByIds(List.of(INVITER_USER_ID, INVITEE_USER_ID))).thenReturn(List.of(inviter, invitee));

            invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE);

            verify(additionalAuthorizationService).confirmAdditionalAuthorizationCode(eq(INVITER_USER_ID), eq(new ConfirmAuthorizationCodeDto(AUTHORIZATION_CODE)));
        }

        @Test
        void shouldSaveInvitationWhenSendingValidInvitation() {
            when(inviter.id()).thenReturn(INVITER_USER_ID);
            when(inviter.username()).thenReturn("inviterName");
            when(invitee.id()).thenReturn(INVITEE_USER_ID);
            when(invitee.username()).thenReturn("inviteeName");
            when(invitee.email()).thenReturn("invitee@mail.com");

            when(userRepository.findBasicInfoByIds(List.of(INVITER_USER_ID, INVITEE_USER_ID))).thenReturn(List.of(inviter, invitee));

            invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE);

            verify(sharedAccountInvitationRepository).save(any(SharedAccountInvitation.class));
        }

        @Test
        void shouldSetExpiresAtBasedOnConfiguredExpirationHoursWhenSavingInvitation() {
            when(inviter.id()).thenReturn(INVITER_USER_ID);
            when(inviter.username()).thenReturn("inviterName");
            when(invitee.id()).thenReturn(INVITEE_USER_ID);
            when(invitee.username()).thenReturn("inviteeName");
            when(invitee.email()).thenReturn("invitee@mail.com");

            when(userRepository.findBasicInfoByIds(List.of(INVITER_USER_ID, INVITEE_USER_ID))).thenReturn(List.of(inviter, invitee));

            invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE);

            ArgumentCaptor<SharedAccountInvitation> captor = ArgumentCaptor.forClass(SharedAccountInvitation.class);
            verify(sharedAccountInvitationRepository).save(captor.capture());
            assertThat(captor.getValue().getExpiresAt()).isAfter(captor.getValue().getCreatedAt().plusHours(EXPIRATION_HOURS).minusMinutes(1));
        }

        @Test
        void shouldPublishActivityAndNotificationEventsWhenSendingValidInvitation() {
            when(inviter.id()).thenReturn(INVITER_USER_ID);
            when(inviter.username()).thenReturn("inviterName");
            when(invitee.id()).thenReturn(INVITEE_USER_ID);
            when(invitee.username()).thenReturn("inviteeName");
            when(invitee.email()).thenReturn("invitee@mail.com");

            when(userRepository.findBasicInfoByIds(List.of(INVITER_USER_ID, INVITEE_USER_ID))).thenReturn(List.of(inviter, invitee));

            invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE);

            verify(outboxService).save(eq("User"), eq(INVITER_USER_ID.toString()), eq("activity.shared-account"), any(SharedAccountActivityEvent.class));

            ArgumentCaptor<UserSentSharedAccountInvitationEvent> eventCaptor = ArgumentCaptor.forClass(UserSentSharedAccountInvitationEvent.class);
            verify(outboxService).save(eq("User"), eq(INVITER_USER_ID.toString()), eq("notification.shared-account.invitation-sent"), eventCaptor.capture());
            assertThat(eventCaptor.getValue().userId()).isEqualTo(INVITEE_USER_ID);
        }

        @Test
        void shouldThrowExceptionWhenInviterNotFound() {
            when(invitee.id()).thenReturn(INVITEE_USER_ID);

            when(userRepository.findBasicInfoByIds(List.of(INVITER_USER_ID, INVITEE_USER_ID))).thenReturn(List.of(invitee));

            assertThrows(RequestedEntityNotFoundException.class, () -> invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE));

            verify(sharedAccountInvitationRepository, never()).save(any(SharedAccountInvitation.class));
        }

        @Test
        void shouldThrowExceptionWhenInviteeNotFound() {
            when(inviter.id()).thenReturn(INVITER_USER_ID);

            when(userRepository.findBasicInfoByIds(List.of(INVITER_USER_ID, INVITEE_USER_ID))).thenReturn(List.of(inviter));

            assertThrows(RequestedEntityNotFoundException.class, () -> invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE));

            verify(sharedAccountInvitationRepository, never()).save(any(SharedAccountInvitation.class));
        }

        @Test
        void shouldThrowExceptionWhenSendInvitationValidationFails() {
            doThrow(new RequestedEntityNotFoundException("Cannot send invitation")).when(invitationValidator).validateSendInvitation(INVITER_USER_ID, INVITEE_USER_ID);

            assertThrows(RequestedEntityNotFoundException.class, () -> invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE));

            verify(userRepository, never()).findBasicInfoByIds(any());
            verify(sharedAccountInvitationRepository, never()).save(any(SharedAccountInvitation.class));
        }

        @Test
        void shouldNotValidateOrSaveWhenAdditionalAuthorizationFails() {
            doThrow(new IllegalArgumentException("Invalid authorization code")).when(additionalAuthorizationService).confirmAdditionalAuthorizationCode(eq(INVITER_USER_ID), any(ConfirmAuthorizationCodeDto.class));

            assertThrows(IllegalArgumentException.class, () -> invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE));

            verifyNoInteractions(invitationValidator);
            verifyNoInteractions(userRepository);
            verify(sharedAccountInvitationRepository, never()).save(any(SharedAccountInvitation.class));
        }
    }

    @Nested
    class GetPendingInvitations {

        @Test
        void shouldReturnPendingInvitationsWhenUserHasInvitations() {
            InvitationResponse response = mock(InvitationResponse.class);
            when(sharedAccountInvitationRepository.findInvitationWithInviterUsername(INVITEE_USER_ID)).thenReturn(List.of(response));

            List<InvitationResponse> result = invitationService.getPendingInvitations(INVITEE_USER_ID);

            assertThat(result).containsExactly(response);
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoPendingInvitations() {
            when(sharedAccountInvitationRepository.findInvitationWithInviterUsername(INVITEE_USER_ID)).thenReturn(List.of());

            List<InvitationResponse> result = invitationService.getPendingInvitations(INVITEE_USER_ID);

            assertThat(result).isEmpty();
        }
    }
}