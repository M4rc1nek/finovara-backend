package com.finovara.authservice.sharedaccount.service.invitation;

import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import com.finovara.authservice.sharedaccount.model.SharedAccount;
import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountRepository;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.mainaccount.activity.event.sharedaccount.SharedAccountChangeHistoryActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.SharedAccountChangeHistoryActivityType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.event.UsersCreatedSharedAccountEvent;
import com.finovara.contracts.sharedaccount.event.invitation.UserAcceptSharedAccountInvitationEvent;
import com.finovara.contracts.sharedaccount.event.invitation.UserRejectSharedAccountInvitationEvent;
import com.finovara.contracts.sharedaccount.event.settings.SharedAccountCreateDefaultSettingsEvent;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.user.authorization.dto.UserDataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationResponseService {

    private final SharedAccountInvitationRepository sharedAccountInvitationRepository;
    private final SharedAccountRepository sharedAccountRepository;
    private final InvitationValidator invitationValidator;
    private final SharedAccountMemberService sharedAccountMemberService;
    private final UserRepository userRepository;
    private final OutboxService outboxService;
    private final AdditionalAuthorizationService additionalAuthorizationService;
    private final AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Transactional
    public void acceptInvite(Long inviteeUserId, Long invitationId, String authorizationCode) {
        additionalAuthorizationService.confirmAdditionalAuthorizationCode(inviteeUserId, additionalAuthorizationCodeResolver.resolve(authorizationCode));

        SharedAccountInvitation invitation = loadActiveInvitation(inviteeUserId, invitationId);

        Long inviterUserId = invitation.getInviterUserId();

        invitationValidator.validateAcceptInvite(inviterUserId, inviteeUserId, invitationId);

        Map<Long, UserDataDto> users = loadUsers(inviterUserId, inviteeUserId);
        UserDataDto inviter = requireUser(users, inviterUserId);
        UserDataDto invitee = requireUser(users, inviteeUserId);

        consumeInvitation(invitation);

        SharedAccount sharedAccount = sharedAccountRepository.save(
                SharedAccount.builder()
                        .createdAt(LocalDateTime.now())
                        .build());

        try {
            sharedAccountMemberService.createOwnerAndMember(sharedAccount, inviterUserId, inviteeUserId);
        } catch (DataIntegrityViolationException e) {
            throw new EntityAlreadyExistsException("One of the users already belongs to a shared account");
        }
        List<SharedAccountInvitation> sharedAccountInvitations = sharedAccountInvitationRepository.findAllInvolvingUsers(List.of(inviterUserId, inviteeUserId));
        sharedAccountInvitationRepository.deleteAll(sharedAccountInvitations);

        log.info("Accepted invitation id={}, created sharedAccountId={} for inviterUserId={} and inviteeUserId={}",
                invitationId, sharedAccount.getId(), inviterUserId, inviteeUserId);

        outboxService.save("User", inviterUserId.toString(),
                "finance.shared-account.invitation-accepted",
                new UsersCreatedSharedAccountEvent(inviterUserId, inviteeUserId));

        outboxService.save("User", inviteeUserId.toString(),
                "finance.shared-account.create-default-settings",
                new SharedAccountCreateDefaultSettingsEvent(inviterUserId, inviteeUserId));

        outboxService.save("User", inviteeUserId.toString(), "shared-account.changed",
                new SharedAccountChangeHistoryActivityEvent(
                        inviteeUserId, SharedAccountChangeHistoryActivityType.ACCEPTED_INVITATION, null,
                        inviter.username(), inviter.email(),
                        LocalDateTime.now()));

        outboxService.save("User", inviterUserId.toString(),
                "notification.shared-account.invitation-accepted",
                new UserAcceptSharedAccountInvitationEvent(inviterUserId, invitee.username()));
    }

    @Transactional
    public void rejectInvite(Long inviteeUserId, Long invitationId, String authorizationCode) {
        additionalAuthorizationService.confirmAdditionalAuthorizationCode(inviteeUserId, additionalAuthorizationCodeResolver.resolve(authorizationCode));

        SharedAccountInvitation invitation = loadActiveInvitation(inviteeUserId, invitationId);

        Long inviterUserId = invitation.getInviterUserId();

        Map<Long, UserDataDto> users = loadUsers(inviterUserId, inviteeUserId);
        UserDataDto inviter = requireUser(users, inviterUserId);
        UserDataDto invitee = requireUser(users, inviteeUserId);

        consumeInvitation(invitation);

        outboxService.save("User", inviterUserId.toString(), "user.shared-account.reject-invitation",
                new UserRejectSharedAccountInvitationEvent(inviterUserId, invitee.username()));

        outboxService.save("User", inviteeUserId.toString(), "shared-account.changed",
                new SharedAccountChangeHistoryActivityEvent(
                        inviteeUserId, SharedAccountChangeHistoryActivityType.REJECTED_INVITATION, null,
                        inviter.username(), inviter.email(),
                        LocalDateTime.now()));

        log.info("Rejected invitation id={}, inviterUserId={}, inviteeUserId={}",
                invitationId, inviterUserId, inviteeUserId);
    }

    private SharedAccountInvitation loadActiveInvitation(Long inviteeUserId, Long invitationId) {
        SharedAccountInvitation invitation = sharedAccountInvitationRepository
                .findByIdAndInviteeUserId(invitationId, inviteeUserId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Pending invitation not found"));

        if (invitation.hasExpired()) {
            throw new RequestedEntityNotFoundException("Pending invitation not found");
        }

        return invitation;
    }

    private void consumeInvitation(SharedAccountInvitation invitation) {
        try {
            sharedAccountInvitationRepository.delete(invitation);
            sharedAccountInvitationRepository.flush();
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new RequestedEntityNotFoundException("Pending invitation not found");
        }
    }

    private Map<Long, UserDataDto> loadUsers(Long firstUserId, Long secondUserId) {
        return userRepository.findBasicInfoByIds(List.of(firstUserId, secondUserId))
                .stream()
                .collect(Collectors.toMap(UserDataDto::id, Function.identity()));
    }

    private UserDataDto requireUser(Map<Long, UserDataDto> users, Long userId) {
        UserDataDto user = users.get(userId);
        if (user == null) {
            throw new RequestedEntityNotFoundException("User not found, userId=" + userId);
        }
        return user;
    }
}