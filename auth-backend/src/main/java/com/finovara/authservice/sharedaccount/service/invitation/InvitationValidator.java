package com.finovara.authservice.sharedaccount.service.invitation;

import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

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

        if (sharedAccountMemberRepository.existsByUserIdIn(List.of(inviterUserId, inviteeUserId))) {
            log.warn("Rejected invitation: inviterUserId={} or inviteeUserId={} already has a shared account", inviterUserId, inviteeUserId);
            throw new EntityAlreadyExistsException("One of the users already belongs to a shared account");
        }

        sharedAccountInvitationRepository.findInvitationBetweenUsers(inviterUserId, inviteeUserId)
                .ifPresent(invitation -> {
                    log.warn("Rejected invitation: invitation already exists between inviterUserId={} and inviteeUserId={}", inviterUserId, inviteeUserId);
                    throw new EntityAlreadyExistsException("Invitation already exists between these users!");
                });
    }

    public void validateAcceptInvite(Long inviterUserId, Long inviteeUserId, Long invitationId) {
        if (sharedAccountMemberRepository.existsByUserIdIn(List.of(inviterUserId, inviteeUserId))) {
            log.warn("Rejected accept: inviterUserId={} or inviteeUserId={} already has a shared account, invitationId={}",
                    inviterUserId, inviteeUserId, invitationId);
            throw new EntityAlreadyExistsException("One of the users already belongs to a shared account");
        }
    }
}