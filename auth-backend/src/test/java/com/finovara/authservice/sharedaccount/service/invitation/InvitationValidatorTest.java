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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationValidatorTest {

    @Mock
    private SharedAccountMemberRepository sharedAccountMemberRepository;
    @Mock
    private SharedAccountInvitationRepository sharedAccountInvitationRepository;

    @InjectMocks
    private InvitationValidator invitationValidator;

    private static final Long INVITER_USER_ID = 1L;
    private static final Long INVITEE_USER_ID = 2L;
    private static final Long INVITATION_ID = 10L;

    @Nested
    class ValidateSendInvitationTests {

        @Test
        void shouldPassWhenNoConflictsExist() {
            when(sharedAccountMemberRepository.existsByUserId(INVITER_USER_ID)).thenReturn(false);
            when(sharedAccountMemberRepository.existsByUserId(INVITEE_USER_ID)).thenReturn(false);
            when(sharedAccountInvitationRepository.findInvitationBetweenUsers(INVITER_USER_ID, INVITEE_USER_ID))
                    .thenReturn(Optional.empty());

            assertDoesNotThrow(() -> invitationValidator.validateSendInvitation(INVITER_USER_ID, INVITEE_USER_ID));
        }

        @Test
        void shouldThrowExceptionWhenInvitingSelf() {
            InvalidInputException exception = assertThrows(InvalidInputException.class,
                    () -> invitationValidator.validateSendInvitation(INVITER_USER_ID, INVITER_USER_ID));

            assertEquals("You cannot invite yourself", exception.getMessage());
            verifyNoInteractions(sharedAccountMemberRepository);
            verifyNoInteractions(sharedAccountInvitationRepository);
        }

        @Test
        void shouldThrowExceptionWhenInviterAlreadyHasSharedAccount() {
            when(sharedAccountMemberRepository.existsByUserId(INVITER_USER_ID)).thenReturn(true);

            EntityAlreadyExistsException exception = assertThrows(EntityAlreadyExistsException.class,
                    () -> invitationValidator.validateSendInvitation(INVITER_USER_ID, INVITEE_USER_ID));

            assertEquals("One of the users already belongs to a shared account", exception.getMessage());
            verifyNoInteractions(sharedAccountInvitationRepository);
        }

        @Test
        void shouldThrowExceptionWhenInviteeAlreadyHasSharedAccount() {
            when(sharedAccountMemberRepository.existsByUserId(INVITER_USER_ID)).thenReturn(false);
            when(sharedAccountMemberRepository.existsByUserId(INVITEE_USER_ID)).thenReturn(true);

            EntityAlreadyExistsException exception = assertThrows(EntityAlreadyExistsException.class,
                    () -> invitationValidator.validateSendInvitation(INVITER_USER_ID, INVITEE_USER_ID));

            assertEquals("One of the users already belongs to a shared account", exception.getMessage());
            verifyNoInteractions(sharedAccountInvitationRepository);
        }

        @Test
        void shouldThrowExceptionWhenInvitationAlreadyExistsBetweenUsers() {
            when(sharedAccountMemberRepository.existsByUserId(INVITER_USER_ID)).thenReturn(false);
            when(sharedAccountMemberRepository.existsByUserId(INVITEE_USER_ID)).thenReturn(false);

            SharedAccountInvitation existingInvitation = SharedAccountInvitation.builder()
                    .inviterUserId(INVITER_USER_ID)
                    .inviteeUserId(INVITEE_USER_ID)
                    .build();

            when(sharedAccountInvitationRepository.findInvitationBetweenUsers(INVITER_USER_ID, INVITEE_USER_ID))
                    .thenReturn(Optional.of(existingInvitation));

            EntityAlreadyExistsException exception = assertThrows(EntityAlreadyExistsException.class,
                    () -> invitationValidator.validateSendInvitation(INVITER_USER_ID, INVITEE_USER_ID));

            assertEquals("Invitation already exists between these users!", exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenInviterUserIdIsNull() {
            assertThrows(NullPointerException.class,
                    () -> invitationValidator.validateSendInvitation(null, INVITEE_USER_ID));
        }

        @Test
        void shouldNotCallInvitationRepositoryWhenMemberConflictExists() {
            when(sharedAccountMemberRepository.existsByUserId(INVITER_USER_ID)).thenReturn(true);

            assertThrows(EntityAlreadyExistsException.class,
                    () -> invitationValidator.validateSendInvitation(INVITER_USER_ID, INVITEE_USER_ID));

            verifyNoInteractions(sharedAccountInvitationRepository);
        }
    }

    @Nested
    class ValidateAcceptInviteTests {

        @Test
        void shouldPassWhenNeitherUserHasSharedAccount() {
            when(sharedAccountMemberRepository.existsByUserId(INVITER_USER_ID)).thenReturn(false);
            when(sharedAccountMemberRepository.existsByUserId(INVITEE_USER_ID)).thenReturn(false);

            assertDoesNotThrow(() ->
                    invitationValidator.validateAcceptInvite(INVITER_USER_ID, INVITEE_USER_ID, INVITATION_ID));
        }

        @Test
        void shouldThrowExceptionWhenInviterAlreadyHasSharedAccount() {
            when(sharedAccountMemberRepository.existsByUserId(INVITER_USER_ID)).thenReturn(true);

            EntityAlreadyExistsException exception = assertThrows(EntityAlreadyExistsException.class,
                    () -> invitationValidator.validateAcceptInvite(INVITER_USER_ID, INVITEE_USER_ID, INVITATION_ID));

            assertEquals("One of the users already belongs to a shared account", exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenInviteeAlreadyHasSharedAccount() {
            when(sharedAccountMemberRepository.existsByUserId(INVITER_USER_ID)).thenReturn(false);
            when(sharedAccountMemberRepository.existsByUserId(INVITEE_USER_ID)).thenReturn(true);

            EntityAlreadyExistsException exception = assertThrows(EntityAlreadyExistsException.class,
                    () -> invitationValidator.validateAcceptInvite(INVITER_USER_ID, INVITEE_USER_ID, INVITATION_ID));

            assertEquals("One of the users already belongs to a shared account", exception.getMessage());
        }

        @Test
        void shouldNotCallInvitationRepositoryWhenValidatingAccept() {
            when(sharedAccountMemberRepository.existsByUserId(INVITER_USER_ID)).thenReturn(false);
            when(sharedAccountMemberRepository.existsByUserId(INVITEE_USER_ID)).thenReturn(false);

            invitationValidator.validateAcceptInvite(INVITER_USER_ID, INVITEE_USER_ID, INVITATION_ID);

            verifyNoInteractions(sharedAccountInvitationRepository);
        }

        @Test
        void shouldPassWhenInviterUserIdIsNullAndNeitherUserHasSharedAccount() {
            when(sharedAccountMemberRepository.existsByUserId(null)).thenReturn(false);
            when(sharedAccountMemberRepository.existsByUserId(INVITEE_USER_ID)).thenReturn(false);

            assertDoesNotThrow(() ->
                    invitationValidator.validateAcceptInvite(null, INVITEE_USER_ID, INVITATION_ID));
        }

        @Test
        void shouldThrowExceptionWhenInviterUserIdIsNullButInviteeAlreadyHasSharedAccount() {
            when(sharedAccountMemberRepository.existsByUserId(null)).thenReturn(false);
            when(sharedAccountMemberRepository.existsByUserId(INVITEE_USER_ID)).thenReturn(true);

            EntityAlreadyExistsException exception = assertThrows(EntityAlreadyExistsException.class,
                    () -> invitationValidator.validateAcceptInvite(null, INVITEE_USER_ID, INVITATION_ID));

            assertEquals("One of the users already belongs to a shared account", exception.getMessage());
        }
    }

}