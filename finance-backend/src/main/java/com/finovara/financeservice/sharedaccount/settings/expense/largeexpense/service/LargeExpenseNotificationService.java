package com.finovara.financeservice.sharedaccount.settings.expense.largeexpense.service;

import com.finovara.contracts.sharedaccount.event.notification.LargeExpenseNotificationEvent;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.financeservice.sharedaccount.expense.model.SharedExpense;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettings;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettingsRepository;
import com.finovara.financeservice.sharedaccount.settings.expense.largeexpense.dto.LargeExpenseNotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class LargeExpenseNotificationService {

    private final SharedAccountSettingsRepository sharedAccountSettingsRepository;
    private final OutboxService outboxService;
    private final SharedAccountParticipantsService sharedAccountParticipantsService;

    @Transactional
    public void saveLargeExpenseNotification(Long userId, LargeExpenseNotificationDto largeExpenseNotificationDto) {
        SharedAccountSettings sharedAccountSettings = sharedAccountSettingsRepository.findByUserId(userId);

        boolean changed = sharedAccountSettings.isLargeExpenseNotificationEnabled() != largeExpenseNotificationDto.largeExpenseNotificationEnabled()
                || !Objects.equals(sharedAccountSettings.getLargeExpenseNotificationThreshold(), largeExpenseNotificationDto.largeExpenseNotificationThreshold());

        if (changed) {
            sharedAccountSettings.setLargeExpenseNotificationEnabled(largeExpenseNotificationDto.largeExpenseNotificationEnabled());
            sharedAccountSettings.setLargeExpenseNotificationThreshold(largeExpenseNotificationDto.largeExpenseNotificationThreshold());

            SharedAccountParticipantsResponse participants = sharedAccountParticipantsService.getParticipants(userId);
            outboxService.save("SharedAccountSettings", userId.toString(), "shared-account.activity",
                    new SharedAccountActivityLogEvent(participants.ownerId(), participants.memberId(), userId, null, SharedAccountActivityLogType.SETTING_CHANGED, LocalDateTime.now()));

            log.info("Updated large expense notification settings userId={}, enabled={}, threshold={}", userId, sharedAccountSettings.isLargeExpenseNotificationEnabled(), sharedAccountSettings.getLargeExpenseNotificationThreshold());
        }
    }

    @Transactional
    public LargeExpenseNotificationDto getLargeExpenseNotification(Long userId) {
        SharedAccountSettings sharedAccountSettings = sharedAccountSettingsRepository.findByUserId(userId);

        return new LargeExpenseNotificationDto(sharedAccountSettings.isLargeExpenseNotificationEnabled(), sharedAccountSettings.getLargeExpenseNotificationThreshold());
    }

    @Transactional
    public void handleLargeNotification(Long userId, SharedExpense expense) {
        SharedAccountSettings sharedAccountSettings = sharedAccountSettingsRepository.findByUserId(userId);

        if (!sharedAccountSettings.isLargeExpenseNotificationEnabled()) return;
        if (sharedAccountSettings.getLargeExpenseNotificationThreshold() == null) return;

        if (expense.getAmount().compareTo(sharedAccountSettings.getLargeExpenseNotificationThreshold()) > 0) {

            outboxService.save("SharedExpense", expense.getId().toString(),"notification.shared-account.large-expense-detected",
                    new LargeExpenseNotificationEvent(expense.getOwnerId(),
                    expense.getMemberId(),
                    userId,
                    expense.getId(),
                    expense.getAmount(),
                    sharedAccountSettings.getLargeExpenseNotificationThreshold(),
                    LocalDateTime.now()));

            log.info("Large expense detected, outbox event created expenseId={}, ownerId={}, memberId={}, triggeredByUserId={}, amount={}, threshold={}",
                    expense.getId(), expense.getOwnerId(), expense.getMemberId(), userId, expense.getAmount(), sharedAccountSettings.getLargeExpenseNotificationThreshold());
        }
    }

}