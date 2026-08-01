package com.finovara.authservice.sharedaccount.service.invitation;

import com.finovara.authservice.sharedaccount.model.SharedAccount;
import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import com.finovara.authservice.sharedaccount.model.role.SharedRole;
import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountRepository;
import com.finovara.authservice.user.dto.UserDataDto;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.event.activity.sharedaccount.SharedAccountActivityEvent;
import com.finovara.contracts.event.finance.sharedaccount.UsersCreatedSharedAccountEvent;
import com.finovara.contracts.event.notification.sharedaccount.invitation.UserAcceptSharedAccountInvitationEvent;
import com.finovara.contracts.event.notification.sharedaccount.invitation.UserRejectSharedAccountInvitationEvent;
import com.finovara.contracts.event.finance.sharedaccount.SharedAccountCreateDefaultSettingsEvent;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.activity.SharedAccountActivityType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import com.finovara.contracts.auth.dto.ConfirmAuthorizationCodeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationResponseService {

    private final SharedAccountInvitationRepository sharedAccountInvitationRepository;
    private final SharedAccountRepository sharedAccountRepository;
    private final InvitationValidator invitationValidator;
    private final InvitationExpirationService invitationExpirationService;
    private final SharedAccountMemberService sharedAccountMemberService;
    private final UserManagerService userManagerService;
    private final OutboxService outboxService;
    private final AdditionalAuthorizationService additionalAuthorizationService;

    @Transactional
    public void acceptInvite(Long inviteeUserId, Long invitationId, String authorizationCode) {
        additionalAuthorizationService.confirmAdditionalAuthorizationCode(inviteeUserId, new ConfirmAuthorizationCodeDto(authorizationCode));
        
        SharedAccountInvitation invitation = loadAndValidateInvitation(inviteeUserId, invitationId);

        Long inviterUserId = invitation.getInviterUserId();

        invitationValidator.validateAcceptInvite(inviterUserId, inviteeUserId, invitationId);

        UserDataDto inviter = userManagerService.getUserDataWithProfileImg(inviterUserId);
        UserDataDto invitee = userManagerService.getUserDataWithProfileImg(inviteeUserId);

        sharedAccountInvitationRepository.delete(invitation);

        SharedAccount sharedAccount = sharedAccountRepository.save(
                SharedAccount.builder()
                        .createdAt(LocalDateTime.now())
                        .build());

        sharedAccountMemberService.createMember(sharedAccount, inviterUserId, SharedRole.OWNER);
        sharedAccountMemberService.createMember(sharedAccount, inviteeUserId, SharedRole.MEMBER);

        log.info("Accepted invitation id={}, created sharedAccountId={} for inviterUserId={} and inviteeUserId={}",
                invitationId, sharedAccount.getId(), inviterUserId, inviteeUserId);

        outboxService.save("User", inviterUserId.toString(),
                "finance.shared-account.invitation-accepted",
                new UsersCreatedSharedAccountEvent(inviterUserId, inviteeUserId));

        outboxService.save("User", inviteeUserId.toString(),
                "finance.shared-account.create-default-settings",
                new SharedAccountCreateDefaultSettingsEvent(inviterUserId, inviteeUserId));

        outboxService.save("User", inviteeUserId.toString(), "activity.shared-account",
                new SharedAccountActivityEvent(
                        inviteeUserId, SharedAccountActivityType.ACCEPTED_INVITATION, null,
                        inviter.username(), inviter.email(),
                        LocalDateTime.now()));

        outboxService.save("User", inviterUserId.toString(),
                "notification.shared-account.invitation-accepted",
                new UserAcceptSharedAccountInvitationEvent(inviterUserId, invitee.username()));
    }

    @Transactional
    public void rejectInvite(Long inviteeUserId, Long invitationId, String authorizationCode) {
        additionalAuthorizationService.confirmAdditionalAuthorizationCode(inviteeUserId, new ConfirmAuthorizationCodeDto(authorizationCode));
        
        SharedAccountInvitation invitation = loadAndValidateInvitation(inviteeUserId, invitationId);

        Long inviterUserId = invitation.getInviterUserId();

        UserDataDto invitee = userManagerService.getUserDataWithProfileImg(inviteeUserId);
        UserDataDto inviter = userManagerService.getUserDataWithProfileImg(inviterUserId);

        sharedAccountInvitationRepository.delete(invitation);

        outboxService.save("User", inviterUserId.toString(), "user.shared-account.reject-invitation",
                new UserRejectSharedAccountInvitationEvent(inviterUserId, invitee.username()));

        outboxService.save("User", inviteeUserId.toString(), "activity.shared-account",
                new SharedAccountActivityEvent(
                        inviteeUserId, SharedAccountActivityType.REJECTED_INVITATION, null,
                        inviter.username(), inviter.email(),
                        LocalDateTime.now()));

        log.info("Rejected invitation id={}, inviterUserId={}, inviteeUserId={}",
                invitationId, inviterUserId, inviteeUserId);
    }

    private SharedAccountInvitation loadAndValidateInvitation(Long inviteeUserId, Long invitationId) {
        SharedAccountInvitation invitation = sharedAccountInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Pending invitation not found"));

        invitationValidator.validateInvitationOwnership(invitation.getInviteeUserId(), inviteeUserId, invitationId);

        if (invitation.hasExpired()) {
            invitationExpirationService.expireInvitation(invitation);
            throw new RequestedEntityNotFoundException("This invitation has expired");
        }

        return invitation;
    }

}