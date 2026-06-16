package com.finovara.financeservice.settings.piggybank.completion.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.piggybank.repository.PiggyBankRepository;
import com.finovara.financeservice.settings.piggybank.completion.dto.GoalCompletionDto;
import com.finovara.financeservice.settings.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.financeservice.settings.piggybank.model.PiggyBankSettings;
import com.finovara.financeservice.util.piggybank.PiggyBankCheckGoalCompletion;
import com.finovara.financeservice.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.repository.WalletRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalCompletionService {
    private final WalletManagerService walletManagerService;
    private final PiggyBankManagerService piggyBankManagerService;
    private final PiggyBankRepository piggyBankRepository;
    private final GoalCompletionCore goalCompletionCore;
    private final WalletRepository walletRepository;

    @Transactional
    public void addGoalCompletion(Long piggyBankId, Long userId, GoalCompletionDto goalCompletionDto) {
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);
        PiggyBankSettings piggyBankSettings = piggyBank.getSettings();

        if (piggyBank.getGoalAmount() == null || piggyBank.getGoalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidInputException("Cannot set goal completion strategy for a piggy bank without a goal.");
        }

        piggyBankSettings.setGoalCompletionStrategy(goalCompletionDto.strategy());
        piggyBankRepository.save(piggyBank);

    }

    @Transactional
    public void saveGoalCompletion(Long userId, Long piggyBankId, GoalCompletionDto settings) {
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);
        PiggyBankSettings piggyBankSettings = piggyBank.getSettings();

        piggyBankSettings.setGoalCompletionStrategy(settings.strategy());
    }

    @Transactional
    public GoalCompletionDto getCompletionDto(Long userId, Long piggyBankId) {
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);
        PiggyBankSettings piggyBankSettings = piggyBank.getSettings();
        return new GoalCompletionDto(piggyBankSettings.getGoalCompletionStrategy());
    }

    @Transactional
    public void handleGoalCompletion(Long userId) {
        Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(userId);
        List<PiggyBank> piggyBanks = piggyBankRepository.findAllByUserId(userId);
        for (PiggyBank piggyBank : piggyBanks) {
            if (!PiggyBankCheckGoalCompletion.isGoalCompleted(piggyBank)) {
                continue;
            }
            GoalCompletionStrategy strategy = piggyBank.getSettings().getGoalCompletionStrategy();
            if (strategy == null) {
                strategy = GoalCompletionStrategy.NONE;
            }

            goalCompletionCore.apply(userId, piggyBank, wallet, strategy);
        }
        walletRepository.save(wallet);
    }
}
