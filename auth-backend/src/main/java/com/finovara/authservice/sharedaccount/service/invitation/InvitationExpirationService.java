package com.finovara.authservice.sharedaccount.service.invitation;

import com.finovara.authservice.sharedaccount.dto.ExpiredInvitationDto;
import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.event.invitation.SharedAccountInvitationExpiredEvent;
import com.finovara.contracts.user.authorization.dto.UserDataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationExpirationService {

    private final SharedAccountInvitationRepository sharedAccountInvitationRepository;
    private final OutboxService outboxService;

    @Transactional
    public int expireBatch(LocalDateTime now, int batchSize) {
        List<ExpiredInvitationDto> expired =
                sharedAccountInvitationRepository.findExpired(now, PageRequest.of(0, batchSize));

        if (expired.isEmpty()) {
            return 0;
        }

        sharedAccountInvitationRepository.deleteByIds(expired.stream().map(ExpiredInvitationDto::id).toList());

        expired.forEach(invitation ->
                outboxService.save("User", invitation.inviterUserId().toString(),
                        "notification.shared-account.invitation-expired",
                        new SharedAccountInvitationExpiredEvent(invitation.inviterUserId(), invitation.inviteeUsername())));

        return expired.size();
    }
}