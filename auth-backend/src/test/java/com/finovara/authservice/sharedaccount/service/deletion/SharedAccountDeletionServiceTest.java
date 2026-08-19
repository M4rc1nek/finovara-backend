package com.finovara.authservice.sharedaccount.service.deletion;

import com.finovara.authservice.sharedaccount.context.SharedAccountUsers;
import com.finovara.authservice.sharedaccount.context.UserContextLoader;
import com.finovara.authservice.sharedaccount.dto.SharedAccountDetailsDto;
import com.finovara.authservice.sharedaccount.model.SharedAccount;
import com.finovara.authservice.sharedaccount.model.SharedAccountMember;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.util.confirmationpassword.service.PasswordValidator;
import com.finovara.authservice.util.deletion.AccountRemovalTemplate;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.activity.event.sharedaccount.SharedAccountActivityEvent;
import com.finovara.contracts.notification.event.sharedaccount.deletion.NotificationSharedAccountLeftEvent;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.activity.SharedAccountActivityType;
import com.finovara.contracts.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedAccountDeletionServiceTest {

    private static final Long ACTING_USER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;
    private static final Long CO_FOUNDER_ID = 2L;
    private static final String USERNAME = "john";
    private static final String CO_FOUNDER_USERNAME = "anna";
    private static final String CO_FOUNDER_EMAIL = "anna@finovara.com";

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private PasswordValidator passwordValidator;
    @Mock
    private OutboxService outboxService;
    @Mock
    private SharedAccountMemberRepository sharedAccountMemberRepository;
    @Mock
    private AccountRemovalTemplate accountRemovalTemplate;
    @Mock
    private UserContextLoader userContextLoader;

    private SharedAccountDeletionService service;

    private User actingUser;
    private User coFounder;
    private SharedAccountMember membership;
    private SharedAccount sharedAccount;
    private ConfirmPasswordDto dto;

    @BeforeEach
    void setUp() {
        service = new SharedAccountDeletionService(
                userManagerService,
                passwordValidator,
                outboxService,
                sharedAccountMemberRepository,
                accountRemovalTemplate,
                userContextLoader
        );

        dto = mock(ConfirmPasswordDto.class);
        actingUser = mock(User.class);
        coFounder = mock(User.class);
        membership = mock(SharedAccountMember.class);
        sharedAccount = mock(SharedAccount.class);
    }

    @Nested
    class WhenUserNotFound {

        @Test
        void shouldThrowExceptionWhenUserNotFound() {
            when(userManagerService.getUserByIdOrThrow(ACTING_USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("User not found"));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> service.leaveSharedAccount(ACTING_USER_ID, dto));

            verifyNoInteractions(sharedAccountMemberRepository, passwordValidator, accountRemovalTemplate,
                    outboxService, userContextLoader);
        }
    }

    @Nested
    class WhenMemberNotFound {

        @BeforeEach
        void setUpUserFound() {
            when(userManagerService.getUserByIdOrThrow(ACTING_USER_ID)).thenReturn(actingUser);
        }

        @Test
        void shouldThrowExceptionWhenUserIsNotMemberOfSharedAccount() {
            when(sharedAccountMemberRepository.findByUserId(ACTING_USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> service.leaveSharedAccount(ACTING_USER_ID, dto));

            verifyNoInteractions(passwordValidator, accountRemovalTemplate, outboxService, userContextLoader);
        }
    }

    @Nested
    class WhenPasswordIsInvalid {

        @BeforeEach
        void setUpUserAndMemberFound() {
            when(userManagerService.getUserByIdOrThrow(ACTING_USER_ID)).thenReturn(actingUser);
            when(actingUser.getId()).thenReturn(ACTING_USER_ID);
            when(sharedAccountMemberRepository.findByUserId(ACTING_USER_ID))
                    .thenReturn(Optional.of(membership));
            doThrow(new InvalidInputException("Invalid password"))
                    .when(passwordValidator).validatePassword(ACTING_USER_ID, dto);
        }

        @Test
        void shouldThrowExceptionWhenPasswordValidationFails() {
            assertThrows(InvalidInputException.class,
                    () -> service.leaveSharedAccount(ACTING_USER_ID, dto));

            verifyNoInteractions(accountRemovalTemplate, outboxService, userContextLoader);
        }
    }

    @Nested
    class WhenLeavingSharedAccountSuccessfully {

        private SharedAccountDetailsDto detailsDto;
        private SharedAccountUsers usersContext;

        @BeforeEach
        void setUpValidPreconditions() {
            when(userManagerService.getUserByIdOrThrow(ACTING_USER_ID)).thenReturn(actingUser);
            when(actingUser.getId()).thenReturn(ACTING_USER_ID);
            when(actingUser.getUsername()).thenReturn(USERNAME);
            when(sharedAccountMemberRepository.findByUserId(ACTING_USER_ID))
                    .thenReturn(Optional.of(membership));
            when(membership.getSharedAccount()).thenReturn(sharedAccount);
            when(sharedAccount.getId()).thenReturn(ACCOUNT_ID);

            detailsDto = mock(SharedAccountDetailsDto.class);
            usersContext = mock(SharedAccountUsers.class);
        }

        @Test
        void shouldSaveNotificationAndActivityWithCoFounderDataWhenRemainingUserExists() {
            when(accountRemovalTemplate.handleSharedAccountRemoval(ACCOUNT_ID, ACTING_USER_ID, USERNAME))
                    .thenReturn(Optional.of(detailsDto));
            when(userContextLoader.loadUsersContext(detailsDto)).thenReturn(usersContext);
            when(usersContext.getParticualUser(ACTING_USER_ID)).thenReturn(coFounder);
            when(coFounder.getId()).thenReturn(CO_FOUNDER_ID);
            when(coFounder.getUsername()).thenReturn(CO_FOUNDER_USERNAME);
            when(coFounder.getEmail()).thenReturn(CO_FOUNDER_EMAIL);

            service.leaveSharedAccount(ACTING_USER_ID, dto);

            ArgumentCaptor<NotificationSharedAccountLeftEvent> notificationCaptor =
                    ArgumentCaptor.forClass(NotificationSharedAccountLeftEvent.class);
            verify(outboxService).save(eq("User"), eq(CO_FOUNDER_ID.toString()),
                    eq("notification.shared-account.left"), notificationCaptor.capture());
            assertEquals(ACCOUNT_ID, notificationCaptor.getValue().accountId());
            assertEquals(CO_FOUNDER_ID, notificationCaptor.getValue().recipientUserId());
            assertEquals(USERNAME, notificationCaptor.getValue().leftUsername());

            ArgumentCaptor<SharedAccountActivityEvent> activityCaptor =
                    ArgumentCaptor.forClass(SharedAccountActivityEvent.class);
            verify(outboxService).save(eq("User"), eq(ACTING_USER_ID.toString()),
                    eq("activity.shared-account"), activityCaptor.capture());
            assertEquals(ACTING_USER_ID, activityCaptor.getValue().userId());
            assertEquals(SharedAccountActivityType.LEFT_SHARED_ACCOUNT, activityCaptor.getValue().type());
            assertEquals(CO_FOUNDER_USERNAME, activityCaptor.getValue().coFounderUsername());
            assertEquals(CO_FOUNDER_EMAIL, activityCaptor.getValue().coFounderEmail());
        }

        @Test
        void shouldNotSaveNotificationButShouldSaveActivityWithNullCoFounderDataWhenNoRemainingUser() {
            when(accountRemovalTemplate.handleSharedAccountRemoval(ACCOUNT_ID, ACTING_USER_ID, USERNAME))
                    .thenReturn(Optional.empty());

            service.leaveSharedAccount(ACTING_USER_ID, dto);

            verify(outboxService, never()).save(eq("User"), anyString(), eq("notification.shared-account.left"), any());
            verifyNoInteractions(userContextLoader);

            ArgumentCaptor<SharedAccountActivityEvent> activityCaptor =
                    ArgumentCaptor.forClass(SharedAccountActivityEvent.class);
            verify(outboxService).save(eq("User"), eq(ACTING_USER_ID.toString()),
                    eq("activity.shared-account"), activityCaptor.capture());
            assertEquals(ACTING_USER_ID, activityCaptor.getValue().userId());
            assertNull(activityCaptor.getValue().coFounderUsername());
            assertNull(activityCaptor.getValue().coFounderEmail());
        }

        @Test
        void shouldCallAccountRemovalTemplateOnceWhenLeavingSharedAccount() {
            when(accountRemovalTemplate.handleSharedAccountRemoval(ACCOUNT_ID, ACTING_USER_ID, USERNAME))
                    .thenReturn(Optional.empty());

            service.leaveSharedAccount(ACTING_USER_ID, dto);

            verify(passwordValidator, times(1)).validatePassword(ACTING_USER_ID, dto);
            verify(accountRemovalTemplate, times(1))
                    .handleSharedAccountRemoval(ACCOUNT_ID, ACTING_USER_ID, USERNAME);
        }

        @Test
        void shouldLoadUsersContextOnceWhenRemainingUserExists() {
            when(accountRemovalTemplate.handleSharedAccountRemoval(ACCOUNT_ID, ACTING_USER_ID, USERNAME))
                    .thenReturn(Optional.of(detailsDto));
            when(userContextLoader.loadUsersContext(detailsDto)).thenReturn(usersContext);
            when(usersContext.getParticualUser(ACTING_USER_ID)).thenReturn(coFounder);
            when(coFounder.getId()).thenReturn(CO_FOUNDER_ID);

            service.leaveSharedAccount(ACTING_USER_ID, dto);

            verify(userContextLoader, times(1)).loadUsersContext(detailsDto);
            verify(usersContext, times(1)).getParticualUser(ACTING_USER_ID);
        }

        @Test
        void shouldValidatePasswordWithActingUserIdWhenLeavingSharedAccount() {
            when(accountRemovalTemplate.handleSharedAccountRemoval(ACCOUNT_ID, ACTING_USER_ID, USERNAME))
                    .thenReturn(Optional.empty());

            service.leaveSharedAccount(ACTING_USER_ID, dto);

            verify(passwordValidator, times(1)).validatePassword(eq(ACTING_USER_ID), eq(dto));
        }
    }

    @Nested
    class WhenOptimisticLockingOccurs {

        @BeforeEach
        void setUpValidPreconditions() {
            when(userManagerService.getUserByIdOrThrow(ACTING_USER_ID)).thenReturn(actingUser);
            when(actingUser.getId()).thenReturn(ACTING_USER_ID);
            when(actingUser.getUsername()).thenReturn(USERNAME);
            when(sharedAccountMemberRepository.findByUserId(ACTING_USER_ID))
                    .thenReturn(Optional.of(membership));
            when(membership.getSharedAccount()).thenReturn(sharedAccount);
            when(sharedAccount.getId()).thenReturn(ACCOUNT_ID);
        }

        @Test
        void shouldThrowExceptionWhenOptimisticLockingFailureOccurs() {
            when(accountRemovalTemplate.handleSharedAccountRemoval(ACCOUNT_ID, ACTING_USER_ID, USERNAME))
                    .thenThrow(new ObjectOptimisticLockingFailureException(SharedAccountMember.class, ACTING_USER_ID));

            assertThrows(ObjectOptimisticLockingFailureException.class,
                    () -> service.leaveSharedAccount(ACTING_USER_ID, dto));

            verifyNoInteractions(outboxService, userContextLoader);
        }
    }
}