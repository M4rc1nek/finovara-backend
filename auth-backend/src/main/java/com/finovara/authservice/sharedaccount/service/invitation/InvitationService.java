package com.finovara.authservice.sharedaccount.service.invitation;

import com.finovara.authservice.sharedaccount.dto.InvitationResponse;
import com.finovara.authservice.sharedaccount.dto.SharedAccountStatusDto;
import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.user.authorization.dto.UserDataDto;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.contracts.mainaccount.activity.model.SharedAccountChangeHistoryActivityType;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.mainaccount.activity.event.sharedaccount.SharedAccountChangeHistoryActivityEvent;
import com.finovara.contracts.sharedaccount.event.invitation.UserSentSharedAccountInvitationEvent;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationService {

    private final SharedAccountMemberRepository sharedAccountMemberRepository;
    private final OutboxService outboxService;
    private final UserRepository userRepository;
    private final SharedAccountInvitationRepository sharedAccountInvitationRepository;
    private final InvitationValidator invitationValidator;
    private final AdditionalAuthorizationService additionalAuthorizationService;
    private final AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Value("${shared-account.search.page-size}")
    private int pageSize;

    @Value("${shared-account.invitation.page-size}")
    private int invitationPageSize;

    @Value("${shared-account.min-search-query}")
    private int minSearchQueryLength;

    @Value("${shared-account.invitation.expiration-hours}")
    private int invitationExpirationHours;

    @Transactional(readOnly = true)
    public List<UserDataDto> searchUser(String query, Long currentUserId, int page) {
        if (query == null || query.trim().length() < minSearchQueryLength) {
            return List.of();
        }
        return userRepository.searchByUsernameOrEmail(query.trim(), currentUserId, PageRequest.of(page, pageSize));
    }

    public SharedAccountStatusDto hasSharedAccount(Long userId) {
        return sharedAccountMemberRepository.findByUserId(userId)
                .map(member -> new SharedAccountStatusDto(true, member.getSharedAccount().getId()))
                .orElseGet(() -> new SharedAccountStatusDto(false, null));
    }

    @Transactional
    public void sendInvitation(Long inviterUserId, Long inviteeUserId, String authorizationCode) {
        additionalAuthorizationService.confirmAdditionalAuthorizationCode(inviterUserId, additionalAuthorizationCodeResolver.resolve(authorizationCode));

        LocalDateTime now = LocalDateTime.now();
        invitationValidator.validateSendInvitation(inviterUserId, inviteeUserId);

        Map<Long, UserDataDto> usersById = userRepository.findBasicInfoByIds(List.of(inviterUserId, inviteeUserId))
                .stream()
                .collect(Collectors.toMap(UserDataDto::id, Function.identity()));

        UserDataDto inviter = Optional.ofNullable(usersById.get(inviterUserId))
                .orElseThrow(() -> new RequestedEntityNotFoundException("Inviter not found"));

        UserDataDto invitee = Optional.ofNullable(usersById.get(inviteeUserId))
                .orElseThrow(() -> new RequestedEntityNotFoundException("Invitee not found"));

        SharedAccountInvitation invitation = SharedAccountInvitation.builder()
                .inviterUserId(inviterUserId)
                .inviteeUserId(inviteeUserId)
                .expiresAt(now.plusHours(invitationExpirationHours))
                .createdAt(now)
                .build();

        try {
            sharedAccountInvitationRepository.save(invitation);
        } catch (DataIntegrityViolationException exception) {
            throw new EntityAlreadyExistsException("Invitation already exists between these users!");
        }

        outboxService.save("User", inviterUserId.toString(), "shared-account.changed",
                new SharedAccountChangeHistoryActivityEvent(inviterUserId, SharedAccountChangeHistoryActivityType.SENT_INVITATION, null, invitee.username(), invitee.email(), now));

        outboxService.save("User", inviterUserId.toString(), "notification.shared-account.invitation-sent",
                new UserSentSharedAccountInvitationEvent(inviteeUserId, inviter.username()));

        log.info("Created invitation id={}, inviterUserId={}, inviteeUserId={}", invitation.getId(), inviterUserId, inviteeUserId);
    }

    public List<InvitationResponse> getPendingInvitations(Long userId, int page) {
        return sharedAccountInvitationRepository.findInvitationWithInviterUsername(userId, PageRequest.of(page, invitationPageSize));
    }
}