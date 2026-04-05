package com.finovara.finovarabackend.piggybank.service;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.exception.conflict.NameAlreadyExistsException;
import com.finovara.finovarabackend.piggybank.dto.PiggyBankDTO;
import com.finovara.finovarabackend.piggybank.mapper.PiggyBankMapper;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.factory.SettingsFactory;
import com.finovara.finovarabackend.usersetting.piggybank.completion.service.GoalCompletionService;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.usersetting.piggybank.repository.PiggyBankSettingsRepository;
import com.finovara.finovarabackend.util.service.calculate.percentage.CalculatePercentage;
import com.finovara.finovarabackend.util.service.piggybank.PiggyBankCheckGoalCompletion;
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
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PiggyBankService {

    private final UserManagerService userManagerService;
    private final PiggyBankRepository piggyBankRepository;
    private final PiggyBankManagerService piggyBankManagerService;
    private final WalletManagerService walletManagerService;
    private final GoalCompletionService goalCompletionService;
    private final WalletRepository walletRepository;
    private final PiggyBankActivityService piggyBankActivityService;
    private final PiggyBankSettingsRepository piggyBankSettingsRepository;
    private final SettingsFactory settingsFactory;
    private final PiggyBankMapper piggyBankMapper;

    @Transactional
    public Long addPiggyBank(PiggyBankDTO piggyBankDTO, String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        long currentPiggyBanks = piggyBankRepository.countPiggyBanksByUserId(user.getId());

        int maxPiggyBanks = 5;
        if (currentPiggyBanks >= maxPiggyBanks) {
            throw new InvalidInputException("you have reached the maximum number of piggy banks: " + maxPiggyBanks);
        }

        if (piggyBankRepository.existsByNameAndUserAssignedId(piggyBankDTO.name(), user.getId())) {
            throw new NameAlreadyExistsException("This piggy bank name already exists");
        }

        if (piggyBankDTO.goalAmount() != null && piggyBankDTO.goalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidInputException("Amount have to be positive");
        }

        PiggyBank piggyBank = PiggyBank.builder()
                .name(piggyBankDTO.name())
                .amount(BigDecimal.ZERO)
                .createdAt(LocalDate.now())
                .userAssigned(user)
                .goalAmount(piggyBankDTO.goalAmount())
                .goalType(piggyBankDTO.goalType())
                .build();

        piggyBankActivityService.createSimplePiggyBankActivity(email, piggyBank, PiggyBankActivityType.ADDED_PIGGY_BANK);
        PiggyBank saved = piggyBankRepository.save(piggyBank);
        PiggyBankSettings settings = settingsFactory.createDefaultPiggyBankSettings(saved);
        piggyBankSettingsRepository.save(settings);

        return saved.getId(); //saved or piggybank
    }

    @Transactional
    public void addBalanceToPiggyBank(String email, Long piggyBankId, BigDecimal amount) {

        UserContext userContext = getEntitiesForTransaction(email, piggyBankId);

        validateAmount(amount);
        validateSufficientFunds(userContext.wallet.getBalance(), amount);

        userContext.wallet.setBalance(userContext.wallet.getBalance().subtract(amount));
        userContext.piggyBank.setAmount(userContext.piggyBank.getAmount().add(amount));

        piggyBankActivityService.createPaymentPiggyBankActivity(email, userContext.piggyBank,
                PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY, amount);
        calculateProgress(userContext.piggyBank);
        PiggyBankCheckGoalCompletion.isGoalCompleted(userContext.piggyBank);

        walletRepository.save(userContext.wallet);
        piggyBankRepository.save(userContext.piggyBank);

        if (PiggyBankCheckGoalCompletion.isGoalCompleted(userContext.piggyBank)) {
            goalCompletionService.handleGoalCompletion(email);
        }
    }

    @Transactional
    public void removeBalanceFromPiggyBank(String email, Long piggyBankId, BigDecimal amount) {

        UserContext userContext = getEntitiesForTransaction(email, piggyBankId);

        validateAmount(amount);
        validateSufficientFunds(userContext.piggyBank.getAmount(), amount);

        userContext.piggyBank.setAmount(userContext.piggyBank.getAmount().subtract(amount));
        userContext.wallet.setBalance(userContext.wallet.getBalance().add(amount));
        calculateProgress(userContext.piggyBank);
        PiggyBankCheckGoalCompletion.isGoalCompleted((userContext.piggyBank));

        piggyBankActivityService.createPaymentPiggyBankActivity(email, userContext.piggyBank, PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK, amount);

        walletRepository.save(userContext.wallet);
        piggyBankRepository.save(userContext.piggyBank);
    }

    public List<PiggyBankDTO> getAllPiggyBanks(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        List<PiggyBank> piggyBanks = piggyBankRepository.findAllByUserAssignedId(user.getId());

        return piggyBanks.stream()
                .map(piggyBank -> piggyBankMapper.mapToPiggyBankDto(piggyBank, user, calculateProgress(piggyBank), PiggyBankCheckGoalCompletion.isGoalCompleted(piggyBank)))
                .toList();
    }

    @Transactional
    public void deletePiggyBank(String email, Long piggyBankId) {
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserEmail(piggyBankId, email);

        if (piggyBank == null || piggyBank.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidInputException("Cannot delete piggy bank with balance.  Withdraw funds first.");
        }
        piggyBankActivityService.createSimplePiggyBankActivity(email, piggyBank, PiggyBankActivityType.DELETED_PIGGY_BANK);
        piggyBankRepository.delete(piggyBank);
    }

    private record UserContext(Wallet wallet, PiggyBank piggyBank, User user) {

    }
    private UserContext getEntitiesForTransaction(String email, Long piggyBankId) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserEmail(piggyBankId, email);
        Wallet wallet = walletManagerService.getWalletByUserEmailOrThrow(email);

        return new UserContext(wallet, piggyBank, user);
    }

    private Double calculateProgress(PiggyBank piggyBank) {
        BigDecimal goalAmount = piggyBank.getGoalAmount();

        if (goalAmount == null || piggyBank.getGoalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }

        return piggyBank.getAmount()
                .divide(piggyBank.getGoalAmount(), 4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidInputException("Amount must be non negative");
        }
    }

    private void validateSufficientFunds(BigDecimal sourceAmount, BigDecimal amount) {
        if (sourceAmount == null || sourceAmount.compareTo(amount) < 0) {
            throw new InvalidInputException("Insufficient funds");
        }
    }
}
