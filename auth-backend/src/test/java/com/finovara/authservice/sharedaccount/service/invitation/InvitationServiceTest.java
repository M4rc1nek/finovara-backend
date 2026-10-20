package com.finovara.authservice.sharedaccount.service.invitation;

import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import com.finovara.authservice.sharedaccount.dto.InvitationResponse;
import com.finovara.authservice.sharedaccount.dto.SharedAccountStatusDto;
import com.finovara.authservice.sharedaccount.model.SharedAccount;
import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import com.finovara.authservice.sharedaccount.model.SharedAccountMember;
import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.mainaccount.activity.event.sharedaccount.SharedAccountChangeHistoryActivityEvent;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.event.invitation.UserSentSharedAccountInvitationEvent;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.user.authorization.dto.ConfirmAuthorizationCodeDto;
import com.finovara.contracts.user.authorization.dto.UserDataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

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

    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @InjectMocks
    private InvitationService invitationService;

    private static final Long INVITER_USER_ID = 1L;
    private static final Long INVITEE_USER_ID = 2L;
    private static final int PAGE_SIZE = 10;
    private static final int MIN_SEARCH_QUERY_LENGTH = 3;
    private static final int EXPIRATION_HOURS = 48;
    private static final String AUTHORIZATION_CODE = "auth-code";
    private static final String INVITER_USERNAME = "inviterName";
    private static final String INVITER_EMAIL = "inviter@mail.com";
    private static final String INVITEE_USERNAME = "inviteeName";
    private static final String INVITEE_EMAIL = "invitee@mail.com";
    private static final String PROFILE_IMAGE_PATH = "images/profile.png";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(invitationService, "pageSize", PAGE_SIZE);
        ReflectionTestUtils.setField(invitationService, "minSearchQueryLength", MIN_SEARCH_QUERY_LENGTH);
        ReflectionTestUtils.setField(invitationService, "invitationExpirationHours", EXPIRATION_HOURS);
    }

    @Nested
    class SearchUser {

        private UserDataDto currentUser;
        private UserDataDto otherUser;

        @BeforeEach
        void setUp() {
            currentUser = new UserDataDto(INVITER_USER_ID, INVITER_USERNAME, INVITER_EMAIL, null);
            otherUser = new UserDataDto(INVITEE_USER_ID, INVITEE_USERNAME, INVITEE_EMAIL, PROFILE_IMAGE_PATH);
        }

        @Test
        void shouldReturnEmptyListWhenQueryIsNull() {
            List<UserDataDto> result = invitationService.searchUser(null, INVITER_USER_ID);

            assertThat(result).isEmpty();
            verifyNoInteractions(userRepository);
        }

        @Test
        void shouldReturnEmptyListWhenQueryIsShorterThanMinimumLength() {
            List<UserDataDto> result = invitationService.searchUser("ab", INVITER_USER_ID);

            assertThat(result).isEmpty();
            verifyNoInteractions(userRepository);
        }

        @Test
        void shouldReturnEmptyListWhenQueryIsBlank() {
            List<UserDataDto> result = invitationService.searchUser("     ", INVITER_USER_ID);

            assertThat(result).isEmpty();
            verifyNoInteractions(userRepository);
        }

        @Test
        void shouldReturnEmptyListWhenTrimmedQueryIsShorterThanMinimumLength() {
            List<UserDataDto> result = invitationService.searchUser("  ab  ", INVITER_USER_ID);

            assertThat(result).isEmpty();
            verifyNoInteractions(userRepository);
        }

        @Test
        void shouldSearchWhenQueryLengthEqualsMinimumLength() {
            when(userRepository.searchByUsernameOrEmail(eq("abc"), any(PageRequest.class))).thenReturn(List.of(otherUser));

            List<UserDataDto> result = invitationService.searchUser("abc", INVITER_USER_ID);

            assertThat(result).containsExactly(otherUser);
        }

        @Test
        void shouldSearchUsingTrimmedQueryWhenQueryContainsWhitespaces() {
            when(userRepository.searchByUsernameOrEmail(eq("john"), any(PageRequest.class))).thenReturn(List.of(otherUser));

            invitationService.searchUser("  john  ", INVITER_USER_ID);

            verify(userRepository).searchByUsernameOrEmail(eq("john"), any(PageRequest.class));
        }

        @Test
        void shouldReturnMatchingUsersExcludingCurrentUserWhenSearching() {
            when(userRepository.searchByUsernameOrEmail(eq("john"), any(PageRequest.class))).thenReturn(List.of(currentUser, otherUser));

            List<UserDataDto> result = invitationService.searchUser("john", INVITER_USER_ID);

            assertThat(result).containsExactly(otherUser);
        }

        @Test
        void shouldReturnEmptyListWhenOnlyCurrentUserMatches() {
            when(userRepository.searchByUsernameOrEmail(eq("john"), any(PageRequest.class))).thenReturn(List.of(currentUser));

            List<UserDataDto> result = invitationService.searchUser("john", INVITER_USER_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyListWhenNoUsersMatchQuery() {
            when(userRepository.searchByUsernameOrEmail(eq("unknown"), any(PageRequest.class))).thenReturn(List.of());

            List<UserDataDto> result = invitationService.searchUser("unknown", INVITER_USER_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldMapAllUserFieldsWhenReturningFoundUsers() {
            when(userRepository.searchByUsernameOrEmail(eq("invitee"), any(PageRequest.class))).thenReturn(List.of(otherUser));

            List<UserDataDto> result = invitationService.searchUser("invitee", INVITER_USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(INVITEE_USER_ID);
            assertThat(result.get(0).username()).isEqualTo(INVITEE_USERNAME);
            assertThat(result.get(0).email()).isEqualTo(INVITEE_EMAIL);
            assertThat(result.get(0).profileImagePath()).isEqualTo(PROFILE_IMAGE_PATH);
        }

        @Test
        void shouldReturnAllMatchingUsersWhenCurrentUserIsNotAmongThem() {
            UserDataDto thirdUser = new UserDataDto(3L, "third", "third@mail.com", null);
            when(userRepository.searchByUsernameOrEmail(eq("mail"), any(PageRequest.class))).thenReturn(List.of(otherUser, thirdUser));

            List<UserDataDto> result = invitationService.searchUser("mail", INVITER_USER_ID);

            assertThat(result).containsExactly(otherUser, thirdUser);
        }

        @Test
        void shouldUseConfiguredPageSizeWhenSearching() {
            when(userRepository.searchByUsernameOrEmail(eq("john"), any(PageRequest.class))).thenReturn(List.of());

            invitationService.searchUser("john", INVITER_USER_ID);

            ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
            verify(userRepository).searchByUsernameOrEmail(eq("john"), pageRequestCaptor.capture());
            assertThat(pageRequestCaptor.getValue().getPageSize()).isEqualTo(PAGE_SIZE);
        }

        @Test
        void shouldRequestFirstPageWhenSearching() {
            when(userRepository.searchByUsernameOrEmail(eq("john"), any(PageRequest.class))).thenReturn(List.of());

            invitationService.searchUser("john", INVITER_USER_ID);

            ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
            verify(userRepository).searchByUsernameOrEmail(eq("john"), pageRequestCaptor.capture());
            assertThat(pageRequestCaptor.getValue().getPageNumber()).isZero();
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

        @Test
        void shouldQueryRepositoryWithGivenUserIdWhenCheckingSharedAccount() {
            when(sharedAccountMemberRepository.findByUserId(INVITEE_USER_ID)).thenReturn(Optional.empty());

            invitationService.hasSharedAccount(INVITEE_USER_ID);

            verify(sharedAccountMemberRepository).findByUserId(INVITEE_USER_ID);
        }
    }

    @Nested
    class SendInvitation {

        private ConfirmAuthorizationCodeDto confirmAuthorizationCodeDto;
        private UserDataDto inviter;
        private UserDataDto invitee;

        @BeforeEach
        void setUp() {
            confirmAuthorizationCodeDto = new ConfirmAuthorizationCodeDto(AUTHORIZATION_CODE);
            inviter = new UserDataDto(INVITER_USER_ID, INVITER_USERNAME, INVITER_EMAIL, null);
            invitee = new UserDataDto(INVITEE_USER_ID, INVITEE_USERNAME, INVITEE_EMAIL, null);
            when(additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE)).thenReturn(confirmAuthorizationCodeDto);
        }

        @Test
        void shouldThrowExceptionWhenAdditionalAuthorizationFails() {
            doThrow(new IllegalArgumentException("Invalid authorization code")).when(additionalAuthorizationService).confirmAdditionalAuthorizationCode(INVITER_USER_ID, confirmAuthorizationCodeDto);

            assertThrows(IllegalArgumentException.class, () -> invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE));

            verifyNoInteractions(invitationValidator);
            verifyNoInteractions(userRepository);
            verifyNoInteractions(sharedAccountInvitationRepository);
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenSendInvitationValidationFails() {
            doThrow(new RequestedEntityNotFoundException("Cannot send invitation")).when(invitationValidator).validateSendInvitation(INVITER_USER_ID, INVITEE_USER_ID);

            assertThrows(RequestedEntityNotFoundException.class, () -> invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE));

            verify(additionalAuthorizationService).confirmAdditionalAuthorizationCode(INVITER_USER_ID, confirmAuthorizationCodeDto);
            verifyNoInteractions(userRepository);
            verify(sharedAccountInvitationRepository, never()).save(any(SharedAccountInvitation.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenInviterNotFound() {
            when(userRepository.findBasicInfoByIds(List.of(INVITER_USER_ID, INVITEE_USER_ID))).thenReturn(List.of(invitee));

            assertThrows(RequestedEntityNotFoundException.class, () -> invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE));

            verify(sharedAccountInvitationRepository, never()).save(any(SharedAccountInvitation.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenInviteeNotFound() {
            when(userRepository.findBasicInfoByIds(List.of(INVITER_USER_ID, INVITEE_USER_ID))).thenReturn(List.of(inviter));

            assertThrows(RequestedEntityNotFoundException.class, () -> invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE));

            verify(sharedAccountInvitationRepository, never()).save(any(SharedAccountInvitation.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenNoUsersFound() {
            when(userRepository.findBasicInfoByIds(List.of(INVITER_USER_ID, INVITEE_USER_ID))).thenReturn(List.of());

            assertThrows(RequestedEntityNotFoundException.class, () -> invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE));

            verify(sharedAccountInvitationRepository, never()).save(any(SharedAccountInvitation.class));
            verifyNoInteractions(outboxService);
        }

        @Nested
        class WhenBothUsersExist {

            @BeforeEach
            void setUp() {
                when(userRepository.findBasicInfoByIds(List.of(INVITER_USER_ID, INVITEE_USER_ID))).thenReturn(List.of(inviter, invitee));
            }

            @Test
            void shouldConfirmAdditionalAuthorizationCodeWhenSendingInvitation() {
                invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE);

                verify(additionalAuthorizationService).confirmAdditionalAuthorizationCode(INVITER_USER_ID, confirmAuthorizationCodeDto);
            }

            @Test
            void shouldValidateInvitationWhenSendingInvitation() {
                invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE);

                verify(invitationValidator).validateSendInvitation(INVITER_USER_ID, INVITEE_USER_ID);
            }

            @Test
            void shouldAuthorizeAndValidateBeforeSavingWhenSendingInvitation() {
                invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE);

                InOrder inOrder = inOrder(additionalAuthorizationService, invitationValidator, sharedAccountInvitationRepository, outboxService);
                inOrder.verify(additionalAuthorizationService).confirmAdditionalAuthorizationCode(INVITER_USER_ID, confirmAuthorizationCodeDto);
                inOrder.verify(invitationValidator).validateSendInvitation(INVITER_USER_ID, INVITEE_USER_ID);
                inOrder.verify(sharedAccountInvitationRepository).save(any(SharedAccountInvitation.class));
                inOrder.verify(outboxService, org.mockito.Mockito.times(2)).save(any(String.class), any(String.class), any(String.class), any(Object.class));
            }

            @Test
            void shouldSaveInvitationWhenSendingValidInvitation() {
                invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE);

                verify(sharedAccountInvitationRepository).save(any(SharedAccountInvitation.class));
            }

            @Test
            void shouldSaveInvitationWithInviterAndInviteeIdsWhenSendingValidInvitation() {
                invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE);

                ArgumentCaptor<SharedAccountInvitation> captor = ArgumentCaptor.forClass(SharedAccountInvitation.class);
                verify(sharedAccountInvitationRepository).save(captor.capture());
                assertThat(captor.getValue().getInviterUserId()).isEqualTo(INVITER_USER_ID);
                assertThat(captor.getValue().getInviteeUserId()).isEqualTo(INVITEE_USER_ID);
            }

            @Test
            void shouldSetExpiresAtBasedOnConfiguredExpirationHoursWhenSavingInvitation() {
                invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE);

                ArgumentCaptor<SharedAccountInvitation> captor = ArgumentCaptor.forClass(SharedAccountInvitation.class);
                verify(sharedAccountInvitationRepository).save(captor.capture());
                assertThat(captor.getValue().getExpiresAt()).isEqualTo(captor.getValue().getCreatedAt().plusHours(EXPIRATION_HOURS));
            }

            @Test
            void shouldSetCreatedAtToCurrentTimeWhenSavingInvitation() {
                LocalDateTime before = LocalDateTime.now();

                invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE);

                LocalDateTime after = LocalDateTime.now();
                ArgumentCaptor<SharedAccountInvitation> captor = ArgumentCaptor.forClass(SharedAccountInvitation.class);
                verify(sharedAccountInvitationRepository).save(captor.capture());
                assertThat(captor.getValue().getCreatedAt()).isBetween(before, after);
            }

            @Test
            void shouldUseUpdatedExpirationHoursWhenConfigurationChanges() {
                ReflectionTestUtils.setField(invitationService, "invitationExpirationHours", 1);

                invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE);

                ArgumentCaptor<SharedAccountInvitation> captor = ArgumentCaptor.forClass(SharedAccountInvitation.class);
                verify(sharedAccountInvitationRepository).save(captor.capture());
                assertThat(captor.getValue().getExpiresAt()).isEqualTo(captor.getValue().getCreatedAt().plusHours(1));
            }

            @Test
            void shouldPublishActivityEventWhenSendingValidInvitation() {
                invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE);

                verify(outboxService).save(eq("User"), eq(INVITER_USER_ID.toString()), eq("shared-account.changed"), any(SharedAccountChangeHistoryActivityEvent.class));
            }

            @Test
            void shouldPublishNotificationEventWhenSendingValidInvitation() {
                invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE);

                verify(outboxService).save(eq("User"), eq(INVITER_USER_ID.toString()), eq("notification.shared-account.invitation-sent"), eq(new UserSentSharedAccountInvitationEvent(INVITEE_USER_ID, INVITER_USERNAME)));
            }

            @Test
            void shouldPublishNotificationEventAddressedToInviteeWhenSendingValidInvitation() {
                invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE);

                ArgumentCaptor<UserSentSharedAccountInvitationEvent> eventCaptor = ArgumentCaptor.forClass(UserSentSharedAccountInvitationEvent.class);
                verify(outboxService).save(eq("User"), eq(INVITER_USER_ID.toString()), eq("notification.shared-account.invitation-sent"), eventCaptor.capture());
                assertThat(eventCaptor.getValue().userId()).isEqualTo(INVITEE_USER_ID);
            }

            @Test
            void shouldPublishExactlyTwoEventsWhenSendingValidInvitation() {
                invitationService.sendInvitation(INVITER_USER_ID, INVITEE_USER_ID, AUTHORIZATION_CODE);

                verify(outboxService, org.mockito.Mockito.times(2)).save(any(String.class), any(String.class), any(String.class), any(Object.class));
                verifyNoMoreInteractions(outboxService);
            }
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
        void shouldReturnAllPendingInvitationsWhenUserHasMultipleInvitations() {
            InvitationResponse first = mock(InvitationResponse.class);
            InvitationResponse second = mock(InvitationResponse.class);
            when(sharedAccountInvitationRepository.findInvitationWithInviterUsername(INVITEE_USER_ID)).thenReturn(List.of(first, second));

            List<InvitationResponse> result = invitationService.getPendingInvitations(INVITEE_USER_ID);

            assertThat(result).containsExactly(first, second);
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoPendingInvitations() {
            when(sharedAccountInvitationRepository.findInvitationWithInviterUsername(INVITEE_USER_ID)).thenReturn(List.of());

            List<InvitationResponse> result = invitationService.getPendingInvitations(INVITEE_USER_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldQueryRepositoryWithGivenUserIdWhenGettingPendingInvitations() {
            when(sharedAccountInvitationRepository.findInvitationWithInviterUsername(INVITER_USER_ID)).thenReturn(List.of());

            invitationService.getPendingInvitations(INVITER_USER_ID);

            verify(sharedAccountInvitationRepository).findInvitationWithInviterUsername(INVITER_USER_ID);
        }
    }
}