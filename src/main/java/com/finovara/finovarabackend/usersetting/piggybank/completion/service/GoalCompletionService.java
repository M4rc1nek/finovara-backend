package com.finovara.finovarabackend.usersetting.piggybank.completion.service;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.completion.dto.GoalCompletionDto;
import com.finovara.finovarabackend.usersetting.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.util.piggybank.PiggyBankCheckGoalCompletion;
import com.finovara.finovarabackend.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import com.finovara.finovarabackend.util.wallet.WalletManagerService;
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
    private final GoalCompletionCore goalCompletionCore;
    private final WalletRepository walletRepository;

    @Transactional
    public void addGoalCompletion(Long piggyBankId, Long userId, GoalCompletionDto goalCompletionDto) {
        User user = userManagerService.getUserByIdOrThrow(userId);
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
        userManagerService.getUserByIdOrThrow(userId);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);
        PiggyBankSettings piggyBankSettings = piggyBank.getSettings();

        piggyBankSettings.setGoalCompletionStrategy(settings.strategy());
    }

    @Transactional
    public GoalCompletionDto getCompletionDto(Long userId, Long piggyBankId) {
        userManagerService.getUserByIdOrThrow(userId);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);
        PiggyBankSettings piggyBankSettings = piggyBank.getSettings();
        return new GoalCompletionDto(piggyBankSettings.getGoalCompletionStrategy());
    }

    @Transactional
    public void handleGoalCompletion(Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(userId);
        List<PiggyBank> piggyBanks = piggyBankRepository.findAllByUserAssignedId(userId);
        for (PiggyBank piggyBank : piggyBanks) {
            if (!PiggyBankCheckGoalCompletion.isGoalCompleted(piggyBank)) {
                continue;
            }
            GoalCompletionStrategy strategy = piggyBank.getSettings().getGoalCompletionStrategy();
            if (strategy == null) {
                strategy = GoalCompletionStrategy.NONE;
            }

            goalCompletionCore.apply(userId, piggyBank, wallet, user, strategy);
        }
        walletRepository.save(wallet);
    }
}