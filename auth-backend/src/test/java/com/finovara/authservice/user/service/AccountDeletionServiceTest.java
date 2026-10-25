package com.finovara.authservice.user.service;

import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.authservice.util.confirmationpassword.service.PasswordValidator;
import com.finovara.authservice.util.deletion.AccountRemovalTemplate;
import com.finovara.authservice.util.user.UserDataProjection;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.user.authorization.dto.ConfirmPasswordDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private OutboxService outboxService;
    @Mock private PasswordValidator passwordValidator;
    @Mock private SharedAccountMemberRepository memberRepository;
    @Mock private SharedAccountInvitationRepository invitationRepository;
    @Mock private AccountRemovalTemplate removalTemplate;
    @InjectMocks private AccountDeletionService service;

    @Nested
    class DeleteAccount {
        @Test
        void shouldDeleteAccountAndPublishEventsWhenUserExists() {
            UserDataProjection user = mock(UserDataProjection.class);
            when(user.getUsername()).thenReturn("anna");
            when(user.getEmail()).thenReturn("a@x.com");
            when(userRepository.findUserDataById(1L)).thenReturn(Optional.of(user));
            when(memberRepository.findByUserId(1L)).thenReturn(Optional.empty());
            when(invitationRepository.findAllInvolvingUsers(List.of(1L))).thenReturn(List.of(new SharedAccountInvitation()));

            service.deleteAccount(new ConfirmPasswordDto("password"), 1L);

            verify(passwordValidator).validatePassword(eq(1L), any(ConfirmPasswordDto.class));
            verify(invitationRepository).deleteAll(any());
            verify(outboxService, times(2)).save(any(), any(), any(), any());
            verify(userRepository).deleteById(1L);
        }

        @Test
        void shouldThrowExceptionWhenUserDoesNotExist() {
            when(userRepository.findUserDataById(1L)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.deleteAccount(new ConfirmPasswordDto("password"), 1L));
            verifyNoInteractions(passwordValidator, memberRepository, invitationRepository, outboxService, removalTemplate);
        }

        @Test
        void shouldNotDeleteAccountWhenPasswordValidationFails() {
            UserDataProjection user = mock(UserDataProjection.class);
            when(userRepository.findUserDataById(1L)).thenReturn(Optional.of(user));
            doThrow(new IllegalArgumentException()).when(passwordValidator).validatePassword(eq(1L), any());

            assertThrows(IllegalArgumentException.class, () -> service.deleteAccount(new ConfirmPasswordDto("password"), 1L));
            verify(userRepository, never()).deleteById(1L);
            verifyNoInteractions(memberRepository, invitationRepository, outboxService, removalTemplate);
        }
    }
}
