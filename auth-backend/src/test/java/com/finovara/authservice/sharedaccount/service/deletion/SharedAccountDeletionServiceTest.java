package com.finovara.authservice.sharedaccount.service.deletion;

import com.finovara.authservice.sharedaccount.model.SharedAccount;
import com.finovara.authservice.sharedaccount.model.SharedAccountMember;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.util.confirmationpassword.service.PasswordValidator;
import com.finovara.authservice.util.deletion.AccountRemovalTemplate;
import com.finovara.authservice.util.deletion.SharedAccountRemovalResult;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.user.authorization.dto.ConfirmPasswordDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedAccountDeletionServiceTest {

    @Mock private UserManagerService userManagerService;
    @Mock private PasswordValidator passwordValidator;
    @Mock private OutboxService outboxService;
    @Mock private SharedAccountMemberRepository memberRepository;
    @Mock private AccountRemovalTemplate removalTemplate;
    @InjectMocks private SharedAccountDeletionService service;

    @Nested
    class LeaveSharedAccount {
        @Test
        void shouldThrowExceptionWhenUserHasNoMembership() {
            when(userManagerService.getUserByIdOrThrow(1L)).thenReturn(User.builder().id(1L).build());
            when(memberRepository.findByUserId(1L)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.leaveSharedAccount(1L, new ConfirmPasswordDto("password")));
            verifyNoInteractions(passwordValidator, removalTemplate, outboxService);
        }

        @Test
        void shouldValidatePasswordRemoveMembershipAndPublishActivityWhenMemberExists() {
            User user = User.builder().id(1L).username("anna").email("a@x.com").build();
            SharedAccountMember member = SharedAccountMember.builder().sharedAccount(SharedAccount.builder().id(10L).build()).build();
            when(userManagerService.getUserByIdOrThrow(1L)).thenReturn(user);
            when(memberRepository.findByUserId(1L)).thenReturn(Optional.of(member));
            when(removalTemplate.handleSharedAccountRemoval(10L, 1L)).thenReturn(Optional.empty());

            service.leaveSharedAccount(1L, new ConfirmPasswordDto("password"));

            verify(passwordValidator).validatePassword(any(), any());
            verify(removalTemplate).handleSharedAccountRemoval(10L, 1L);
            verify(outboxService).save(any(), any(), any(), any());
        }
    }
}
