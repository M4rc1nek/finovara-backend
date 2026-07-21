package com.finovara.financeservice.sharedaccount.settings.spendcontrol.service;

import com.finovara.contracts.exception.unprocessablecontent.InvalidOperationException;
import com.finovara.contracts.percentage.CalculatePercentage;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettings;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettingsRepository;
import com.finovara.financeservice.sharedaccount.settings.spendcontrol.dto.SpendControlDto;
import com.finovara.financeservice.sharedaccount.wallet.model.SharedWallet;
import com.finovara.financeservice.sharedaccount.wallet.repository.SharedWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SpendControlService {
    private final SharedAccountSettingsRepository sharedAccountSettingsRepository;
    private final SharedWalletRepository sharedWalletRepository;

    @Transactional
    public void saveSpendControlService(Long userId, SpendControlDto spendControlDto) {
        SharedAccountSettings settings = sharedAccountSettingsRepository.findByUserId(userId);

        settings.setSpendControlEnabled(spendControlDto.spendControlEnabled());
        settings.setSpendControlPercentage(spendControlDto.spendControlPercentage());
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
