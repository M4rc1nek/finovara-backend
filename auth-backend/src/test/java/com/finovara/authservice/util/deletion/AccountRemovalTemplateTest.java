package com.finovara.authservice.util.deletion;

import com.finovara.authservice.sharedaccount.dto.SharedAccountDetailsDto;
import com.finovara.authservice.sharedaccount.model.SharedAccount;
import com.finovara.authservice.sharedaccount.model.SharedAccountMember;
import com.finovara.authservice.sharedaccount.model.role.SharedRole;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountRepository;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountRemovalTemplateTest {

    private static final Long ACCOUNT_ID = 10L;
    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final String ACTING_USERNAME = "john";

    @Mock
    private UserRepository userRepository;
    @Mock
    private OutboxService outboxService;
    @Mock
    private SharedAccountRepository sharedAccountRepository;
    @Mock
    private SharedAccountMemberRepository sharedAccountMemberRepository;

    private AccountRemovalTemplate template;

    private SharedAccount sharedAccount;

    @BeforeEach
    void setUp() {
        template = new AccountRemovalTemplate(
                userRepository,
                outboxService,
                sharedAccountRepository,
                sharedAccountMemberRepository
        );

        sharedAccount = mock(SharedAccount.class);
    }

    @Nested
    class WhenAccountAlreadyDeletedByConcurrentTransaction {

        @BeforeEach
        void setUpLockMiss() {
            when(sharedAccountRepository.findByIdForUpdate(ACCOUNT_ID)).thenReturn(Optional.empty());
        }

        @Test
        void shouldReturnEmptyAndNotTouchAnythingElse() {
            Optional<SharedAccountDetailsDto> result =
                    template.handleSharedAccountRemoval(ACCOUNT_ID, OWNER_ID, ACTING_USERNAME);

            assertTrue(result.isEmpty());
            verifyNoInteractions(sharedAccountMemberRepository, outboxService, userRepository);
        }
    }

    @Nested
    class WhenSharedAccountMembersAreIncomplete {

        @BeforeEach
        void setUpLockedAccount() {
            when(sharedAccountRepository.findByIdForUpdate(ACCOUNT_ID)).thenReturn(Optional.of(sharedAccount));
        }

        @Test
        void shouldDeleteAccountAndReturnEmptyWhenOwnerMissing() {
            SharedAccountMember memberOnly = mock(SharedAccountMember.class);
            when(memberOnly.getRole()).thenReturn(SharedRole.MEMBER);
            when(memberOnly.getUserId()).thenReturn(MEMBER_ID);
            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID))
                    .thenReturn(List.of(memberOnly));

            Optional<SharedAccountDetailsDto> result =
                    template.handleSharedAccountRemoval(ACCOUNT_ID, MEMBER_ID, ACTING_USERNAME);

            assertTrue(result.isEmpty());
            verify(sharedAccountMemberRepository, times(1)).deleteMembersByAccountId(ACCOUNT_ID);
            verify(sharedAccountRepository, times(1)).deleteAccountById(ACCOUNT_ID);
            verifyNoInteractions(outboxService, userRepository);
        }

        @Test
        void shouldDeleteAccountAndReturnEmptyWhenMemberMissing() {
            SharedAccountMember ownerOnly = mock(SharedAccountMember.class);
            when(ownerOnly.getRole()).thenReturn(SharedRole.OWNER);
            when(ownerOnly.getUserId()).thenReturn(OWNER_ID);
            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID))
                    .thenReturn(List.of(ownerOnly));

            Optional<SharedAccountDetailsDto> result =
                    template.handleSharedAccountRemoval(ACCOUNT_ID, OWNER_ID, ACTING_USERNAME);

            assertTrue(result.isEmpty());
            verify(sharedAccountMemberRepository, times(1)).deleteMembersByAccountId(ACCOUNT_ID);
            verify(sharedAccountRepository, times(1)).deleteAccountById(ACCOUNT_ID);
            verifyNoInteractions(outboxService, userRepository);
        }

        @Test
        void shouldDeleteAccountAndReturnEmptyWhenNoMembersFound() {
            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID))
                    .thenReturn(List.of());

            Optional<SharedAccountDetailsDto> result =
                    template.handleSharedAccountRemoval(ACCOUNT_ID, OWNER_ID, ACTING_USERNAME);

            assertTrue(result.isEmpty());
            verify(sharedAccountMemberRepository, times(1)).deleteMembersByAccountId(ACCOUNT_ID);
            verify(sharedAccountRepository, times(1)).deleteAccountById(ACCOUNT_ID);
            verifyNoInteractions(outboxService, userRepository);
        }
    }

    @Nested
    class WhenSharedAccountRemovedSuccessfully {

        private SharedAccountMember ownerMember;
        private SharedAccountMember memberMember;
        private User owner;
        private User member;

        @BeforeEach
        void setUpCompleteMembers() {
            when(sharedAccountRepository.findByIdForUpdate(ACCOUNT_ID)).thenReturn(Optional.of(sharedAccount));

            ownerMember = mock(SharedAccountMember.class);
            when(ownerMember.getRole()).thenReturn(SharedRole.OWNER);
            when(ownerMember.getUserId()).thenReturn(OWNER_ID);

            memberMember = mock(SharedAccountMember.class);
            when(memberMember.getRole()).thenReturn(SharedRole.MEMBER);
            when(memberMember.getUserId()).thenReturn(MEMBER_ID);

            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID))
                    .thenReturn(List.of(ownerMember, memberMember));

            owner = mock(User.class);
            member = mock(User.class);
        }

        @Test
        void shouldRemoveAccountAndNotifyRemainingMemberWhenOwnerActs() {
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
            when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));

            Optional<SharedAccountDetailsDto> result =
                    template.handleSharedAccountRemoval(ACCOUNT_ID, OWNER_ID, ACTING_USERNAME);

            assertTrue(result.isPresent());
            assertEquals(MEMBER_ID, result.get().remainingUserId());
            assertEquals(OWNER_ID, result.get().ownerId());
            assertEquals(MEMBER_ID, result.get().memberId());

            verify(sharedAccountMemberRepository, times(1)).deleteMembersByAccountId(ACCOUNT_ID);
            verify(sharedAccountRepository, times(1)).deleteAccountById(ACCOUNT_ID);

            verify(member, times(1)).setHasSharedAccount(false);
            verify(owner, times(1)).setHasSharedAccount(false);
            verify(userRepository, times(1)).save(member);
            verify(userRepository, times(1)).save(owner);

            verify(outboxService, times(1)).save(eq("User"), eq(OWNER_ID.toString()),
                    eq("shared-account.deleted"), any());
            verify(outboxService, times(1)).save(eq("User"), eq(MEMBER_ID.toString()),
                    eq("notification.shared-account.deleted"), any());
        }

        @Test
        void shouldRemoveAccountAndNotifyRemainingOwnerWhenMemberActs() {
            when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

            Optional<SharedAccountDetailsDto> result =
                    template.handleSharedAccountRemoval(ACCOUNT_ID, MEMBER_ID, ACTING_USERNAME);

            assertTrue(result.isPresent());
            assertEquals(OWNER_ID, result.get().remainingUserId());

            verify(outboxService, times(1)).save(eq("User"), eq(OWNER_ID.toString()),
                    eq("shared-account.deleted"), any());
            verify(outboxService, times(1)).save(eq("User"), eq(OWNER_ID.toString()),
                    eq("notification.shared-account.deleted"), any());
        }
    }

    @Nested
    class WhenClearingHasSharedAccountFlagFailsBecauseUserMissing {

        @BeforeEach
        void setUpCompleteMembers() {
            when(sharedAccountRepository.findByIdForUpdate(ACCOUNT_ID)).thenReturn(Optional.of(sharedAccount));

            SharedAccountMember ownerMember = mock(SharedAccountMember.class);
            when(ownerMember.getRole()).thenReturn(SharedRole.OWNER);
            when(ownerMember.getUserId()).thenReturn(OWNER_ID);

            SharedAccountMember memberMember = mock(SharedAccountMember.class);
            when(memberMember.getRole()).thenReturn(SharedRole.MEMBER);
            when(memberMember.getUserId()).thenReturn(MEMBER_ID);

            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID))
                    .thenReturn(List.of(ownerMember, memberMember));
        }

        @Test
        void shouldThrowWhenRemainingUserNotFound() {
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> template.handleSharedAccountRemoval(ACCOUNT_ID, OWNER_ID, ACTING_USERNAME));

            verify(sharedAccountMemberRepository, times(1)).deleteMembersByAccountId(ACCOUNT_ID);
            verify(sharedAccountRepository, times(1)).deleteAccountById(ACCOUNT_ID);
            verifyNoInteractions(outboxService);
            verify(userRepository, never()).save(any());
        }
    }
}