package com.finovara.financeservice.sharedaccount.service.piggybank;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.financeservice.sharedaccount.dto.piggybank.SharedPiggyBankDto;
import com.finovara.financeservice.sharedaccount.dto.wallet.SharedWalletDto;
import com.finovara.financeservice.sharedaccount.mapper.piggybank.SharedPiggyBankMapper;
import com.finovara.financeservice.sharedaccount.model.piggybank.SharedPiggyBank;
import com.finovara.financeservice.sharedaccount.repository.piggybank.SharedPiggyBankRepository;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import com.finovara.financeservice.util.piggybank.PiggyBankCalculator;
import com.finovara.financeservice.util.piggybank.PiggyBankValidator;
import com.finovara.financeservice.util.piggybank.manager.SharedPiggyBankManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedPiggyBankManagementService {

    private static final int MAX_PIGGY_BANKS = 5;

    private final SharedPiggyBankRepository sharedPiggyBankRepository;
    private final SharedPiggyBankManager sharedPiggyBankManager;
    private final SharedPiggyBankMapper sharedPiggyBankMapper;
    private final SharedWalletService sharedWalletService;

    @Transactional
    public Long addPiggyBank(SharedPiggyBankDto sharedPiggyBankDto, Long userId) {
        SharedWalletDto walletDto = sharedWalletService.getWallet(userId);
        long currentPiggyBanks = sharedPiggyBankRepository.countPiggyBanksByUserId(userId);

        if (currentPiggyBanks >= MAX_PIGGY_BANKS) {
            throw new InvalidInputException("You have reached the maximum number of piggy banks: " + MAX_PIGGY_BANKS);
        }

        if (sharedPiggyBankRepository.existsByNameIgnoreCase(userId, sharedPiggyBankDto.name())) {
            throw new EntityAlreadyExistsException("This piggy bank name already exists");
        }

        PiggyBankValidator.validateSharedPiggyBankGoalAmount(sharedPiggyBankDto);

        SharedPiggyBank sharedPiggyBank = SharedPiggyBank.builder()
                .name(sharedPiggyBankDto.name())
                .amount(BigDecimal.ZERO)
                .createdAt(LocalDate.now())
                .goalAmount(sharedPiggyBankDto.goalAmount())
                .goalType(sharedPiggyBankDto.goalType())
                .ownerId(walletDto.ownerId())
                .memberId(walletDto.memberId())
                .build();

        SharedPiggyBank saved = sharedPiggyBankRepository.save(sharedPiggyBank);

        log.info("Added a new Piggy Bank called {}, with a goal amount of {}", sharedPiggyBankDto.name(), sharedPiggyBankDto.goalAmount());
        return saved.getId();
    }

    @Transactional
    public Long editPiggyBank(Long userId, SharedPiggyBankDto sharedPiggyBankDto, Long piggyBankId) {
        SharedPiggyBank sharedPiggyBank = sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId);

        if (sharedPiggyBankRepository.existsByNameIgnoreCase(userId, sharedPiggyBankDto.name())
                && !sharedPiggyBank.getName().equalsIgnoreCase(sharedPiggyBankDto.name())) {
            throw new EntityAlreadyExistsException("This piggy bank name already exists");
        }

        PiggyBankValidator.validateSharedPiggyBankGoalAmount(sharedPiggyBankDto);

        sharedPiggyBank.setName(sharedPiggyBankDto.name());
        sharedPiggyBank.setGoalAmount(sharedPiggyBankDto.goalAmount());
        sharedPiggyBank.setGoalType(sharedPiggyBankDto.goalType());

        SharedPiggyBank saved = sharedPiggyBankRepository.save(sharedPiggyBank);

        log.info("Edited Piggy Bank. New name: {}, new goal amount: {}", sharedPiggyBankDto.name(), sharedPiggyBankDto.goalAmount());

        return saved.getId();
    }

    public List<SharedPiggyBankDto> getAllPiggyBanks(Long userId) {
        List<SharedPiggyBank> sharedPiggyBanks = sharedPiggyBankRepository.findAllByUserId(userId);

        return sharedPiggyBanks.stream()
                .map(sharedPiggyBank -> sharedPiggyBankMapper.mapToPiggyBankDto(sharedPiggyBank, PiggyBankCalculator.calculateSharedPiggyBankProgress(sharedPiggyBank)))
                .toList();
    }

    @Transactional
    public void deletePiggyBank(Long userId, Long piggyBankId) {
        SharedPiggyBank sharedPiggyBank = sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId);

        if (sharedPiggyBank.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidInputException("Cannot delete piggy bank with balance. Withdraw funds first.");
        }

        sharedPiggyBankRepository.delete(sharedPiggyBank);

        log.info("Deleted sharedPiggyBank id={} for userId={}", piggyBankId, userId);
    }
}