package com.finovara.authservice.sharedaccount.service.deletion;

import com.finovara.authservice.sharedaccount.model.SharedAccountMember;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.util.confirmationpassword.service.PasswordValidator;
import com.finovara.authservice.util.deletion.AccountRemovalTemplate;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import com.finovara.contracts.event.notification.sharedaccount.deletion.NotificationSharedAccountLeftEvent;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedAccountDeletionService {

    private final UserManagerService userManagerService;
    private final PasswordValidator passwordValidator;
    private final OutboxService outboxService;
    private final SharedAccountMemberRepository sharedAccountMemberRepository;
    private final AccountRemovalTemplate accountRemovalTemplate;

    @Transactional
    public void leaveSharedAccount(Long actingUserId, ConfirmPasswordDto dto) {
        User user = userManagerService.getUserByIdOrThrow(actingUserId);

        SharedAccountMember member = sharedAccountMemberRepository.findByUserId(actingUserId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("User is not a member of any shared account"));

        passwordValidator.validatePassword(user.getId(), dto);

        Long accountId = member.getSharedAccount().getId();

        try {
            accountRemovalTemplate.handleSharedAccountRemoval(accountId, actingUserId, user.getUsername())
                    .ifPresent(d -> outboxService.save("User", d.remainingUserId().toString(), "notification.shared-account.left",
                            new NotificationSharedAccountLeftEvent(accountId, d.remainingUserId(), user.getUsername())));
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.warn("Concurrent modification detected while leaving shared account, actingUserId={}. " +
                    "The other party likely deleted/left the shared account at the same time.", actingUserId);
            throw ex;
        }

        log.info("User left shared account. actingUserId={}", actingUserId);
    }
}