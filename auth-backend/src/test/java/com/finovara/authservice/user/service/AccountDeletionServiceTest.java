package com.finovara.authservice.user.service;

import com.finovara.authservice.sharedaccount.model.SharedAccount;
import com.finovara.authservice.sharedaccount.model.SharedAccountMember;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.authservice.util.confirmationpassword.service.PasswordValidator;
import com.finovara.authservice.util.deletion.AccountRemovalTemplate;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
class AccountDeletionServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;
    private static final String USERNAME = "john";
    private static final String EMAIL = "john@finovara.com";

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserManagerService userManagerService;
    @Mock
    private OutboxService outboxService;
    @Mock
    private PasswordValidator passwordValidator;
    @Mock
    private SharedAccountMemberRepository sharedAccountMemberRepository;
    @Mock
    private AccountRemovalTemplate accountRemovalTemplate;

    @InjectMocks
    private AccountDeletionService accountDeletionService;

    private User user;
    private ConfirmPasswordDto dto;

    @BeforeEach
    void setUp() {
        accountDeletionService = new AccountDeletionService(
                userRepository,
                userManagerService,
                outboxService,
                passwordValidator,
                sharedAccountMemberRepository,
                accountRemovalTemplate
        );

        dto = mock(ConfirmPasswordDto.class);
        user = mock(User.class);
    }

    @Nested
    class WhenUserNotFound {

        @Test
        void shouldThrowRequestedEntityNotFoundExceptionWhenUserNotFound() {
            when(userManagerService.getUserByIdOrThrow(USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("User not found"));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> accountDeletionService.deleteAccount(dto, USER_ID));

            verifyNoInteractions(passwordValidator, sharedAccountMemberRepository,
                    accountRemovalTemplate, outboxService, userRepository);
        }
    }

    @Nested
    class WhenPasswordIsInvalid {

        @BeforeEach
        void setUpUserFound() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
            doThrow(new InvalidInputException("Invalid password"))
                    .when(passwordValidator).validatePassword(USER_ID, dto);
        }

        @Test
        void shouldThrowInvalidInputExceptionWhenPasswordValidationFails() {
            assertThrows(InvalidInputException.class,
                    () -> accountDeletionService.deleteAccount(dto, USER_ID));

            verifyNoInteractions(sharedAccountMemberRepository, accountRemovalTemplate,
                    outboxService, userRepository);
        }

        @Test
        void shouldNotDeleteAccountWhenPasswordValidationFails() {
            assertThrows(InvalidInputException.class,
                    () -> accountDeletionService.deleteAccount(dto, USER_ID));

            verify(userRepository, never()).delete(any());
        }
    }

    @Nested
    class WhenUserHasNoSharedAccountMembership {

        @BeforeEach
        void setUpValidPreconditions() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
            when(user.getId()).thenReturn(USER_ID);
            when(user.getUsername()).thenReturn(USERNAME);
            when(user.getEmail()).thenReturn(EMAIL);
            when(sharedAccountMemberRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        }

        @Test
        void shouldDeleteAccountWithoutTouchingSharedAccountRemovalWhenNoMembership() {
            accountDeletionService.deleteAccount(dto, USER_ID);

            verifyNoInteractions(accountRemovalTemplate);
            verify(outboxService, times(1)).save(eq("User"), eq(USER_ID.toString()),
                    eq("notification.email.send"), any());
            verify(outboxService, times(1)).save(eq("User"), eq(USER_ID.toString()),
                    eq("user-account.deleted"), any());
            verify(userRepository, times(1)).delete(user);
        }

        @Test
        void shouldPublishEmailNotificationWhenDeletingAccountWithoutMembership() {
            accountDeletionService.deleteAccount(dto, USER_ID);

            verify(outboxService, times(1)).save(eq("User"), eq(USER_ID.toString()),
                    eq("notification.email.send"), any());
        }

        @Test
        void shouldPublishAccountDeletedEventWhenDeletingAccountWithoutMembership() {
            accountDeletionService.deleteAccount(dto, USER_ID);

            verify(outboxService, times(1)).save(eq("User"), eq(USER_ID.toString()),
                    eq("user-account.deleted"), any());
        }
    }

    @Nested
    class WhenUserHasSharedAccountMembership {

        private SharedAccountMember member;
        private SharedAccount sharedAccount;

        @BeforeEach
        void setUpValidPreconditions() {
            member = mock(SharedAccountMember.class);
            sharedAccount = mock(SharedAccount.class);

            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
            when(user.getUsername()).thenReturn(USERNAME);
            when(sharedAccountMemberRepository.findByUserId(USER_ID)).thenReturn(Optional.of(member));
            when(member.getSharedAccount()).thenReturn(sharedAccount);
            when(sharedAccount.getId()).thenReturn(ACCOUNT_ID);
        }

        @Test
        void shouldHandleSharedAccountRemovalThenDeleteAccountWhenHasMembership() {
            when(user.getId()).thenReturn(USER_ID);
            when(user.getEmail()).thenReturn(EMAIL);

            accountDeletionService.deleteAccount(dto, USER_ID);

            verify(accountRemovalTemplate, times(1))
                    .handleSharedAccountRemovalWithNotification(ACCOUNT_ID, USER_ID, USERNAME);
            verify(outboxService, times(1)).save(eq("User"), eq(USER_ID.toString()),
                    eq("notification.email.send"), any());
            verify(outboxService, times(1)).save(eq("User"), eq(USER_ID.toString()),
                    eq("user-account.deleted"), any());
            verify(userRepository, times(1)).delete(user);
        }

        @Test
        void shouldRethrowOptimisticLockingFailureExceptionAndNotDeleteAccountWhenLockingFails() {
            when(accountRemovalTemplate.handleSharedAccountRemovalWithNotification(ACCOUNT_ID, USER_ID, USERNAME))
                    .thenThrow(new ObjectOptimisticLockingFailureException(SharedAccountMember.class, USER_ID));

            assertThrows(ObjectOptimisticLockingFailureException.class, 
                () -> accountDeletionService.deleteAccount(dto, USER_ID));

            verifyNoInteractions(outboxService);
            verify(userRepository, never()).delete(any());
        }

        @Test
        void shouldNotPublishEventsWhenSharedAccountRemovalFails() {
            when(accountRemovalTemplate.handleSharedAccountRemovalWithNotification(ACCOUNT_ID, USER_ID, USERNAME))
                    .thenThrow(new ObjectOptimisticLockingFailureException(SharedAccountMember.class, USER_ID));

            assertThrows(ObjectOptimisticLockingFailureException.class, 
                () -> accountDeletionService.deleteAccount(dto, USER_ID));

            verifyNoInteractions(outboxService);
        }
    }
}