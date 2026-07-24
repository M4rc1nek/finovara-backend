package com.finovara.financeservice.sharedaccount.settings.expense.largeexpense.service;

import com.finovara.contracts.event.finance.sharedaccount.LargeExpenseNotificationEvent;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.sharedaccount.expense.model.SharedExpense;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettings;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettingsRepository;
import com.finovara.financeservice.sharedaccount.settings.expense.largeexpense.dto.LargeExpenseNotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LargeExpenseNotificationService {

    private final SharedAccountSettingsRepository sharedAccountSettingsRepository;
    private final OutboxService outboxService;

    @Transactional
    public void saveLargeExpenseNotification(Long userId, LargeExpenseNotificationDto largeExpenseNotificationDto) {
        SharedAccountSettings sharedAccountSettings = sharedAccountSettingsRepository.findByUserId(userId);

        sharedAccountSettings.setLargeExpenseNotificationEnabled(largeExpenseNotificationDto.largeExpenseNotificationEnabled());
        sharedAccountSettings.setLargeExpenseNotificationThreshold(largeExpenseNotificationDto.largeExpenseNotificationThreshold());

        log.info("Updated large expense notification settings userId={}, enabled={}, threshold={}",
                userId, sharedAccountSettings.isLargeExpenseNotificationEnabled(), sharedAccountSettings.getLargeExpenseNotificationThreshold());
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