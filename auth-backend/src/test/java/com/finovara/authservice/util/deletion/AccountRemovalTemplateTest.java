package com.finovara.authservice.util.deletion;

import com.finovara.authservice.sharedaccount.context.SharedAccountUsers;
import com.finovara.authservice.sharedaccount.context.UserContextLoader;
import com.finovara.authservice.sharedaccount.dto.SharedAccountDetailsDto;
import com.finovara.authservice.sharedaccount.model.SharedAccount;
import com.finovara.authservice.sharedaccount.model.SharedAccountMember;
import com.finovara.authservice.sharedaccount.model.role.SharedRole;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountRepository;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.contracts.notification.event.sharedaccount.deletion.SharedAccountDeletedEvent;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private static final String OWNER_USERNAME = "ownerUsername";
    private static final String OWNER_EMAIL = "owner@finovara.com";
    private static final String MEMBER_USERNAME = "memberUsername";
    private static final String MEMBER_EMAIL = "member@finovara.com";

    @Mock
    private UserRepository userRepository;
    @Mock
    private OutboxService outboxService;
    @Mock
    private SharedAccountRepository sharedAccountRepository;
    @Mock
    private SharedAccountMemberRepository sharedAccountMemberRepository;
    @Mock
    private UserContextLoader userContextLoader;

    private AccountRemovalTemplate template;

    private SharedAccount sharedAccount;
    private User owner;
    private User member;
    private SharedAccountUsers usersContext;

    @BeforeEach
    void setUp() {
        template = new AccountRemovalTemplate(
                userRepository,
                outboxService,
                sharedAccountRepository,
                sharedAccountMemberRepository,
                userContextLoader
        );

        sharedAccount = mock(SharedAccount.class);
        owner = mock(User.class);
        member = mock(User.class);
        usersContext = mock(SharedAccountUsers.class);
    }

    @Nested
    class WhenAccountAlreadyDeletedByConcurrentTransaction {

        @BeforeEach
        void setUpLockMiss() {
            when(sharedAccountRepository.findByIdForUpdate(ACCOUNT_ID)).thenReturn(Optional.empty());
        }

        @Test
        void shouldReturnEmptyOptionalWhenAccountAlreadyDeleted() {
            Optional<SharedAccountDetailsDto> result =
                    template.handleSharedAccountRemoval(ACCOUNT_ID, OWNER_ID, ACTING_USERNAME);

            assertThat(result).isEmpty();
            verifyNoInteractions(sharedAccountMemberRepository, outboxService, userRepository, userContextLoader);
        }
    }

    @Nested
    class WhenSharedAccountMembersAreIncomplete {

        @BeforeEach
        void setUpLockedAccount() {
            when(sharedAccountRepository.findByIdForUpdate(ACCOUNT_ID)).thenReturn(Optional.of(sharedAccount));
        }

        @Test
        void shouldDeleteAccountAndReturnEmptyOptionalWhenOwnerMissing() {
            SharedAccountMember memberOnly = mock(SharedAccountMember.class);
            when(memberOnly.getRole()).thenReturn(SharedRole.MEMBER);
            when(memberOnly.getUserId()).thenReturn(MEMBER_ID);
            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID))
                    .thenReturn(List.of(memberOnly));

            Optional<SharedAccountDetailsDto> result =
                    template.handleSharedAccountRemoval(ACCOUNT_ID, MEMBER_ID, ACTING_USERNAME);

            assertThat(result).isEmpty();
            verify(sharedAccountMemberRepository, times(1)).deleteMembersByAccountId(ACCOUNT_ID);
            verify(sharedAccountRepository, times(1)).deleteAccountById(ACCOUNT_ID);
            verifyNoInteractions(outboxService, userRepository, userContextLoader);
        }

        @Test
        void shouldDeleteAccountAndReturnEmptyOptionalWhenMemberMissing() {
            SharedAccountMember ownerOnly = mock(SharedAccountMember.class);
            when(ownerOnly.getRole()).thenReturn(SharedRole.OWNER);
            when(ownerOnly.getUserId()).thenReturn(OWNER_ID);
            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID))
                    .thenReturn(List.of(ownerOnly));

            Optional<SharedAccountDetailsDto> result =
                    template.handleSharedAccountRemoval(ACCOUNT_ID, OWNER_ID, ACTING_USERNAME);

            assertThat(result).isEmpty();
            verify(sharedAccountMemberRepository, times(1)).deleteMembersByAccountId(ACCOUNT_ID);
            verify(sharedAccountRepository, times(1)).deleteAccountById(ACCOUNT_ID);
            verifyNoInteractions(outboxService, userRepository, userContextLoader);
        }

        @Test
        void shouldDeleteAccountAndReturnEmptyOptionalWhenNoMembersFound() {
            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID))
                    .thenReturn(List.of());

            Optional<SharedAccountDetailsDto> result =
                    template.handleSharedAccountRemoval(ACCOUNT_ID, OWNER_ID, ACTING_USERNAME);

            assertThat(result).isEmpty();
            verify(sharedAccountMemberRepository, times(1)).deleteMembersByAccountId(ACCOUNT_ID);
            verify(sharedAccountRepository, times(1)).deleteAccountById(ACCOUNT_ID);
            verifyNoInteractions(outboxService, userRepository, userContextLoader);
        }
    }

    @Nested
    class WhenSharedAccountRemovedSuccessfully {

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

            when(owner.getUsername()).thenReturn(OWNER_USERNAME);
            when(owner.getEmail()).thenReturn(OWNER_EMAIL);
            when(member.getUsername()).thenReturn(MEMBER_USERNAME);
            when(member.getEmail()).thenReturn(MEMBER_EMAIL);

            when(usersContext.owner()).thenReturn(owner);
            when(usersContext.member()).thenReturn(member);
            when(userContextLoader.loadUsersContext(any(SharedAccountDetailsDto.class))).thenReturn(usersContext);
        }

        @Test
        void shouldRemoveAccountAndNotifyRemainingMemberWhenOwnerActs() {
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
            when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));

            Optional<SharedAccountDetailsDto> result =
                    template.handleSharedAccountRemovalWithNotification(ACCOUNT_ID, OWNER_ID, ACTING_USERNAME);

            assertThat(result).isPresent();
            assertThat(result.get().remainingUserId()).isEqualTo(MEMBER_ID);
            assertThat(result.get().ownerId()).isEqualTo(OWNER_ID);
            assertThat(result.get().memberId()).isEqualTo(MEMBER_ID);

            verify(sharedAccountMemberRepository, times(1)).deleteMembersByAccountId(ACCOUNT_ID);
            verify(sharedAccountRepository, times(1)).deleteAccountById(ACCOUNT_ID);

            verify(member, times(1)).setHasSharedAccount(false);
            verify(owner, times(1)).setHasSharedAccount(false);
            verify(userRepository, times(1)).save(member);
            verify(userRepository, times(1)).save(owner);

            ArgumentCaptor<SharedAccountDeletedEvent> eventCaptor = ArgumentCaptor.forClass(SharedAccountDeletedEvent.class);
            verify(outboxService, times(1)).save(eq("User"), eq(OWNER_ID.toString()),
                    eq("shared-account.deleted"), eventCaptor.capture());
            assertThat(eventCaptor.getValue().ownerUsername()).isEqualTo(OWNER_USERNAME);
            assertThat(eventCaptor.getValue().ownerEmail()).isEqualTo(OWNER_EMAIL);
            assertThat(eventCaptor.getValue().memberUsername()).isEqualTo(MEMBER_USERNAME);
            assertThat(eventCaptor.getValue().memberEmail()).isEqualTo(MEMBER_EMAIL);

            verify(outboxService, times(1)).save(eq("User"), eq(MEMBER_ID.toString()),
                    eq("notification.shared-account.deleted"), any());
        }

        @Test
        void shouldRemoveAccountAndNotifyRemainingOwnerWhenMemberActs() {
            when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

            Optional<SharedAccountDetailsDto> result =
                    template.handleSharedAccountRemovalWithNotification(ACCOUNT_ID, MEMBER_ID, ACTING_USERNAME);

            assertThat(result).isPresent();
            assertThat(result.get().remainingUserId()).isEqualTo(OWNER_ID);

            verify(outboxService, times(1)).save(eq("User"), eq(OWNER_ID.toString()),
                    eq("shared-account.deleted"), any());
            verify(outboxService, times(1)).save(eq("User"), eq(OWNER_ID.toString()),
                    eq("notification.shared-account.deleted"), any());
        }

        @Test
        void shouldCallUserContextLoaderOnceWhenRemovingSharedAccount() {
            when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

            template.handleSharedAccountRemoval(ACCOUNT_ID, OWNER_ID, ACTING_USERNAME);

            verify(userContextLoader, times(1)).loadUsersContext(any(SharedAccountDetailsDto.class));
        }
    }

    @Nested
    class WhenLoadingUsersContextFailsBecauseUserMissing {

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
        void shouldThrowExceptionWhenUserContextLoaderThrows() {
            when(userContextLoader.loadUsersContext(any(SharedAccountDetailsDto.class)))
                    .thenThrow(new RequestedEntityNotFoundException("User not found, userId=" + OWNER_ID));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> template.handleSharedAccountRemoval(ACCOUNT_ID, OWNER_ID, ACTING_USERNAME));

            verify(sharedAccountMemberRepository, never()).deleteMembersByAccountId(ACCOUNT_ID);
            verify(sharedAccountRepository, never()).deleteAccountById(ACCOUNT_ID);
            verifyNoInteractions(outboxService);
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

            when(owner.getUsername()).thenReturn(OWNER_USERNAME);
            when(owner.getEmail()).thenReturn(OWNER_EMAIL);
            when(member.getUsername()).thenReturn(MEMBER_USERNAME);
            when(member.getEmail()).thenReturn(MEMBER_EMAIL);

            when(usersContext.owner()).thenReturn(owner);
            when(usersContext.member()).thenReturn(member);
            when(userContextLoader.loadUsersContext(any(SharedAccountDetailsDto.class))).thenReturn(usersContext);
        }

        @Test
        void shouldThrowExceptionWhenRemainingUserNotFound() {
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> template.handleSharedAccountRemoval(ACCOUNT_ID, OWNER_ID, ACTING_USERNAME));

            verify(sharedAccountMemberRepository, times(1)).deleteMembersByAccountId(ACCOUNT_ID);
            verify(sharedAccountRepository, times(1)).deleteAccountById(ACCOUNT_ID);
            verify(outboxService, times(1)).save(eq("User"), eq(OWNER_ID.toString()),
                    eq("shared-account.deleted"), any());
            verify(userRepository, never()).save(any());
        }
    }
}