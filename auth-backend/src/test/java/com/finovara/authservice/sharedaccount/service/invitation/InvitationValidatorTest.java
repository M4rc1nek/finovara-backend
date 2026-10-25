package com.finovara.authservice.sharedaccount.service.invitation;

import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationValidatorTest {

    @Mock
    private SharedAccountMemberRepository memberRepository;
    @Mock
    private SharedAccountInvitationRepository invitationRepository;
    @InjectMocks
    private InvitationValidator validator;

    @Nested
    class ValidateSendInvitation {

        @Test
        void shouldValidateWhenUsersAreDifferentAndAvailable() {
            when(memberRepository.existsByUserIdIn(anyList())).thenReturn(false);
            when(invitationRepository.findInvitationBetweenUsers(1L, 2L)).thenReturn(Optional.empty());
            assertDoesNotThrow(() -> validator.validateSendInvitation(1L, 2L));
            verify(invitationRepository).findInvitationBetweenUsers(1L, 2L);
        }

        @Test
        void shouldThrowExceptionWhenUserInvitesThemselves() {
            assertThrows(InvalidInputException.class, () -> validator.validateSendInvitation(1L, 1L));
            verify(memberRepository, never()).existsByUserIdIn(anyList());
        }

        @Test
        void shouldThrowExceptionWhenAnyUserAlreadyHasSharedAccount() {
            when(memberRepository.existsByUserIdIn(anyList())).thenReturn(true);

            assertThrows(EntityAlreadyExistsException.class, () -> validator.validateSendInvitation(1L, 2L));
            verify(invitationRepository, never()).findInvitationBetweenUsers(1L, 2L);
        }

        @Test
        void shouldThrowExceptionWhenInvitationAlreadyExists() {
            when(invitationRepository.findInvitationBetweenUsers(1L, 2L)).thenReturn(Optional.of(new SharedAccountInvitation()));

            assertThrows(EntityAlreadyExistsException.class, () -> validator.validateSendInvitation(1L, 2L));
        }
    }

    @Nested
    class ValidateAcceptInvite {
        @Test
        void shouldValidateWhenNeitherUserHasSharedAccount() {
            when(memberRepository.existsByUserIdIn(anyList())).thenReturn(false);

            assertDoesNotThrow(() -> validator.validateAcceptInvite(1L, 2L, 3L));
        }

        @Test
        void shouldThrowExceptionWhenAnyUserAlreadyHasSharedAccount() {
            when(memberRepository.existsByUserIdIn(anyList())).thenReturn(true);

            assertThrows(EntityAlreadyExistsException.class, () -> validator.validateAcceptInvite(1L, 2L, 3L));
        }
    }
}
