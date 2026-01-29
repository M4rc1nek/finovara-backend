package com.finovara.finovarabackend.usersettings.piggybank.autopayments.service;

import com.finovara.finovarabackend.exception.NotAuthorizedException;
import com.finovara.finovarabackend.exception.PiggyBankNotFoundException;
import com.finovara.finovarabackend.exception.UserNotFoundException;
import com.finovara.finovarabackend.exception.WalletNotFoundException;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersettings.piggybank.autopayments.dto.AutoPaymentsDto;
import com.finovara.finovarabackend.usersettings.piggybank.autopayments.model.AutoPaymentsMode;
import com.finovara.finovarabackend.util.service.user.UserManagerService;
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
    private final PiggyBankRepository piggyBankRepository;
    private final WalletRepository walletRepository;

    @Transactional
    public void createAutomation(String email, Long piggyBankId, AutoPaymentsDto autoPaymentsDto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        PiggyBank piggyBank = getPiggyBankOrThrow(piggyBankId);

        if (!piggyBank.getUserAssigned().getId().equals(user.getId())) {
            throw new NotAuthorizedException("Not your piggy bank");
        }

        piggyBank.setAutomationActive(autoPaymentsDto.isAutomationActive());

        if (piggyBank.isAutomationActive()) {
            validatePercentage(autoPaymentsDto);
            piggyBank.setAutomationPercentage(autoPaymentsDto.percentage());
        } else {
            piggyBank.setAutomationPercentage(BigDecimal.ZERO);
        }

    }

    @Transactional
    public AutoPaymentsDto getAutomation(String email, Long piggyBankId) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        PiggyBank piggyBank = getPiggyBankOrThrow(piggyBankId);

        if (!piggyBank.getUserAssigned().getId().equals(user.getId())) {
            throw new NotAuthorizedException("Not your piggy bank");
        }

        return new AutoPaymentsDto(
                piggyBankId,
                piggyBank.isAutomationActive(),
                piggyBank.getAutomationPercentage()
        );
    }

    @Transactional
    public void updatePiggyBank(String email, List<AutoPaymentsDto> settings) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        for (AutoPaymentsDto dto : settings) {
            PiggyBank piggyBank = getPiggyBankOrThrow(dto.piggyBankId());

            if (!piggyBank.getUserAssigned().getId().equals(user.getId())) {
                throw new NotAuthorizedException("Not your piggy bank");
            }

            validatePercentage(dto);

            piggyBank.setAutomationActive(dto.isAutomationActive());
            piggyBank.setAutomationPercentage(dto.isAutomationActive() ? dto.percentage() : BigDecimal.ZERO);
        }
    }

    public void handleRevenuePiggyBankAutomation(String email, BigDecimal revenueAmount, AutoPaymentsMode mode) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        List<PiggyBank> piggyBanks = user.getPiggyBanks();
        Wallet wallet = getWalletOrThrow(email);

        if (piggyBanks == null || piggyBanks.isEmpty()) return;

        for (PiggyBank piggyBank : piggyBanks) {
            if (piggyBank.isAutomationActive()) { // albo zostawić jak jest albo pomyslec o !piggyBank.isAutomationActive()) continue;
                BigDecimal automationAmount = revenueAmount
                        .multiply(piggyBank.getAutomationPercentage())
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
    private PiggyBank getPiggyBankOrThrow(Long piggyBankId) {
        return piggyBankRepository.findById(piggyBankId)
                .orElseThrow(() -> new PiggyBankNotFoundException("PiggyBank not found"));
    }

    private Wallet getWalletOrThrow(String email) {
        return walletRepository.findByUserAssignedEmail(email)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));
    }

}
