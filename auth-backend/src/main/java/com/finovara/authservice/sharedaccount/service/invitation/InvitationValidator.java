package com.finovara.authservice.sharedaccount.service.invitation;

import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.exception.forbidden.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvitationValidator {

    private final SharedAccountMemberRepository sharedAccountMemberRepository;
    private final SharedAccountInvitationRepository sharedAccountInvitationRepository;

    public void validateSendInvitation(Long inviterUserId, Long inviteeUserId) {
        if (inviterUserId.equals(inviteeUserId)) {
            log.warn("Rejected invitation: userId={} tried to invite themselves", inviterUserId);
            throw new InvalidInputException("You cannot invite yourself");
        }

        if (sharedAccountMemberRepository.existsByUserId(inviterUserId) || sharedAccountMemberRepository.existsByUserId(inviteeUserId)) {
            log.warn("Rejected invitation: inviterUserId={} or inviteeUserId={} already has a shared account", inviterUserId, inviteeUserId);
            throw new EntityAlreadyExistsException("One of the users already belongs to a shared account");
        }

        sharedAccountInvitationRepository.findInvitationBetweenUsers(inviterUserId, inviteeUserId)
                .ifPresent(invitation -> {
                    log.warn("Rejected invitation: invitation already exists between inviterUserId={} and inviteeUserId={}", inviterUserId, inviteeUserId);
                    throw new EntityAlreadyExistsException("Invitation already exists between these users!");
                });
    }


    public void validateAcceptInvite(Long inviterUserId, Long inviteeUserId, Long invitationId){
        if (sharedAccountMemberRepository.existsByUserId(inviterUserId) || sharedAccountMemberRepository.existsByUserId(inviteeUserId)) {
            log.warn("Rejected accept: inviterUserId={} or inviteeUserId={} already has a shared account, invitationId={}",
                    inviterUserId, inviteeUserId, invitationId);
            throw new EntityAlreadyExistsException("One of the users already belongs to a shared account");
        }
    }

    public void validateInvitationOwnership(Long actualInviteeUserId, Long callerUserId, Long invitationId) {
        if (!actualInviteeUserId.equals(callerUserId)) {
            log.warn("Access denied: userId={} tried to act on invitationId={} belonging to inviteeUserId={}",
                    callerUserId, invitationId, actualInviteeUserId);
            throw new AccessDeniedException("This invitation does not belong to the current user");
        }
    }


    public void validateMembership(Long accountId, Long callerId) {
        boolean isMember = sharedAccountMemberRepository.findByUserId(callerId)
                .map(member -> member.getSharedAccount().getId().equals(accountId))
                .orElse(false);

        if (!isMember) {
            log.warn("Access denied: userId={} tried to access members of sharedAccountId={} they don't belong to", callerId, accountId);
            throw new AccessDeniedException("You are not a member of this shared account");
        }
    }
}