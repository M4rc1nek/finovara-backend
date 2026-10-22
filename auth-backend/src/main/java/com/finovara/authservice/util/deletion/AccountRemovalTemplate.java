package com.finovara.authservice.util.deletion;

import com.finovara.authservice.sharedaccount.context.SharedAccountUsers;
import com.finovara.authservice.sharedaccount.context.UserContextLoader;
import com.finovara.authservice.sharedaccount.dto.SharedAccountDetailsDto;
import com.finovara.authservice.sharedaccount.model.SharedAccountMember;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountRepository;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.SharedRole;
import com.finovara.contracts.sharedaccount.event.deletion.NotificationSharedAccountDeletedEvent;
import com.finovara.contracts.sharedaccount.event.deletion.SharedAccountDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountRemovalTemplate {

    private final UserRepository userRepository;
    private final OutboxService outboxService;
    private final SharedAccountRepository sharedAccountRepository;
    private final SharedAccountMemberRepository sharedAccountMemberRepository;
    private final UserContextLoader userContextLoader;

    @Transactional
    public Optional<SharedAccountRemovalResult> handleSharedAccountRemovalWithNotification(
            Long accountId, Long actingUserId, String actingUsername) {

        Optional<SharedAccountRemovalResult> result = handleSharedAccountRemoval(accountId, actingUserId);

        result.ifPresent(removal -> {
            Long remainingUserId = removal.details().remainingUserId();
            outboxService.save("User", remainingUserId.toString(), "notification.shared-account.deleted",
                    new NotificationSharedAccountDeletedEvent(accountId, remainingUserId, actingUsername));
        });
        return result;
    }

    @Transactional
    public Optional<SharedAccountRemovalResult> handleSharedAccountRemoval(Long accountId, Long actingUserId) {
        if (sharedAccountRepository.findByIdForUpdate(accountId).isEmpty()) {
            log.info("Shared account accountId={} was already deleted by a concurrent transaction. Skipping.", accountId);
            return Optional.empty();
        }

        SharedAccountDetailsDto details = getAccountDetails(accountId, actingUserId);

        if (details.ownerId() == null || details.memberId() == null) {
            log.warn("Incomplete shared account members for accountId={}. Skipping event propagation.", accountId);
            deleteAccount(accountId);
            clearHasSharedAccountFlags(Stream.of(details.ownerId(), details.memberId()).filter(Objects::nonNull).toList());
            return Optional.empty();
        }

        SharedAccountUsers users = userContextLoader.loadUsersContext(details);

        outboxService.save("User", details.ownerId().toString(), "shared-account.deleted",
                new SharedAccountDeletedEvent(accountId, details.ownerId(), details.memberId(), details.remainingUserId(),
                        users.owner().getUsername(), users.owner().getEmail(),
                        users.member().getUsername(), users.member().getEmail()));

        deleteAccount(accountId);
        clearHasSharedAccountFlags(List.of(details.ownerId(), details.memberId()));

        log.info("Shared account removed, accountId={}, ownerId={}, memberId={}, remainingUserId={}",
                accountId, details.ownerId(), details.memberId(), details.remainingUserId());

        return Optional.of(new SharedAccountRemovalResult(details, users));
    }

    private void deleteAccount(Long accountId) {
        sharedAccountMemberRepository.deleteMembersByAccountId(accountId);
        sharedAccountRepository.deleteAccountById(accountId);
    }

    private void clearHasSharedAccountFlags(List<Long> userIds) {
        if (!userIds.isEmpty()) {
            userRepository.updateHasSharedAccount(userIds, false);
        }
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