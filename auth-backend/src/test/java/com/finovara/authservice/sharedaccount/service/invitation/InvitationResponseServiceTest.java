package com.finovara.authservice.sharedaccount.service.invitation;

import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import com.finovara.authservice.sharedaccount.model.SharedAccount;
import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountRepository;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.mainaccount.activity.event.sharedaccount.SharedAccountChangeHistoryActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.SharedAccountChangeHistoryActivityType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.SharedRole;
import com.finovara.contracts.sharedaccount.event.UsersCreatedSharedAccountEvent;
import com.finovara.contracts.sharedaccount.event.invitation.UserAcceptSharedAccountInvitationEvent;
import com.finovara.contracts.sharedaccount.event.invitation.UserRejectSharedAccountInvitationEvent;
import com.finovara.contracts.sharedaccount.event.settings.SharedAccountCreateDefaultSettingsEvent;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationResponseServiceTest {

    private static final Long INVITER_USER_ID = 1L;
    private static final Long INVITEE_USER_ID = 2L;
    private static final Long INVITATION_ID = 10L;
    private static final Long SHARED_ACCOUNT_ID = 100L;
    private static final String AUTHORIZATION_CODE = "auth";
    private static final String AGGREGATE_TYPE = "User";
    private static final String INVITER_USERNAME = "inviterName";
    private static final String INVITER_EMAIL = "inviter@mail.com";
    private static final String INVITEE_USERNAME = "inviteeName";
    private static final String INVITATION_ACCEPTED_TOPIC = "finance.shared-account.invitation-accepted";
    private static final String CREATE_DEFAULT_SETTINGS_TOPIC = "finance.shared-account.create-default-settings";
    private static final String SHARED_ACCOUNT_CHANGED_TOPIC = "shared-account.changed";
    private static final String ACCEPTED_NOTIFICATION_TOPIC = "notification.shared-account.invitation-accepted";
    private static final String REJECT_INVITATION_TOPIC = "user.shared-account.reject-invitation";

    @Mock
    private SharedAccountInvitationRepository sharedAccountInvitationRepository;

    @Mock
    private SharedAccountRepository sharedAccountRepository;

    @Mock
    private InvitationValidator invitationValidator;

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

    private SharedAccountInvitation invitation;

    private ConfirmAuthorizationCodeDto resolvedAuthorizationCode;

    private UserDataDto inviter;

    private UserDataDto invitee;

    private SharedAccount savedAccount;

    @BeforeEach
    void setUp() {
        invitation = mock(SharedAccountInvitation.class);
        resolvedAuthorizationCode = mock(ConfirmAuthorizationCodeDto.class);
        inviter = mock(UserDataDto.class);
        invitee = mock(UserDataDto.class);
        savedAccount = SharedAccount.builder().id(SHARED_ACCOUNT_ID).createdAt(LocalDateTime.now()).build();
    }

    @Nested
    class AcceptInvite {

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeCannotBeResolved() {
            when(additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE))
                    .thenThrow(new IllegalArgumentException("invalid code"));

            assertThrows(IllegalArgumentException.class,
                    () -> invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

            verifyNoInteractions(additionalAuthorizationService, sharedAccountInvitationRepository,
                    sharedAccountRepository, invitationValidator, sharedAccountMemberService,
                    userManagerService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenAdditionalAuthorizationFails() {
            when(additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE)).thenReturn(resolvedAuthorizationCode);
            doThrow(new IllegalStateException("authorization denied"))
                    .when(additionalAuthorizationService)
                    .confirmAdditionalAuthorizationCode(INVITEE_USER_ID, resolvedAuthorizationCode);

            assertThrows(IllegalStateException.class,
                    () -> invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

            verifyNoInteractions(sharedAccountInvitationRepository, sharedAccountRepository, invitationValidator,
                    sharedAccountMemberService, userManagerService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenInvitationNotFound() {
            when(additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE)).thenReturn(resolvedAuthorizationCode);
            when(sharedAccountInvitationRepository.findByIdAndInviteeUserId(INVITATION_ID, INVITEE_USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

            verify(sharedAccountInvitationRepository, never()).delete(any(SharedAccountInvitation.class));
            verifyNoInteractions(sharedAccountRepository, invitationValidator, sharedAccountMemberService,
                    userManagerService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenInvitationHasExpired() {
            when(additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE)).thenReturn(resolvedAuthorizationCode);
            when(sharedAccountInvitationRepository.findByIdAndInviteeUserId(INVITATION_ID, INVITEE_USER_ID))
                    .thenReturn(Optional.of(invitation));
            when(invitation.hasExpired()).thenReturn(true);

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

            verify(sharedAccountInvitationRepository, never()).delete(any(SharedAccountInvitation.class));
            verifyNoInteractions(sharedAccountRepository, invitationValidator, sharedAccountMemberService,
                    userManagerService, outboxService);
        }

        @Nested
        class WhenInvitationIsActive {

            @BeforeEach
            void setUp() {
                when(additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE)).thenReturn(resolvedAuthorizationCode);
                when(sharedAccountInvitationRepository.findByIdAndInviteeUserId(INVITATION_ID, INVITEE_USER_ID))
                        .thenReturn(Optional.of(invitation));
                when(invitation.hasExpired()).thenReturn(false);
                when(invitation.getInviterUserId()).thenReturn(INVITER_USER_ID);
            }

            @Test
            void shouldThrowExceptionWhenAcceptValidationFails() {
                doThrow(new IllegalStateException("cannot accept"))
                        .when(invitationValidator)
                        .validateAcceptInvite(INVITER_USER_ID, INVITEE_USER_ID, INVITATION_ID);

                assertThrows(IllegalStateException.class,
                        () -> invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

                verify(sharedAccountInvitationRepository, never()).delete(any(SharedAccountInvitation.class));
                verifyNoInteractions(userManagerService, sharedAccountRepository, sharedAccountMemberService, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenInviterDataCannotBeLoaded() {
                when(userManagerService.getUserDataWithProfileImg(INVITER_USER_ID))
                        .thenThrow(new IllegalStateException("inviter lookup failed"));

                assertThrows(IllegalStateException.class,
                        () -> invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

                verify(sharedAccountInvitationRepository, never()).delete(any(SharedAccountInvitation.class));
                verifyNoInteractions(sharedAccountRepository, sharedAccountMemberService, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenInviteeDataCannotBeLoaded() {
                when(userManagerService.getUserDataWithProfileImg(INVITER_USER_ID)).thenReturn(inviter);
                when(userManagerService.getUserDataWithProfileImg(INVITEE_USER_ID))
                        .thenThrow(new IllegalStateException("invitee lookup failed"));

                assertThrows(IllegalStateException.class,
                        () -> invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

                verify(sharedAccountInvitationRepository, never()).delete(any(SharedAccountInvitation.class));
                verifyNoInteractions(sharedAccountRepository, sharedAccountMemberService, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenDeletingInvitationFails() {
                when(userManagerService.getUserDataWithProfileImg(INVITER_USER_ID)).thenReturn(inviter);
                when(userManagerService.getUserDataWithProfileImg(INVITEE_USER_ID)).thenReturn(invitee);
                doThrow(new IllegalStateException("delete failed"))
                        .when(sharedAccountInvitationRepository)
                        .delete(invitation);

                assertThrows(IllegalStateException.class,
                        () -> invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

                verifyNoInteractions(sharedAccountRepository, sharedAccountMemberService, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenSavingSharedAccountFails() {
                when(userManagerService.getUserDataWithProfileImg(INVITER_USER_ID)).thenReturn(inviter);
                when(userManagerService.getUserDataWithProfileImg(INVITEE_USER_ID)).thenReturn(invitee);
                when(sharedAccountRepository.save(any(SharedAccount.class)))
                        .thenThrow(new IllegalStateException("save failed"));

                assertThrows(IllegalStateException.class,
                        () -> invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

                verifyNoInteractions(sharedAccountMemberService, outboxService);
            }

            @Nested
            class WhenAccountCanBeCreated {

                @BeforeEach
                void setUp() {
                    when(userManagerService.getUserDataWithProfileImg(INVITER_USER_ID)).thenReturn(inviter);
                    when(userManagerService.getUserDataWithProfileImg(INVITEE_USER_ID)).thenReturn(invitee);
                    when(sharedAccountRepository.save(any(SharedAccount.class))).thenReturn(savedAccount);
                }

                @Test
                void shouldConfirmAdditionalAuthorizationCodeWhenInvitationIsValid() {
                    invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

                    verify(additionalAuthorizationCodeResolver).resolve(AUTHORIZATION_CODE);
                    verify(additionalAuthorizationService)
                            .confirmAdditionalAuthorizationCode(INVITEE_USER_ID, resolvedAuthorizationCode);
                }

                @Test
                void shouldValidateAcceptanceWhenInvitationIsValid() {
                    invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

                    verify(invitationValidator).validateAcceptInvite(INVITER_USER_ID, INVITEE_USER_ID, INVITATION_ID);
                }

                @Test
                void shouldDeleteInvitationWhenInvitationIsValid() {
                    invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

                    verify(sharedAccountInvitationRepository).delete(invitation);
                }

                @Test
                void shouldSaveSharedAccountWithCreationTimeWhenInvitationIsValid() {
                    invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

                    ArgumentCaptor<SharedAccount> captor = ArgumentCaptor.forClass(SharedAccount.class);
                    verify(sharedAccountRepository).save(captor.capture());
                    assertThat(captor.getValue()).extracting("createdAt").isNotNull();
                }

                @Test
                void shouldCreateOwnerAndMemberWhenInvitationIsValid() {
                    invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

                    verify(sharedAccountMemberService).createMember(savedAccount, INVITER_USER_ID, SharedRole.OWNER);
                    verify(sharedAccountMemberService).createMember(savedAccount, INVITEE_USER_ID, SharedRole.MEMBER);
                }

                @Test
                void shouldProcessAcceptanceStepsInOrderWhenInvitationIsValid() {
                    invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

                    InOrder inOrder = inOrder(additionalAuthorizationService, invitationValidator,
                            sharedAccountInvitationRepository, sharedAccountRepository,
                            sharedAccountMemberService, outboxService);
                    inOrder.verify(additionalAuthorizationService)
                            .confirmAdditionalAuthorizationCode(INVITEE_USER_ID, resolvedAuthorizationCode);
                    inOrder.verify(invitationValidator)
                            .validateAcceptInvite(INVITER_USER_ID, INVITEE_USER_ID, INVITATION_ID);
                    inOrder.verify(sharedAccountInvitationRepository).delete(invitation);
                    inOrder.verify(sharedAccountRepository).save(any(SharedAccount.class));
                    inOrder.verify(sharedAccountMemberService).createMember(savedAccount, INVITER_USER_ID, SharedRole.OWNER);
                    inOrder.verify(sharedAccountMemberService).createMember(savedAccount, INVITEE_USER_ID, SharedRole.MEMBER);
                    inOrder.verify(outboxService).save(AGGREGATE_TYPE, INVITER_USER_ID.toString(),
                            INVITATION_ACCEPTED_TOPIC, new UsersCreatedSharedAccountEvent(INVITER_USER_ID, INVITEE_USER_ID));
                }

                @Test
                void shouldSaveUsersCreatedSharedAccountEventWhenInvitationIsValid() {
                    invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

                    verify(outboxService).save(AGGREGATE_TYPE, INVITER_USER_ID.toString(), INVITATION_ACCEPTED_TOPIC,
                            new UsersCreatedSharedAccountEvent(INVITER_USER_ID, INVITEE_USER_ID));
                }

                @Test
                void shouldSaveCreateDefaultSettingsEventWhenInvitationIsValid() {
                    invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

                    verify(outboxService).save(AGGREGATE_TYPE, INVITEE_USER_ID.toString(), CREATE_DEFAULT_SETTINGS_TOPIC,
                            new SharedAccountCreateDefaultSettingsEvent(INVITER_USER_ID, INVITEE_USER_ID));
                }

                @Test
                void shouldSaveHistoryEventWithInviterDataWhenInvitationIsValid() {
                    when(inviter.username()).thenReturn(INVITER_USERNAME);
                    when(inviter.email()).thenReturn(INVITER_EMAIL);

                    invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

                    ArgumentCaptor<SharedAccountChangeHistoryActivityEvent> captor =
                            ArgumentCaptor.forClass(SharedAccountChangeHistoryActivityEvent.class);
                    verify(outboxService).save(eq(AGGREGATE_TYPE), eq(INVITEE_USER_ID.toString()),
                            eq(SHARED_ACCOUNT_CHANGED_TOPIC), captor.capture());
                    assertThat(captor.getValue())
                            .usingRecursiveComparison()
                            .ignoringFieldsOfTypes(LocalDateTime.class)
                            .isEqualTo(new SharedAccountChangeHistoryActivityEvent(
                                    INVITEE_USER_ID, SharedAccountChangeHistoryActivityType.ACCEPTED_INVITATION, null,
                                    INVITER_USERNAME, INVITER_EMAIL, LocalDateTime.now()));
                }

                @Test
                void shouldNotifyInviterWithInviteeUsernameWhenInvitationIsValid() {
                    when(invitee.username()).thenReturn(INVITEE_USERNAME);

                    invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

                    verify(outboxService).save(AGGREGATE_TYPE, INVITER_USER_ID.toString(), ACCEPTED_NOTIFICATION_TOPIC,
                            new UserAcceptSharedAccountInvitationEvent(INVITER_USER_ID, INVITEE_USERNAME));
                }

                @Test
                void shouldSaveExactlyFourOutboxEventsWhenInvitationIsValid() {
                    invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

                    verify(outboxService, times(4)).save(anyString(), anyString(), anyString(), any());
                }

                @Test
                void shouldThrowExceptionWhenCreatingOwnerFails() {
                    doThrow(new IllegalStateException("owner creation failed"))
                            .when(sharedAccountMemberService)
                            .createMember(savedAccount, INVITER_USER_ID, SharedRole.OWNER);

                    assertThrows(IllegalStateException.class,
                            () -> invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

                    verify(sharedAccountMemberService, never()).createMember(savedAccount, INVITEE_USER_ID, SharedRole.MEMBER);
                    verifyNoInteractions(outboxService);
                }

                @Test
                void shouldThrowExceptionWhenSavingFirstOutboxEventFails() {
                    doThrow(new IllegalStateException("outbox failed"))
                            .when(outboxService)
                            .save(AGGREGATE_TYPE, INVITER_USER_ID.toString(), INVITATION_ACCEPTED_TOPIC,
                                    new UsersCreatedSharedAccountEvent(INVITER_USER_ID, INVITEE_USER_ID));

                    assertThrows(IllegalStateException.class,
                            () -> invitationResponseService.acceptInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

                    verify(outboxService, times(1)).save(anyString(), anyString(), anyString(), any());
                }
            }
        }
    }

    @Nested
    class RejectInvite {

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeCannotBeResolved() {
            when(additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE))
                    .thenThrow(new IllegalArgumentException("invalid code"));

            assertThrows(IllegalArgumentException.class,
                    () -> invitationResponseService.rejectInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

            verifyNoInteractions(additionalAuthorizationService, sharedAccountInvitationRepository,
                    userManagerService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenAdditionalAuthorizationFails() {
            when(additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE)).thenReturn(resolvedAuthorizationCode);
            doThrow(new IllegalStateException("authorization denied"))
                    .when(additionalAuthorizationService)
                    .confirmAdditionalAuthorizationCode(INVITEE_USER_ID, resolvedAuthorizationCode);

            assertThrows(IllegalStateException.class,
                    () -> invitationResponseService.rejectInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

            verifyNoInteractions(sharedAccountInvitationRepository, userManagerService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenInvitationNotFound() {
            when(additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE)).thenReturn(resolvedAuthorizationCode);
            when(sharedAccountInvitationRepository.findByIdAndInviteeUserId(INVITATION_ID, INVITEE_USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationResponseService.rejectInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

            verify(sharedAccountInvitationRepository, never()).delete(any(SharedAccountInvitation.class));
            verifyNoInteractions(userManagerService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenInvitationHasExpired() {
            when(additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE)).thenReturn(resolvedAuthorizationCode);
            when(sharedAccountInvitationRepository.findByIdAndInviteeUserId(INVITATION_ID, INVITEE_USER_ID))
                    .thenReturn(Optional.of(invitation));
            when(invitation.hasExpired()).thenReturn(true);

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> invitationResponseService.rejectInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

            verify(sharedAccountInvitationRepository, never()).delete(any(SharedAccountInvitation.class));
            verifyNoInteractions(userManagerService, outboxService);
        }

        @Nested
        class WhenInvitationIsActive {

            @BeforeEach
            void setUp() {
                when(additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE)).thenReturn(resolvedAuthorizationCode);
                when(sharedAccountInvitationRepository.findByIdAndInviteeUserId(INVITATION_ID, INVITEE_USER_ID))
                        .thenReturn(Optional.of(invitation));
                when(invitation.hasExpired()).thenReturn(false);
                when(invitation.getInviterUserId()).thenReturn(INVITER_USER_ID);
            }

            @Test
            void shouldThrowExceptionWhenInviteeDataCannotBeLoaded() {
                when(userManagerService.getUserDataWithProfileImg(INVITEE_USER_ID))
                        .thenThrow(new IllegalStateException("invitee lookup failed"));

                assertThrows(IllegalStateException.class,
                        () -> invitationResponseService.rejectInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

                verify(sharedAccountInvitationRepository, never()).delete(any(SharedAccountInvitation.class));
                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldThrowExceptionWhenInviterDataCannotBeLoaded() {
                when(userManagerService.getUserDataWithProfileImg(INVITEE_USER_ID)).thenReturn(invitee);
                when(userManagerService.getUserDataWithProfileImg(INVITER_USER_ID))
                        .thenThrow(new IllegalStateException("inviter lookup failed"));

                assertThrows(IllegalStateException.class,
                        () -> invitationResponseService.rejectInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

                verify(sharedAccountInvitationRepository, never()).delete(any(SharedAccountInvitation.class));
                verifyNoInteractions(outboxService);
            }

            @Nested
            class WhenUsersAreLoaded {

                @BeforeEach
                void setUp() {
                    when(userManagerService.getUserDataWithProfileImg(INVITEE_USER_ID)).thenReturn(invitee);
                    when(userManagerService.getUserDataWithProfileImg(INVITER_USER_ID)).thenReturn(inviter);
                }

                @Test
                void shouldConfirmAdditionalAuthorizationCodeWhenInvitationIsValid() {
                    invitationResponseService.rejectInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

                    verify(additionalAuthorizationCodeResolver).resolve(AUTHORIZATION_CODE);
                    verify(additionalAuthorizationService)
                            .confirmAdditionalAuthorizationCode(INVITEE_USER_ID, resolvedAuthorizationCode);
                }

                @Test
                void shouldDeleteInvitationWhenInvitationIsValid() {
                    invitationResponseService.rejectInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

                    verify(sharedAccountInvitationRepository).delete(invitation);
                }

                @Test
                void shouldNotCreateSharedAccountWhenInvitationIsRejected() {
                    invitationResponseService.rejectInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

                    verifyNoInteractions(sharedAccountRepository, sharedAccountMemberService, invitationValidator);
                }

                @Test
                void shouldProcessRejectionStepsInOrderWhenInvitationIsValid() {
                    when(invitee.username()).thenReturn(INVITEE_USERNAME);

                    invitationResponseService.rejectInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

                    InOrder inOrder = inOrder(additionalAuthorizationService, sharedAccountInvitationRepository, outboxService);
                    inOrder.verify(additionalAuthorizationService)
                            .confirmAdditionalAuthorizationCode(INVITEE_USER_ID, resolvedAuthorizationCode);
                    inOrder.verify(sharedAccountInvitationRepository).delete(invitation);
                    inOrder.verify(outboxService).save(AGGREGATE_TYPE, INVITER_USER_ID.toString(), REJECT_INVITATION_TOPIC,
                            new UserRejectSharedAccountInvitationEvent(INVITER_USER_ID, INVITEE_USERNAME));
                }

                @Test
                void shouldNotifyInviterWithInviteeUsernameWhenInvitationIsValid() {
                    when(invitee.username()).thenReturn(INVITEE_USERNAME);

                    invitationResponseService.rejectInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

                    verify(outboxService).save(AGGREGATE_TYPE, INVITER_USER_ID.toString(), REJECT_INVITATION_TOPIC,
                            new UserRejectSharedAccountInvitationEvent(INVITER_USER_ID, INVITEE_USERNAME));
                }

                @Test
                void shouldSaveHistoryEventWithInviterDataWhenInvitationIsValid() {
                    when(inviter.username()).thenReturn(INVITER_USERNAME);
                    when(inviter.email()).thenReturn(INVITER_EMAIL);

                    invitationResponseService.rejectInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

                    ArgumentCaptor<SharedAccountChangeHistoryActivityEvent> captor =
                            ArgumentCaptor.forClass(SharedAccountChangeHistoryActivityEvent.class);
                    verify(outboxService).save(eq(AGGREGATE_TYPE), eq(INVITEE_USER_ID.toString()),
                            eq(SHARED_ACCOUNT_CHANGED_TOPIC), captor.capture());
                    assertThat(captor.getValue())
                            .usingRecursiveComparison()
                            .ignoringFieldsOfTypes(LocalDateTime.class)
                            .isEqualTo(new SharedAccountChangeHistoryActivityEvent(
                                    INVITEE_USER_ID, SharedAccountChangeHistoryActivityType.REJECTED_INVITATION, null,
                                    INVITER_USERNAME, INVITER_EMAIL, LocalDateTime.now()));
                }

                @Test
                void shouldSaveExactlyTwoOutboxEventsWhenInvitationIsValid() {
                    invitationResponseService.rejectInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE);

                    verify(outboxService, times(2)).save(anyString(), anyString(), anyString(), any());
                }

                @Test
                void shouldThrowExceptionWhenDeletingInvitationFails() {
                    doThrow(new IllegalStateException("delete failed"))
                            .when(sharedAccountInvitationRepository)
                            .delete(invitation);

                    assertThrows(IllegalStateException.class,
                            () -> invitationResponseService.rejectInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

                    verifyNoInteractions(outboxService);
                }

                @Test
                void shouldThrowExceptionWhenSavingRejectionOutboxEventFails() {
                    doThrow(new IllegalStateException("outbox failed"))
                            .when(outboxService)
                            .save(eq(AGGREGATE_TYPE), eq(INVITER_USER_ID.toString()), eq(REJECT_INVITATION_TOPIC),
                                    any(UserRejectSharedAccountInvitationEvent.class));

                    assertThrows(IllegalStateException.class,
                            () -> invitationResponseService.rejectInvite(INVITEE_USER_ID, INVITATION_ID, AUTHORIZATION_CODE));

                    verify(outboxService, times(1)).save(anyString(), anyString(), anyString(), any());
                }
            }
        }
    }
}