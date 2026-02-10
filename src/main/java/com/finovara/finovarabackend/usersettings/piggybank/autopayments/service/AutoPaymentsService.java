package com.finovara.finovarabackend.usersettings.piggybank.autopayments.service;

import com.finovara.finovarabackend.exception.NotAuthorizedException;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersettings.piggybank.autopayments.dto.AutoPaymentsDto;
import com.finovara.finovarabackend.usersettings.piggybank.autopayments.model.AutoPaymentsMode;
import com.finovara.finovarabackend.usersettings.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.util.service.piggybank.PiggyBankManagerService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import com.finovara.finovarabackend.util.service.wallet.WalletManagerService;
import com.finovara.finovarabackend.wallet.model.Wallet;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AutoPaymentsService {

    private final UserManagerService userManagerService;
    private final WalletManagerService walletManagerService;

    @Transactional
    public void createAutomation(String email, AutoPaymentsDto autoPaymentsDto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        PiggyBankSettings piggyBankSettings = user.getPiggyBankSettings();


        piggyBankSettings.setAutomationActive(autoPaymentsDto.isAutomationActive());

        if (piggyBankSettings.isAutomationActive()) {
            validatePercentage(autoPaymentsDto);
            piggyBankSettings.setAutomationPercentage(autoPaymentsDto.percentage());
        } else {
            piggyBankSettings.setAutomationPercentage(BigDecimal.ZERO);
        }

    }

    @Transactional
    public AutoPaymentsDto getAutomation(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        PiggyBankSettings piggyBankSettings = user.getPiggyBankSettings();


        return new AutoPaymentsDto(
                piggyBankSettings.isAutomationActive(),
                piggyBankSettings.getAutomationPercentage()
        );
    }

    @Transactional
    public void saveAutoPaymentsPiggyBank(String email, AutoPaymentsDto settings) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        PiggyBankSettings piggyBankSettings = user.getPiggyBankSettings();

            validatePercentage(settings);

            piggyBankSettings.setAutomationActive(settings.isAutomationActive());
            piggyBankSettings.setAutomationPercentage(settings.isAutomationActive() ? settings.percentage() : BigDecimal.ZERO);
        }


    public void handleRevenuePiggyBankAutomation(String email, BigDecimal revenueAmount, AutoPaymentsMode mode) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        List<PiggyBank> piggyBanks = user.getPiggyBanks();
        PiggyBankSettings piggyBankSettings = user.getPiggyBankSettings();
        Wallet wallet = walletManagerService.getWalletByUserEmailOrThrow(email);

        if (piggyBanks == null || piggyBanks.isEmpty()) return;

        for (PiggyBank piggyBank : piggyBanks) {
            if (piggyBankSettings.isAutomationActive()) { // albo zostawić jak jest albo pomyslec o !piggyBank.isAutomationActive()) continue;
                BigDecimal automationAmount = revenueAmount
                        .multiply(piggyBankSettings.getAutomationPercentage())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                switch (mode) {
                    case APPLY -> {
                        BigDecimal availableToTransfer = wallet.getBalance().min(automationAmount);
                        piggyBank.setAmount(piggyBank.getAmount().add(availableToTransfer));
                        wallet.setBalance(wallet.getBalance().subtract(availableToTransfer));
                    }
                    case ROLLBACK -> {
                        BigDecimal amountToRollback = automationAmount.min(piggyBank.getAmount());
                        piggyBank.setAmount(piggyBank.getAmount().subtract(amountToRollback));
                        wallet.setBalance(wallet.getBalance().add(amountToRollback));
                    }
                }
            }
        }
    }

    private void validatePercentage(AutoPaymentsDto autoPaymentsDto) {
        if (autoPaymentsDto.isAutomationActive()) {
            if (autoPaymentsDto.percentage() == null) {
                throw new IllegalArgumentException("Percentage is required");
            }

            if (autoPaymentsDto.percentage().compareTo(BigDecimal.ZERO) <= 0 ||
                    autoPaymentsDto.percentage().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Percentage must be between 1 and 100");
            }
        }

    }
}
