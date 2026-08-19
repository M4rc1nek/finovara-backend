package com.finovara.authservice.sharedaccount.service.deletion;

import com.finovara.authservice.sharedaccount.context.UserContextLoader;
import com.finovara.authservice.sharedaccount.dto.SharedAccountDetailsDto;
import com.finovara.authservice.sharedaccount.model.SharedAccountMember;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.util.confirmationpassword.service.PasswordValidator;
import com.finovara.authservice.util.deletion.AccountRemovalTemplate;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.activity.event.sharedaccount.SharedAccountActivityEvent;
import com.finovara.contracts.notification.event.sharedaccount.deletion.NotificationSharedAccountLeftEvent;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.activity.SharedAccountActivityType;
import com.finovara.contracts.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedAccountDeletionService {

    private final UserManagerService userManagerService;
    private final PasswordValidator passwordValidator;
    private final OutboxService outboxService;
    private final SharedAccountMemberRepository sharedAccountMemberRepository;
    private final AccountRemovalTemplate accountRemovalTemplate;
    private final UserContextLoader userContextLoader;

    @Transactional
    public void leaveSharedAccount(Long actingUserId, ConfirmPasswordDto dto) {
        User actingUser = userManagerService.getUserByIdOrThrow(actingUserId);

        SharedAccountMember membership = sharedAccountMemberRepository.findByUserId(actingUserId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("User is not a member of any shared account"));

        passwordValidator.validatePassword(actingUser.getId(), dto);

        Long accountId = membership.getSharedAccount().getId();

        try {
            Optional<SharedAccountDetailsDto> removalResult =
                    accountRemovalTemplate.handleSharedAccountRemoval(accountId, actingUserId, actingUser.getUsername());

            Optional<User> coFounder = resolveCoFounder(removalResult, actingUserId);

            notifyCoFounderIfPresent(coFounder, accountId, actingUser);
            publishLeftActivity(actingUserId, coFounder);

        } catch (ObjectOptimisticLockingFailureException exception) {
            log.warn("Concurrent modification detected while leaving shared account, actingUserId={}. " +
                    "The other party likely deleted/left the shared account at the same time.", actingUserId);
            throw exception;
        }
        log.info("User left shared account. actingUserId={}", actingUserId);
    }

    private Optional<User> resolveCoFounder(Optional<SharedAccountDetailsDto> removalResult, Long actingUserId) {
        return removalResult
                .map(userContextLoader::loadUsersContext)
                .map(usersContext -> usersContext.getParticualUser(actingUserId));
    }

    private void notifyCoFounderIfPresent(Optional<User> coFounder, Long accountId, User actingUser) {
        coFounder.ifPresent(recipient ->
                outboxService.save("User", recipient.getId().toString(), "notification.shared-account.left",
                        new NotificationSharedAccountLeftEvent(accountId, recipient.getId(), actingUser.getUsername())));
    }

    private void publishLeftActivity(Long actingUserId, Optional<User> coFounder) {
        String coFounderUsername = coFounder.map(User::getUsername).orElse(null);
        String coFounderEmail = coFounder.map(User::getEmail).orElse(null);

        outboxService.save("User", actingUserId.toString(), "activity.shared-account",
                new SharedAccountActivityEvent(actingUserId, SharedAccountActivityType.LEFT_SHARED_ACCOUNT,
                        null, coFounderUsername, coFounderEmail, LocalDateTime.now()));
    }
}