package com.finovara.finovarabackend.piggybank.service;

import com.finovara.finovarabackend.exception.InvalidInputException;
import com.finovara.finovarabackend.exception.NameAlreadyExistsException;
import com.finovara.finovarabackend.piggybank.dto.PiggyBankDTO;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.piggybank.PiggyBankManagerService;
import com.finovara.finovarabackend.util.service.user.UserManagerService;
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
    private final WalletRepository walletRepository;

    @Transactional
    public PiggyBankDTO addPiggyBank(PiggyBankDTO piggyBankDTO, String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        long currentPiggyBanks = piggyBankRepository.countPiggyBanksByUserId(user.getId());

        int maxPiggyBanks = 5;
        if (currentPiggyBanks >= maxPiggyBanks) {
            throw new InvalidInputException("you have reached the maximum number of piggy banks: " + maxPiggyBanks);
        }

        if (piggyBankRepository.existsByName(piggyBankDTO.name())) {
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

        piggyBankRepository.save(piggyBank);

        return buildDTO(piggyBank, user);
    }

    private PiggyBankDTO buildDTO(PiggyBank piggyBank, User user) {
        Double progress = null;
        boolean goalCompleted = false;
        if (piggyBank.getGoalAmount() != null && piggyBank.getGoalAmount().compareTo(BigDecimal.ZERO) > 0) {
            progress =
                    piggyBank.getAmount()
                            .divide(piggyBank.getGoalAmount(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue();
            goalCompleted = piggyBank.getAmount()
                    .compareTo(piggyBank.getGoalAmount()) >= 0;
        }

        return new PiggyBankDTO(
                piggyBank.getId(),
                user.getId(),
                piggyBank.getName(),
                piggyBank.getAmount(),
                piggyBank.getCreatedAt(),
                piggyBank.getGoalType(),
                piggyBank.getGoalAmount(),
                progress,
                goalCompleted
        );
    }

    @Transactional
    public PiggyBankDTO addBalanceToPiggyBank(String email, Long piggyBankId, BigDecimal amount) {

        UserContext userContext = getEntitiesForTransaction(email, piggyBankId);

        if (isGoalCompleted(userContext.piggyBank)) {
            throw new InvalidInputException("Goal is already completed. You can only withdraw funds.");
        }

        validateAmount(amount);
        validateSufficientFunds(userContext.wallet.getBalance(), amount);

        userContext.wallet.setBalance(userContext.wallet.getBalance().subtract(amount));
        userContext.piggyBank.setAmount(userContext.piggyBank.getAmount().add(amount));

        walletRepository.save(userContext.wallet);
        piggyBankRepository.save(userContext.piggyBank);

        return buildDTO(userContext.piggyBank, userContext.user);

    }

    @Transactional
    public PiggyBankDTO removeBalanceFromPiggyBank(String email, Long piggyBankId, BigDecimal amount) {

        UserContext userContext = getEntitiesForTransaction(email, piggyBankId);

        validateAmount(amount);
        validateSufficientFunds(userContext.piggyBank.getAmount(), amount);

        userContext.piggyBank.setAmount(userContext.piggyBank.getAmount().subtract(amount));
        userContext.wallet.setBalance(userContext.wallet.getBalance().add(amount));

        walletRepository.save(userContext.wallet);
        piggyBankRepository.save(userContext.piggyBank);

        return buildDTO(userContext.piggyBank, userContext.user);

    }

    public List<PiggyBankDTO> getAllPiggyBanks(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        List<PiggyBank> piggyBanks = piggyBankRepository.findAllByUserAssignedEmail(email);

        return piggyBanks.stream()
                .map(pb -> buildDTO(pb, user))
                .toList();
    }

    @Transactional
    public void deletePiggyBank(String email, Long piggyBankId) {
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserEmail(piggyBankId, email);

        if (piggyBank == null || piggyBank.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidInputException("Cannot delete piggy bank with balance.  Withdraw funds first.");
        }
        piggyBankRepository.delete(piggyBank);
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

    private record UserContext(Wallet wallet, PiggyBank piggyBank, User user) {
    }

    private UserContext getEntitiesForTransaction(String email, Long piggyBankId) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserEmail(piggyBankId, email);
        Wallet wallet = walletManagerService.getWalletByUserEmailOrThrow(email);

        return new UserContext(wallet, piggyBank, user);
    }

    private boolean isGoalCompleted(PiggyBank piggyBank) {
        if (piggyBank.getGoalAmount() == null) {
            return false;
        }
        return piggyBank.getAmount().compareTo(piggyBank.getGoalAmount()) >= 0;
    }

}
