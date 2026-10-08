package com.finovara.financeservice.sharedaccount.settings.expense.spendcontrol.service;

import com.finovara.contracts.exception.unprocessablecontent.InvalidOperationException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.contracts.util.calculate.CalculatePercentage;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettings;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettingsRepository;
import com.finovara.financeservice.sharedaccount.settings.expense.spendcontrol.dto.SpendControlDto;
import com.finovara.financeservice.sharedaccount.wallet.model.SharedWallet;
import com.finovara.financeservice.sharedaccount.wallet.repository.SharedWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SpendControlService {
    private final SharedAccountSettingsRepository sharedAccountSettingsRepository;
    private final SharedWalletRepository sharedWalletRepository;
    private final SharedAccountParticipantsService sharedAccountParticipantsService;
    private final OutboxService outboxService;

    @Transactional
    public void saveSpendControlService(Long userId, SpendControlDto spendControlDto) {
        SharedAccountSettings settings = sharedAccountSettingsRepository.findByUserId(userId);

        boolean changed = settings.isSpendControlEnabled() != spendControlDto.spendControlEnabled()
                || !settings.getSpendControlPercentage().equals(spendControlDto.spendControlPercentage());

        if (changed) {
            settings.setSpendControlEnabled(spendControlDto.spendControlEnabled());
            settings.setSpendControlPercentage(spendControlDto.spendControlPercentage());

            SharedAccountParticipantsResponse participants = sharedAccountParticipantsService.getParticipants(userId);
            outboxService.save("SharedAccountSettings", userId.toString(), "shared-account.activity",
                    new SharedAccountActivityLogEvent(participants.ownerId(), participants.memberId(), userId, null, SharedAccountActivityLogType.SETTING_CHANGED, LocalDateTime.now()));
        }
    }

    @Transactional
    public SpendControlDto getSmartScan(Long userId) {
        SharedAccountSettings settings = sharedAccountSettingsRepository.findByUserId(userId);

        return new SpendControlDto(settings.isSpendControlEnabled(), settings.getSpendControlPercentage());
    }

    @Transactional
    public void handleSpendControl(Long userId, BigDecimal expenseAmount) {
        SharedAccountSettings settings = sharedAccountSettingsRepository.findByUserId(userId);

        if (!settings.isSpendControlEnabled()) return;

        SharedWallet sharedWallet = sharedWalletRepository.findByUserId(userId);

        BigDecimal maxAllowed = CalculatePercentage.calculateValueFromPercentage(sharedWallet.getBalance(), settings.getSpendControlPercentage());

        if (expenseAmount.compareTo(maxAllowed) > 0) {
            throw new InvalidOperationException("Spending exceeds allowable limit  of wallet balance");
        }
    }
}
