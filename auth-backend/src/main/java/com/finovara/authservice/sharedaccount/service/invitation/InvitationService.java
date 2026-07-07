package com.finovara.authservice.sharedaccount.service.invitation;

import com.finovara.authservice.sharedaccount.dto.InvitationResponse;
import com.finovara.authservice.sharedaccount.dto.SharedAccountMemberDto;
import com.finovara.authservice.sharedaccount.dto.SharedAccountStatusDto;
import com.finovara.authservice.sharedaccount.model.SharedAccount;
import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import com.finovara.authservice.sharedaccount.model.SharedAccountMember;
import com.finovara.authservice.sharedaccount.model.role.SharedRole;
import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountRepository;
import com.finovara.authservice.user.dto.UserDataDto;
import com.finovara.authservice.user.mapper.UserDataMapper;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.contracts.event.finance.sharedaccount.UsersCreatedSharedAccountEvent;
import com.finovara.contracts.event.notification.sharedaccount.invitation.UserAcceptSharedAccountInvitationEvent;
import com.finovara.contracts.event.notification.sharedaccount.invitation.UserRejectSharedAccountInvitationEvent;
import com.finovara.contracts.event.notification.sharedaccount.invitation.UserSentSharedAccountInvitationEvent;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationService {

    private final UserDataMapper userDataMapper;
    private final SharedAccountMemberRepository sharedAccountMemberRepository;
    private final OutboxService outboxService;
    private final UserRepository userRepository;
    private final SharedAccountRepository sharedAccountRepository;
    private final SharedAccountInvitationRepository sharedAccountInvitationRepository;
    private final InvitationValidator invitationValidator;

    @Value("${shared-account.search.page-size}")
    private int pageSize;

    public List<UserDataDto> searchUser(String query, Long currentUserId) {
        return userRepository.searchByUsernameOrEmail(query, PageRequest.of(0, pageSize))
                .stream()
                .filter(user -> !user.id().equals(currentUserId))
                .map(userDataMapper::mapToUserData)
                .toList();
    }

    public SharedAccountStatusDto hasSharedAccount(Long userId) {
        return sharedAccountMemberRepository.findByUserId(userId)
                .map(member -> new SharedAccountStatusDto(true, member.getSharedAccount().getId()))
                .orElseGet(() -> new SharedAccountStatusDto(false, null));
    }

    @Transactional
    public void sendInvitation(Long inviterUserId, Long inviteeUserId) {
        invitationValidator.validateSendInvitation(inviterUserId, inviteeUserId);

        String inviterUsername = userRepository.findUsernameById(inviterUserId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Invitee username not found"));

        SharedAccountInvitation invitation = SharedAccountInvitation.builder()
                .inviterUserId(inviterUserId)
                .inviteeUserId(inviteeUserId)
                .createdAt(LocalDateTime.now())
                .build();

        sharedAccountInvitationRepository.save(invitation);

        outboxService.save("User", inviterUserId.toString(), "notification.shared-account.invitation-sent",
                new UserSentSharedAccountInvitationEvent(inviteeUserId, inviterUsername));
        log.info("Created invitation id={}, inviterUserId={}, inviteeUserId={}", invitation.getId(), inviterUserId, inviteeUserId);

    }

    public List<InvitationResponse> getPendingInvitations(Long userId) {
        return sharedAccountInvitationRepository.findInvitationWithInviterUsername(userId);
    }

    @Transactional
    public void acceptInvite(Long inviteeUserId, Long invitationId) {
        InvitationContext invitationContext = loadAndValidateInvitation(inviteeUserId, invitationId);

        Long inviterUserId = invitationContext.invitation().getInviterUserId();

        invitationValidator.validateAcceptInvite(inviterUserId, inviteeUserId, invitationId);

        sharedAccountInvitationRepository.delete(invitationContext.invitation());

        SharedAccount sharedAccount = sharedAccountRepository.save(
                SharedAccount.builder()
                        .createdAt(LocalDateTime.now())
                        .build());

        createMember(sharedAccount, inviterUserId, SharedRole.OWNER);
        createMember(sharedAccount, inviteeUserId, SharedRole.MEMBER);

        log.info("Accepted invitation id={}, created sharedAccountId={} for inviterUserId={} and inviteeUserId={}",
                invitationId, sharedAccount.getId(), inviterUserId, inviteeUserId);

        outboxService.save("User", inviterUserId.toString(),
                "finance.shared-account.invitation-accepted",
                new UsersCreatedSharedAccountEvent(inviterUserId, inviteeUserId));

        outboxService.save("User", inviterUserId.toString(),
                "notification.shared-account.invitation-accepted",
                new UserAcceptSharedAccountInvitationEvent(inviterUserId, invitationContext.inviteeUsername()));
    }

    @Transactional
    public void rejectInvite(Long inviteeUserId, Long invitationId) {

        InvitationContext invitationContext = loadAndValidateInvitation(inviteeUserId, invitationId);

        sharedAccountInvitationRepository.delete(invitationContext.invitation());

        log.info("Rejected invitation id={}, inviterUserId={}, inviteeUserId={}",
                invitationId, invitationContext.invitation().getInviterUserId(), inviteeUserId);

        outboxService.save("User", invitationContext.invitation().getInviterUserId().toString(), "user.shared-account.reject-invitation",
                new UserRejectSharedAccountInvitationEvent(
                        invitationContext.invitation().getInviterUserId(),
                        invitationContext.inviteeUsername()
                ));
    }

    public List<SharedAccountMemberDto> getMemberDetails(Long accountId, Long callerId) {
        invitationValidator.validateMembership(accountId, callerId);

        return sharedAccountMemberRepository.findMembersByAccountId(accountId).stream()
                .map(member -> userDataMapper.toSharedAccountMemberDto(
                        member, userRepository.getReferenceById(member.getUserId())))
                .toList();
    }

    private InvitationContext loadAndValidateInvitation(Long inviteeUserId, Long invitationId) {

        SharedAccountInvitation invitation = sharedAccountInvitationRepository.findInvitationForInviteeUser(invitationId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Pending invitation not found"));

        invitationValidator.validateInvitationOwnership(invitation, inviteeUserId);

        String inviteeUsername = sharedAccountInvitationRepository.findInviteeUsernameByInvitationId(invitationId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Invitee username not found"));

        return new InvitationContext(invitation, inviteeUsername);
    }

    private void createMember(SharedAccount sharedAccount, Long userId, SharedRole role) {
        User user = userRepository.getReferenceById(userId);
        sharedAccountMemberRepository.save(
                SharedAccountMember.builder()
                        .sharedAccount(sharedAccount)
                        .userId(userId)
                        .role(role)
                        .joinedAt(LocalDateTime.now())
                        .build());
        user.setHasSharedAccount(true);
    }

    private record InvitationContext(SharedAccountInvitation invitation, String inviteeUsername) {
    }

}