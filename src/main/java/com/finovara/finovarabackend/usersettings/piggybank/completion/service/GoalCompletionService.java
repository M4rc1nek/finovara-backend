package com.finovara.finovarabackend.usersettings.piggybank.completion.service;

import com.finovara.finovarabackend.exception.InvalidInputException;
import com.finovara.finovarabackend.exception.NotAuthorizedException;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersettings.piggybank.completion.dto.GoalCompletionDto;
import com.finovara.finovarabackend.util.service.piggybank.PiggyBankCheckGoalCompletion;
import com.finovara.finovarabackend.util.service.piggybank.PiggyBankManagerService;
import com.finovara.finovarabackend.util.service.user.UserManagerService;
import com.finovara.finovarabackend.util.service.wallet.WalletManagerService;
import com.finovara.finovarabackend.wallet.model.Wallet;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service

@RequiredArgsConstructor
public class GoalCompletionService {
    private final UserManagerService userManagerService;
    private final WalletManagerService walletManagerService;
    private final PiggyBankManagerService piggyBankManagerService;
    private final PiggyBankRepository piggyBankRepository;
    private final WalletRepository walletRepository;

    private final PiggyBankCheckGoalCompletion piggyBankCheckGoalCompletion;

    @Transactional
    public void addGoalCompletion(Long piggyBankId, String email, GoalCompletionDto goalCompletionDto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserEmail(piggyBankId, user.getEmail());

        if (piggyBank.getGoalAmount() == null || piggyBank.getGoalAmount().compareTo(BigDecimal.ZERO) <= 0 ) {
            throw new InvalidInputException("Cannot set goal completion strategy for a piggy bank without a goal.");
        }

        piggyBank.setGoalCompletionStrategy(goalCompletionDto.strategy());
        piggyBankRepository.save(piggyBank);

    }

    @Transactional
    public void saveGoalCompletion(String email, List<GoalCompletionDto> settings) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        for (GoalCompletionDto dto : settings) {
            PiggyBank piggyBank = piggyBankManagerService.getPiggyBankOrThrow(dto.piggyBankId());

            if (!piggyBank.getUserAssigned().getId().equals(user.getId())) {
                throw new NotAuthorizedException("Not your piggy bank");
            }
            piggyBank.setGoalCompletionStrategy(dto.strategy());
        }
    }

    @Transactional
    public GoalCompletionDto getCompletionDto(String email, Long piggyBankId) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankOrThrow(piggyBankId);

        if (!piggyBank.getUserAssigned().getId().equals(user.getId())) {
            throw new NotAuthorizedException("Not your piggy bank");
        }

        return new GoalCompletionDto(piggyBankId, piggyBank.getGoalCompletionStrategy());
    }

    @Transactional
    public void handleGoalCompletion(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        Wallet wallet = walletManagerService.getWalletByUserEmailOrThrow(user.getEmail());
        List<PiggyBank> piggyBanks = piggyBankRepository.findAllByUserAssignedEmail(user.getEmail());

        for (PiggyBank piggyBank : piggyBanks) {
            if (!piggyBankCheckGoalCompletion.isGoalCompleted(piggyBank)) {
                continue;
            }

            switch (piggyBank.getGoalCompletionStrategy()) {
                case NONE -> {
                }

                case WITHDRAW_AND_KEEP -> {
                    transferFund(piggyBank, wallet);
                }

                case WITHDRAW_AND_DELETE -> {
                    transferFund(piggyBank, wallet);
                    // Safety check: ensure no money is left before deleting.
                    if (piggyBank.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                        throw new InvalidInputException("Cannot delete piggy bank with balance.");
                    }
                    piggyBankRepository.delete(piggyBank);
                }
            }
        }
        walletRepository.save(wallet);
    }

    private void transferFund(PiggyBank piggyBank, Wallet wallet) {
        BigDecimal amountToTransfer = piggyBank.getAmount();

        if (amountToTransfer == null || amountToTransfer.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        wallet.setBalance(wallet.getBalance().add(amountToTransfer));
        piggyBank.setAmount(BigDecimal.ZERO);
    }

}