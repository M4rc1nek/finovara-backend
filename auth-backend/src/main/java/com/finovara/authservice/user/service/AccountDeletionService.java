package com.finovara.authservice.user.service;

import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.authservice.util.confirmationpassword.service.PasswordValidator;
import com.finovara.authservice.util.deletion.AccountRemovalTemplate;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import com.finovara.contracts.event.notification.SendEmailEvent;
import com.finovara.contracts.event.user.delete.account.UserAccountDeletedEvent;
import com.finovara.contracts.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final UserManagerService userManagerService;
    private final OutboxService outboxService;
    private final PasswordValidator passwordValidator;
    private final SharedAccountMemberRepository sharedAccountMemberRepository;
    private final AccountRemovalTemplate accountRemovalTemplate;

    @Transactional
    public void deleteAccount(ConfirmPasswordDto confirmPasswordDto, Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);

        passwordValidator.validatePassword(userId, confirmPasswordDto);

        try {
            sharedAccountMemberRepository.findByUserId(userId)
                    .ifPresent(member -> accountRemovalTemplate.handleSharedAccountRemovalWithNotification(
                            member.getSharedAccount().getId(), userId, user.getUsername()));

        } catch (ObjectOptimisticLockingFailureException ex) {
            log.warn("Concurrent modification detected while removing shared account for userId={}. " +
                    "The other party likely deleted/left the shared account at the same time.", userId);
            throw ex;
        }

        outboxService.save("User", userId.toString(), "notification.email.send",
                new SendEmailEvent(user.getId(), user.getUsername(), user.getEmail(), "Finovara - Usunięcie konta", "email/account-deleted.html"));
        outboxService.save("User", userId.toString(), "user-account.deleted",
                new UserAccountDeletedEvent(user.getId()));

        userRepository.delete(user);
        log.info("User account has been deleted. User email: {}", user.getEmail());
    }
}