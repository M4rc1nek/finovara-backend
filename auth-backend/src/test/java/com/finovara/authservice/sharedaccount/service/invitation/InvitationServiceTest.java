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
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.user.authorization.dto.UserDataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock private SharedAccountMemberRepository memberRepository;
    @Mock private OutboxService outboxService;
    @Mock private UserRepository userRepository;
    @Mock private SharedAccountInvitationRepository invitationRepository;
    @Mock private InvitationValidator invitationValidator;
    @Mock private AdditionalAuthorizationService authorizationService;
    @Mock private AdditionalAuthorizationCodeResolver codeResolver;
    @InjectMocks private InvitationService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pageSize", 20);
        ReflectionTestUtils.setField(service, "invitationPageSize", 10);
        ReflectionTestUtils.setField(service, "minSearchQueryLength", 3);
        ReflectionTestUtils.setField(service, "invitationExpirationHours", 24);
    }

    @Nested
    class SearchUser {
        @Test
        void shouldReturnEmptyListWhenQueryIsNullOrTooShort() {
            assertTrue(service.searchUser(null, 1L, 0).isEmpty());
            assertTrue(service.searchUser(" ab ", 1L, 0).isEmpty());
            verifyNoInteractions(userRepository);
        }

        @Test
        void shouldTrimQueryAndSearchWhenQueryHasMinimumLength() {
            List<UserDataDto> users = List.of(new UserDataDto(2L, "anna", "a@x.com", null));
            when(userRepository.searchByUsernameOrEmail(eq("anna"), eq(1L), any())).thenReturn(users);

            assertEquals(users, service.searchUser(" anna ", 1L, 1));
        }
    }

    @Nested
    class SharedAccountStatus {
        @Test
        void shouldReturnActiveStatusWhenMembershipExists() {
            SharedAccount account = SharedAccount.builder().id(5L).build();
            when(memberRepository.findByUserId(1L)).thenReturn(Optional.of(SharedAccountMember.builder().sharedAccount(account).build()));

            SharedAccountStatusDto result = service.hasSharedAccount(1L);

            assertTrue(result.hasSharedAccount());
            assertEquals(5L, result.accountId());
        }

        @Test
        void shouldReturnInactiveStatusWhenMembershipDoesNotExist() {
            when(memberRepository.findByUserId(1L)).thenReturn(Optional.empty());

            SharedAccountStatusDto result = service.hasSharedAccount(1L);

            assertFalse(result.hasSharedAccount());
            assertEquals(null, result.accountId());
        }
    }

    @Nested
    class SendInvitation {
        @Test
        void shouldSaveInvitationAndPublishEventsWhenUsersExist() {
            UserDataDto inviter = new UserDataDto(1L, "anna", "a@x.com", null);
            UserDataDto invitee = new UserDataDto(2L, "bob", "b@x.com", null);
            when(userRepository.findBasicInfoByIds(List.of(1L, 2L))).thenReturn(List.of(inviter, invitee));

            service.sendInvitation(1L, 2L, "code");

            verify(invitationValidator).validateSendInvitation(1L, 2L);
            verify(invitationRepository).save(any(SharedAccountInvitation.class));
            verify(outboxService).save(eq("User"), eq("1"), eq("shared-account.changed"), any());
            verify(outboxService).save(eq("User"), eq("1"), eq("notification.shared-account.invitation-sent"), any());
        }

        @Test
        void shouldThrowExceptionWhenInviterDoesNotExist() {
            when(userRepository.findBasicInfoByIds(List.of(1L, 2L))).thenReturn(List.of(new UserDataDto(2L, "bob", "b@x.com", null)));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.sendInvitation(1L, 2L, "code"));
            verify(invitationRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenDuplicateInvitationIsDetectedByDatabase() {
            UserDataDto inviter = new UserDataDto(1L, "anna", "a@x.com", null);
            UserDataDto invitee = new UserDataDto(2L, "bob", "b@x.com", null);
            when(userRepository.findBasicInfoByIds(List.of(1L, 2L))).thenReturn(List.of(inviter, invitee));
            when(invitationRepository.save(any(SharedAccountInvitation.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

            assertThrows(EntityAlreadyExistsException.class, () -> service.sendInvitation(1L, 2L, "code"));
            verifyNoInteractions(outboxService);
        }
    }

    @Nested
    class PendingInvitations {
        @Test
        void shouldReturnRepositoryResultsWhenPendingInvitationsAreRequested() {
            List<InvitationResponse> invitations = List.of(new InvitationResponse(1L, 2L, "anna", LocalDateTime.now()));
            when(invitationRepository.findInvitationWithInviterUsername(eq(1L), any())).thenReturn(invitations);

            assertEquals(invitations, service.getPendingInvitations(1L, 0));
        }
    }
}
