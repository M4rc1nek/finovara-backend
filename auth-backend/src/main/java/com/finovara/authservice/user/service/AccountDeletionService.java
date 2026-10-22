package com.finovara.authservice.user.service;

import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.authservice.util.confirmationpassword.service.PasswordValidator;
import com.finovara.authservice.util.deletion.AccountRemovalTemplate;
import com.finovara.authservice.util.user.UserDataProjection;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.user.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.user.event.account.UserAccountDeletedEvent;
import com.finovara.contracts.util.notification.ActionEmailEventType;
import com.finovara.contracts.util.notification.event.SendEmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final OutboxService outboxService;
    private final PasswordValidator passwordValidator;
    private final SharedAccountMemberRepository sharedAccountMemberRepository;
    private final SharedAccountInvitationRepository sharedAccountInvitationRepository;
    private final AccountRemovalTemplate accountRemovalTemplate;

    @Transactional
    public void deleteAccount(ConfirmPasswordDto confirmPasswordDto, Long userId) {
        UserDataProjection userData = userRepository.findUserDataById(userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("User not found"));
        String username = userData.getUsername();
        String email = userData.getEmail();

        passwordValidator.validatePassword(userId, confirmPasswordDto);

        try {
            sharedAccountMemberRepository.findByUserId(userId)
                    .ifPresent(member -> accountRemovalTemplate.handleSharedAccountRemovalWithNotification(
                            member.getSharedAccount().getId(), userId, username));
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.warn("Concurrent modification detected while removing shared account for userId={}. " +
                    "The other party likely deleted/left the shared account at the same time.", userId);
            throw ex;
        }

        sharedAccountInvitationRepository.deleteAllInvolvingUsers(List.of(userId));

        outboxService.save("User", userId.toString(), "notification.email.send",
                new SendEmailEvent(userId, username, email, ActionEmailEventType.ACCOUNT_DELETED, Map.of()));
        outboxService.save("User", userId.toString(), "user-account.deleted",
                new UserAccountDeletedEvent(userId));

        userRepository.deleteById(userId);
        log.info("User account has been deleted. userId={}", userId);
    }
}
