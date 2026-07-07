package com.finovara.authservice.sharedaccount.service.deletion;

import com.finovara.authservice.sharedaccount.dto.SharedAccountDetailsDto;
import com.finovara.authservice.sharedaccount.model.SharedAccount;
import com.finovara.authservice.sharedaccount.model.SharedAccountMember;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.util.confirmationpassword.service.PasswordValidator;
import com.finovara.authservice.util.deletion.AccountRemovalTemplate;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedAccountDeletionServiceTest {

    private static final Long ACTING_USER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;
    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long REMAINING_USER_ID = 2L;
    private static final String USERNAME = "john";

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

    private SharedAccountDeletionService service;

    private User user;
    private SharedAccountMember member;
    private SharedAccount sharedAccount;
    private ConfirmPasswordDto dto;

    @BeforeEach
    void setUp() {
        service = new SharedAccountDeletionService(
                userManagerService,
                passwordValidator,
                outboxService,
                sharedAccountMemberRepository,
                accountRemovalTemplate
        );

        dto = mock(ConfirmPasswordDto.class);
        user = mock(User.class);
        member = mock(SharedAccountMember.class);
        sharedAccount = mock(SharedAccount.class);
    }

    @Nested
    class WhenUserNotFound {

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userManagerService.getUserByIdOrThrow(ACTING_USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("User not found"));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> service.leaveSharedAccount(ACTING_USER_ID, dto));

            verifyNoInteractions(sharedAccountMemberRepository, passwordValidator, accountRemovalTemplate, outboxService);
        }
    }

    @Nested
    class WhenMemberNotFound {

        @BeforeEach
        void setUpUserFound() {
            when(userManagerService.getUserByIdOrThrow(ACTING_USER_ID)).thenReturn(user);
        }

        @Test
        void shouldThrowWhenUserIsNotMemberOfSharedAccount() {
            when(sharedAccountMemberRepository.findByUserId(ACTING_USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> service.leaveSharedAccount(ACTING_USER_ID, dto));

            verifyNoInteractions(passwordValidator, accountRemovalTemplate, outboxService);
        }
    }

    @Nested
    class WhenPasswordIsInvalid {

        @BeforeEach
        void setUpUserAndMemberFound() {
            when(userManagerService.getUserByIdOrThrow(ACTING_USER_ID)).thenReturn(user);
            when(user.getId()).thenReturn(ACTING_USER_ID);
            when(sharedAccountMemberRepository.findByUserId(ACTING_USER_ID))
                    .thenReturn(Optional.of(member));
            doThrow(new InvalidInputException("Invalid password"))
                    .when(passwordValidator).validatePassword(ACTING_USER_ID, dto);
        }

        @Test
        void shouldThrowWhenPasswordValidationFails() {
            assertThrows(InvalidInputException.class,
                    () -> service.leaveSharedAccount(ACTING_USER_ID, dto));

            verifyNoInteractions(accountRemovalTemplate, outboxService);
        }
    }

    @Nested
    class WhenLeavingSharedAccountSuccessfully {

        @BeforeEach
        void setUpValidPreconditions() {
            when(userManagerService.getUserByIdOrThrow(ACTING_USER_ID)).thenReturn(user);
            when(user.getId()).thenReturn(ACTING_USER_ID);
            when(user.getUsername()).thenReturn(USERNAME);
            when(sharedAccountMemberRepository.findByUserId(ACTING_USER_ID))
                    .thenReturn(Optional.of(member));
            when(member.getSharedAccount()).thenReturn(sharedAccount);
            when(sharedAccount.getId()).thenReturn(ACCOUNT_ID);
        }

        @Test
        void shouldNotSaveOutboxEventWhenNoRemainingUser() {
            when(accountRemovalTemplate.handleSharedAccountRemoval(ACCOUNT_ID, ACTING_USER_ID, USERNAME))
                    .thenReturn(Optional.empty());

            service.leaveSharedAccount(ACTING_USER_ID, dto);

            verify(passwordValidator, times(1)).validatePassword(ACTING_USER_ID, dto);
            verify(accountRemovalTemplate, times(1))
                    .handleSharedAccountRemoval(ACCOUNT_ID, ACTING_USER_ID, USERNAME);
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldSaveOutboxEventWhenRemainingUserPresent() {
            SharedAccountDetailsDto details = new SharedAccountDetailsDto(REMAINING_USER_ID, OWNER_ID, MEMBER_ID);
            when(accountRemovalTemplate.handleSharedAccountRemoval(ACCOUNT_ID, ACTING_USER_ID, USERNAME))
                    .thenReturn(Optional.of(details));

            service.leaveSharedAccount(ACTING_USER_ID, dto);

            verify(outboxService, times(1)).save(
                    eq("User"),
                    eq(REMAINING_USER_ID.toString()),
                    eq("notification.shared-account.left"),
                    any());
        }
    }

    @Nested
    class WhenOptimisticLockingOccurs {

        @BeforeEach
        void setUpValidPreconditions() {
            when(userManagerService.getUserByIdOrThrow(ACTING_USER_ID)).thenReturn(user);
            when(user.getId()).thenReturn(ACTING_USER_ID);
            when(user.getUsername()).thenReturn(USERNAME);
            when(sharedAccountMemberRepository.findByUserId(ACTING_USER_ID))
                    .thenReturn(Optional.of(member));
            when(member.getSharedAccount()).thenReturn(sharedAccount);
            when(sharedAccount.getId()).thenReturn(ACCOUNT_ID);
        }

        @Test
        void shouldRethrowOptimisticLockingFailureException() {
            when(accountRemovalTemplate.handleSharedAccountRemoval(ACCOUNT_ID, ACTING_USER_ID, USERNAME))
                    .thenThrow(new ObjectOptimisticLockingFailureException(SharedAccountMember.class, ACTING_USER_ID));

            assertThrows(ObjectOptimisticLockingFailureException.class,
                    () -> service.leaveSharedAccount(ACTING_USER_ID, dto));

            verifyNoInteractions(outboxService);
        }
    }
}