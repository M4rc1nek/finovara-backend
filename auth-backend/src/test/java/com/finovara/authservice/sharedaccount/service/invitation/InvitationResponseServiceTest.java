package com.finovara.authservice.sharedaccount.service.invitation;

import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import com.finovara.authservice.sharedaccount.model.SharedAccount;
import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationResponseServiceTest {

    @Mock private SharedAccountInvitationRepository invitationRepository;
    @Mock private SharedAccountRepository sharedAccountRepository;
    @Mock private InvitationValidator invitationValidator;
    @Mock private SharedAccountMemberService memberService;
    @Mock private UserRepository userRepository;
    @Mock private OutboxService outboxService;
    @Mock private AdditionalAuthorizationService authorizationService;
    @Mock private AdditionalAuthorizationCodeResolver codeResolver;
    @InjectMocks private InvitationResponseService service;
    private SharedAccountInvitation invitation;

    @BeforeEach
    void setUp() {
        invitation = SharedAccountInvitation.builder().id(5L).inviterUserId(1L).inviteeUserId(2L)
                .expiresAt(LocalDateTime.now().plusHours(1)).build();
    }

    @Nested
    class AcceptInvite {
        @Test
        void shouldThrowExceptionWhenActiveInvitationDoesNotExist() {
            when(invitationRepository.findByIdAndInviteeUserId(5L, 2L)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.acceptInvite(2L, 5L, "code"));
            verifyNoInteractions(invitationValidator, memberService, userRepository, sharedAccountRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenInvitationHasExpired() {
            invitation = SharedAccountInvitation.builder().id(5L).inviterUserId(1L).inviteeUserId(2L)
                    .expiresAt(LocalDateTime.now().minusSeconds(1)).build();
            when(invitationRepository.findByIdAndInviteeUserId(5L, 2L)).thenReturn(Optional.of(invitation));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.acceptInvite(2L, 5L, "code"));
            verifyNoInteractions(invitationValidator, memberService, userRepository, sharedAccountRepository, outboxService);
        }

        @Test
        void shouldCreateAccountAndPublishEventsWhenInvitationIsAccepted() {
            UserDataDto inviter = new UserDataDto(1L, "anna", "a@x.com", null);
            UserDataDto invitee = new UserDataDto(2L, "bob", "b@x.com", null);
            SharedAccount account = SharedAccount.builder().id(20L).build();
            when(invitationRepository.findByIdAndInviteeUserId(5L, 2L)).thenReturn(Optional.of(invitation));
            when(userRepository.findBasicInfoByIds(List.of(1L, 2L))).thenReturn(List.of(inviter, invitee));
            when(sharedAccountRepository.save(any(SharedAccount.class))).thenReturn(account);
            when(invitationRepository.findAllInvolvingUsers(List.of(1L, 2L))).thenReturn(List.of());

            service.acceptInvite(2L, 5L, "code");

            verify(invitationValidator).validateAcceptInvite(1L, 2L, 5L);
            verify(memberService).createOwnerAndMember(account, 1L, 2L);
            verify(invitationRepository).delete(invitation);
            verify(outboxService, times(4)).save(any(), any(), any(), any());
        }

        @Test
        void shouldThrowExceptionWhenMemberCreationViolatesUniqueConstraint() {
            UserDataDto inviter = new UserDataDto(1L, "anna", "a@x.com", null);
            UserDataDto invitee = new UserDataDto(2L, "bob", "b@x.com", null);
            when(invitationRepository.findByIdAndInviteeUserId(5L, 2L)).thenReturn(Optional.of(invitation));
            when(userRepository.findBasicInfoByIds(List.of(1L, 2L))).thenReturn(List.of(inviter, invitee));
            when(sharedAccountRepository.save(any(SharedAccount.class))).thenReturn(SharedAccount.builder().id(20L).build());
            doThrow(new DataIntegrityViolationException("duplicate")).when(memberService).createOwnerAndMember(any(), eq(1L), eq(2L));

            assertThrows(EntityAlreadyExistsException.class, () -> service.acceptInvite(2L, 5L, "code"));
            verifyNoInteractions(outboxService);
        }
    }

    @Nested
    class RejectInvite {
        @Test
        void shouldDeleteInvitationAndPublishEventsWhenInvitationIsRejected() {
            UserDataDto inviter = new UserDataDto(1L, "anna", "a@x.com", null);
            UserDataDto invitee = new UserDataDto(2L, "bob", "b@x.com", null);
            when(invitationRepository.findByIdAndInviteeUserId(5L, 2L)).thenReturn(Optional.of(invitation));
            when(userRepository.findBasicInfoByIds(List.of(1L, 2L))).thenReturn(List.of(inviter, invitee));

            service.rejectInvite(2L, 5L, "code");

            verify(invitationRepository).delete(invitation);
            verify(outboxService, times(2)).save(any(), any(), any(), any());
        }

        @Test
        void shouldThrowExceptionWhenInviterCannotBeLoaded() {
            when(invitationRepository.findByIdAndInviteeUserId(5L, 2L)).thenReturn(Optional.of(invitation));
            when(userRepository.findBasicInfoByIds(List.of(1L, 2L))).thenReturn(List.of(new UserDataDto(2L, "bob", "b@x.com", null)));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.rejectInvite(2L, 5L, "code"));
            verify(invitationRepository, never()).delete(invitation);
        }
    }
}
