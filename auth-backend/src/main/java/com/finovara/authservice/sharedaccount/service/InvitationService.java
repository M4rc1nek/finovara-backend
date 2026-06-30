package com.finovara.authservice.sharedaccount.service;

import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import com.finovara.authservice.sharedaccount.model.dto.InvitationResponse;
import com.finovara.authservice.sharedaccount.model.status.InvitationStatus;
import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.authservice.user.dto.UserDataDto;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.contracts.event.notification.sharedaccount.invitation.UserAcceptSharedAccountInvitationEvent;
import com.finovara.contracts.event.notification.sharedaccount.invitation.UserRejectSharedAccountInvitationEvent;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.exception.forbidden.AccessDeniedException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final UserRepository userRepository;
    private final OutboxService outboxService;
    private final SharedAccountInvitationRepository invitationRepository;

    public List<UserDataDto> searchUser(String query) {
        return userRepository.searchByUsernameOrEmail(query);
    }

    @Transactional
    public void sendInvitation(Long inviterUserId, Long inviteeUserId) {

        invitationRepository.findPendingBetweenUsers(inviterUserId, inviteeUserId)
                .ifPresent(invitation -> {
                    throw new EntityAlreadyExistsException("Invitation already exists between these users!");
                });

        SharedAccountInvitation invitation = SharedAccountInvitation.builder()
                .inviterUserId(inviterUserId)
                .inviteeUserId(inviteeUserId)
                .status(InvitationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        invitationRepository.save(invitation);
    }

    public List<InvitationResponse> getPendingInvitations(Long userId) {
        return invitationRepository.findPendingWithInviterUsername(userId, InvitationStatus.PENDING);
    }


    @Transactional
    public void acceptInvite(Long inviteeUserId, Long invitationId) {
        InvitationContext invitationContext = loadAndValidateInvitation(inviteeUserId, invitationId);

        invitationContext.invitation().setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitationContext.invitation());


        outboxService.save("User", invitationContext.invitation().getInviterUserId().toString(), "user.shared-account.accept-invitation",
                new UserAcceptSharedAccountInvitationEvent(
                        invitationContext.invitation().getInviterUserId(),
                        invitationContext.inviteeUsername()
                ));
    }

    @Transactional
    public void rejectInvite(Long inviteeUserId, Long invitationId) {

        InvitationContext invitationContext = loadAndValidateInvitation(inviteeUserId, invitationId);

        invitationRepository.delete(invitationContext.invitation());

        outboxService.save("User", invitationContext.invitation().getInviterUserId().toString(), "user.shared-account.reject-invitation",
                new UserRejectSharedAccountInvitationEvent(
                        invitationContext.invitation().getInviterUserId(),
                        invitationContext.inviteeUsername()
                ));
    }

    private InvitationContext loadAndValidateInvitation(Long inviteeUserId, Long invitationId) {

        SharedAccountInvitation invitation = invitationRepository.findInvitationForInviteeUser(invitationId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Pending invitation not found"));

        if (!invitation.getInviteeUserId().equals(inviteeUserId)) {
            throw new AccessDeniedException("This invitation does not belong to the current user");
        }

        String inviteeUsername = invitationRepository.findInviteeUsernameByInvitationId(invitationId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Invitee username not found"));

        return new InvitationContext(invitation, inviteeUsername);
    }

    private record InvitationContext(SharedAccountInvitation invitation, String inviteeUsername) {}

}