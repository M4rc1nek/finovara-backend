package com.finovara.finovarabackend.usersetting.piggybank.roundup.service;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.exception.notfound.WalletNotFoundException;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.piggybank.dto.PiggyBankDTO;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.piggybank.service.PiggyBankService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.factory.SettingsFactory;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.AutoPaymentsMode;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.usersetting.piggybank.repository.PiggyBankSettingsRepository;
import com.finovara.finovarabackend.usersetting.piggybank.roundup.dto.RoundUpDto;
import com.finovara.finovarabackend.util.service.expense.ExpenseManagerService;
import com.finovara.finovarabackend.util.service.piggybank.PiggyBankManagerService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import com.finovara.finovarabackend.wallet.model.Wallet;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoundUpService {

    private final UserManagerService userManagerService;
    private final ExpenseManagerService expenseManagerService;
    private final PiggyBankManagerService piggyBankManagerService;
    private final PiggyBankService piggyBankService;
    private final PiggyBankRepository piggyBankRepository;
    private final WalletRepository walletRepository;
    private final PiggyBankActivityService piggyBankActivityService;

    @Transactional
    public RoundUpDto getRoundUp(String email, Long piggyBankId) {
        userManagerService.getUserByEmailOrThrow(email);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserEmail(piggyBankId, email);
        PiggyBankSettings piggyBankSettings = piggyBank.getSettings();

        return new RoundUpDto(piggyBankSettings.isRoundUpActive());
    }

    @Transactional
    public PiggyBankDTO addDefaultPiggyBank(PiggyBankDTO piggyBankDTO, String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        return piggyBankService.addPiggyBank(piggyBankDTO, user.getEmail());
    }

    @Transactional
    public void saveRoundUpPiggyBank(String email, Long piggyBankId, RoundUpDto dto) {
        userManagerService.getUserByEmailOrThrow(email);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserEmail(piggyBankId, email);

        PiggyBankSettings settings = piggyBank.getSettings();
        settings.setRoundUpActive(dto.roundUpActive());
    }

    @Transactional
    public void handleExpenseForRoundUp(String email, Long expenseId, AutoPaymentsMode mode) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        Expense expense = expenseManagerService.getExpenseByUserIdOrThrow(expenseId, user.getId());
        List<PiggyBank> piggyBanks = piggyBankRepository.findAllByUserAssignedEmail(email);
        Wallet wallet = walletRepository.findByUserAssignedEmail(email)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        if (piggyBanks == null || piggyBanks.isEmpty()) return;

        for (PiggyBank piggyBank : piggyBanks) {
            PiggyBankSettings piggyBankSettings = piggyBank.getSettings();
            if (piggyBankSettings.isRoundUpActive()) {
                BigDecimal expenseAmount = expense.getAmount();
                BigDecimal roundUpAmount = expenseAmount.setScale(0, RoundingMode.CEILING).subtract(expenseAmount);
                if (wallet.getBalance().compareTo(roundUpAmount) < 0) {
                    throw new InvalidInputException("Insufficient funds for round-up");
                }
                switch (mode) {
                    case APPLY -> {
                        if (roundUpAmount.compareTo(BigDecimal.ZERO) > 0) {
                            piggyBank.setAmount(piggyBank.getAmount().add(roundUpAmount));
                            wallet.setBalance(wallet.getBalance().subtract(roundUpAmount));
                            piggyBankActivityService.createPaymentPiggyBankActivity(email, piggyBank, PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, roundUpAmount);
                        }
                    }
                    case ROLLBACK -> {
                        BigDecimal amountToRollBack = roundUpAmount.min(piggyBank.getAmount());
                        piggyBank.setAmount(piggyBank.getAmount().subtract(amountToRollBack));
                        wallet.setBalance(wallet.getBalance().add(amountToRollBack));
                        piggyBankActivityService.createPaymentPiggyBankActivity(email, piggyBank, PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, roundUpAmount);
                    }
                }
            }
        }
    }

}
