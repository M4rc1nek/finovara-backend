package com.finovara.authservice.util.deletion;

import com.finovara.authservice.sharedaccount.dto.SharedAccountDetailsDto;
import com.finovara.authservice.sharedaccount.model.SharedAccount;
import com.finovara.authservice.sharedaccount.model.SharedAccountMember;
import com.finovara.authservice.sharedaccount.model.role.SharedRole;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountRepository;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.contracts.event.finance.sharedaccount.SharedAccountDeletedEvent;
import com.finovara.contracts.event.notification.sharedaccount.deletion.NotificationSharedAccountDeletedEvent;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountRemovalTemplate {

    private final UserRepository userRepository;
    private final OutboxService outboxService;
    private final SharedAccountRepository sharedAccountRepository;
    private final SharedAccountMemberRepository sharedAccountMemberRepository;

    @Transactional
    public Optional<SharedAccountDetailsDto> handleSharedAccountRemoval(Long accountId, Long actingUserId, String actingUsername) {
        Optional<SharedAccount> lockedAccount = sharedAccountRepository.findByIdForUpdate(accountId);

        if (lockedAccount.isEmpty()) {
            log.info("Shared account accountId={} was already deleted by a concurrent transaction. Skipping.", accountId);
            return Optional.empty();
        }

        SharedAccountDetailsDto details = getAccountDetails(accountId, actingUserId);

        if (details.ownerId() == null || details.memberId() == null) {
            log.warn("Incomplete shared account members for accountId={}. Skipping event propagation.", accountId);
            sharedAccountMemberRepository.deleteMembersByAccountId(accountId);
            sharedAccountRepository.deleteAccountById(accountId);
            return Optional.empty();
        }

        sharedAccountMemberRepository.deleteMembersByAccountId(accountId);
        sharedAccountRepository.deleteAccountById(accountId);

        clearHasSharedAccountFlag(details.remainingUserId());
        clearHasSharedAccountFlag(actingUserId);

        outboxService.save("User", details.ownerId().toString(), "shared-account.deleted",
                new SharedAccountDeletedEvent(accountId, details.ownerId(), details.memberId(), details.remainingUserId()));

        outboxService.save("User", details.remainingUserId().toString(), "notification.shared-account.deleted",
                new NotificationSharedAccountDeletedEvent(accountId, details.remainingUserId(), actingUsername));

        log.info("Shared account removed, accountId={}, ownerId={}, memberId={}, remainingUserId={}",
                accountId, details.ownerId(), details.memberId(), details.remainingUserId());

        return Optional.of(details);
    }

    private void clearHasSharedAccountFlag(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("User not found, userId=" + userId));
        user.setHasSharedAccount(false);
        userRepository.save(user);
    }

    private SharedAccountDetailsDto getAccountDetails(Long accountId, Long actingUserId) {
        List<SharedAccountMember> members = sharedAccountMemberRepository.findMembersByAccountId(accountId);

        Long ownerId = extractUserIdByRoleSafe(members, SharedRole.OWNER);
        Long memberId = extractUserIdByRoleSafe(members, SharedRole.MEMBER);

        Long remainingUserId = actingUserId.equals(ownerId) ? memberId : ownerId;

        return new SharedAccountDetailsDto(remainingUserId, ownerId, memberId);
    }

    private Long extractUserIdByRoleSafe(List<SharedAccountMember> members, SharedRole role) {
        return members.stream()
                .filter(m -> m.getRole() == role)
                .map(SharedAccountMember::getUserId)
                .findFirst()
                .orElse(null);
    }
}

