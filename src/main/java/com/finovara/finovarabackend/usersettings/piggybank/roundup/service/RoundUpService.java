package com.finovara.finovarabackend.usersettings.piggybank.roundup.service;

import com.finovara.finovarabackend.exception.*;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.piggybank.dto.PiggyBankDTO;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.piggybank.service.PiggyBankService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersettings.piggybank.autopayments.model.AutoPaymentsMode;
import com.finovara.finovarabackend.usersettings.piggybank.roundup.dto.RoundUpDto;
import com.finovara.finovarabackend.util.service.expense.ExpenseManagerService;
import com.finovara.finovarabackend.util.service.piggybank.PiggyBankManagerService;
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
public class RoundUpService {

    private final UserManagerService userManagerService;
    private final ExpenseManagerService expenseManagerService;
    private final PiggyBankService piggyBankService;
    private final PiggyBankManagerService piggyBankManagerService;
    private final PiggyBankRepository piggyBankRepository;;
    private final WalletRepository walletRepository;

    @Transactional
    public void createRoundUp(RoundUpDto roundUpDto, Long piggyBankId, String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankOrThrow(piggyBankId);

        if (user.getPiggyBanks().isEmpty()) {
            throw new MissingRequirementException("Piggy banks not found for you");
        }

        piggyBank.setRoundUpActive(roundUpDto.roundUpActive());

        if (!piggyBank.getUserAssigned().getId().equals(user.getId())) {
            throw new NotAuthorizedException("Not your Piggy Bank");

        }

    }

    @Transactional
    public RoundUpDto getRoundUp(String email, Long piggyBankId) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankOrThrow(piggyBankId);

        if (!piggyBank.getUserAssigned().getId().equals(user.getId())) {
            throw new NotAuthorizedException("Not your piggy bank");
        }

        return new RoundUpDto(piggyBankId, piggyBank.isRoundUpActive());
    }

    @Transactional
    public PiggyBankDTO addDefaultPiggyBank(PiggyBankDTO piggyBankDTO, String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        return piggyBankService.addPiggyBank(piggyBankDTO, user.getEmail());
    }

    @Transactional
    public void saveRoundUpPiggyBank(String email, List<RoundUpDto> settings) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        for (RoundUpDto dto : settings) {
            PiggyBank piggyBank = piggyBankManagerService.getPiggyBankOrThrow(dto.piggyBankId());

            if (!piggyBank.getUserAssigned().getId().equals(user.getId())) {
                throw new NotAuthorizedException("Not your piggy bank");
            }
            piggyBank.setRoundUpActive(dto.roundUpActive());
        }
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
            if (piggyBank.isRoundUpActive()) {
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
                        }
                    }
                    case ROLLBACK -> {
                        BigDecimal amountToRollBack = roundUpAmount.min(piggyBank.getAmount());
                        piggyBank.setAmount(piggyBank.getAmount().subtract(amountToRollBack));
                        wallet.setBalance(wallet.getBalance().add(amountToRollBack));
                    }
                }
            }
        }
    }

}
