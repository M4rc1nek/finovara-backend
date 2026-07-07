package com.finovara.authservice.user.service;

import com.finovara.authservice.sharedaccount.model.SharedAccountMember;
import com.finovara.authservice.sharedaccount.model.role.SharedRole;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountRepository;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.authservice.util.confirmationpassword.service.PasswordValidator;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import com.finovara.contracts.event.notification.SendEmailEvent;
import com.finovara.contracts.event.user.delete.account.SharedAccountDeletedEvent;
import com.finovara.contracts.event.user.delete.account.UserAccountDeletedEvent;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeleteService {

    private final UserRepository userRepository;
    private final UserManagerService userManagerService;
    private final OutboxService outboxService;
    private  final PasswordValidator passwordValidator;
    private  final SharedAccountRepository sharedAccountRepository;
    private  final SharedAccountMemberRepository   sharedAccountMemberRepository;

    @Transactional
    public void deleteAccount(ConfirmPasswordDto confirmPasswordDto, Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);

        passwordValidator.validatePassword(userId, confirmPasswordDto);

        sharedAccountMemberRepository.findByUserId(userId)
                .ifPresent(member -> handleSharedAccountDeletion(member.getSharedAccount().getId()));

        outboxService.save("User", userId.toString(), "notification.email.send",
                new SendEmailEvent(user.getId(), user.getUsername(), user.getEmail(), "Finovara - Usuniecie konta", "email/account-deleted.html"));
        outboxService.save("User", userId.toString(), "user-account.deleted",
                new UserAccountDeletedEvent(user.getId()));

        userRepository.delete(user);
        log.info("User account has been deleted. User email: {}", user.getEmail());
    }

    private void handleSharedAccountDeletion(Long accountId) {
        List<SharedAccountMember> members = sharedAccountMemberRepository.findMembersByAccountId(accountId);

        Long ownerId = extractUserIdByRole(members, SharedRole.OWNER);
        Long memberId = extractUserIdByRole(members, SharedRole.MEMBER);

        sharedAccountMemberRepository.deleteMembersByAccountId(accountId);
        sharedAccountRepository.deleteAccountById(accountId);

        outboxService.save("User", ownerId.toString(), "shared-account.deleted",
                new SharedAccountDeletedEvent(accountId, ownerId, memberId));

        log.info("Shared account deleted, accountId={}, ownerId={}, memberId={}", accountId, ownerId, memberId);
    }

    private Long extractUserIdByRole(List<SharedAccountMember> members, SharedRole role) {
        return members.stream()
                .filter(m -> m.getRole() == role)
                .map(SharedAccountMember::getUserId)
                .findFirst()
                .orElseThrow(() -> new RequestedEntityNotFoundException(
                        "Shared account member with role " + role + " not found"));
    }
}
