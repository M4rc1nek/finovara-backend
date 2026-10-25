package com.finovara.financeservice.sharedaccount.piggybank.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.piggybank.dto.SharedPiggyBankDto;
import com.finovara.financeservice.sharedaccount.piggybank.mapper.SharedPiggyBankMapper;
import com.finovara.financeservice.sharedaccount.piggybank.model.SharedPiggyBank;
import com.finovara.financeservice.sharedaccount.piggybank.repository.SharedPiggyBankRepository;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import com.finovara.financeservice.util.sharedaccount.piggybank.SharedPiggyBankManager;
import com.finovara.financeservice.util.transaction.piggybank.PiggyBankCalculator;
import com.finovara.financeservice.util.transaction.piggybank.PiggyBankValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedPiggyBankManagementService {

    @Value("${piggy-bank.max-piggy-banks}")
    int maxPiggyBanks;

    private final SharedPiggyBankRepository sharedPiggyBankRepository;
    private final SharedPiggyBankManager sharedPiggyBankManager;
    private final SharedPiggyBankMapper sharedPiggyBankMapper;
    private final SharedAccountParticipantsService sharedAccountParticipantsService;
    private final SharedWalletService sharedWalletService;
    private final OutboxService outboxService;

    @Transactional
    public Long addPiggyBank(SharedPiggyBankDto dto, Long userId) {
        SharedAccountParticipantsResponse participants = sharedAccountParticipantsService.getParticipants(userId);

        sharedWalletService.lockWalletForUpdate(userId);

        List<String> existingNames = sharedPiggyBankRepository.findLowerCaseNamesByUserId(userId);
        if (existingNames.size() >= maxPiggyBanks) {
            throw new InvalidInputException("You have reached the maximum number of piggy banks: " + maxPiggyBanks);
        }
        if (existingNames.contains(dto.name().toLowerCase(Locale.ROOT))) {
            throw new EntityAlreadyExistsException("This piggy bank name already exists");
        }

        PiggyBankValidator.validateSharedPiggyBankGoalAmount(dto);

        SharedPiggyBank saved = sharedPiggyBankRepository.save(SharedPiggyBank.builder()
                .name(dto.name())
                .amount(BigDecimal.ZERO)
                .createdAt(LocalDate.now())
                .goalAmount(dto.goalAmount())
                .goalType(dto.goalType())
                .ownerId(participants.ownerId())
                .memberId(participants.memberId())
                .build());

        logActivity(participants, userId, saved.getId(), SharedAccountActivityLogType.PIGGY_BANK_CREATED);

        log.info("Added a new Piggy Bank called {}, with a goal amount of {}", dto.name(), dto.goalAmount());
        return saved.getId();
    }

    @Transactional
    public Long editPiggyBank(Long userId, SharedPiggyBankDto dto, Long piggyBankId) {
        SharedPiggyBank piggyBank = sharedPiggyBankManager.getSharedPiggyBankForUpdateByIdAndUserId(piggyBankId, userId);

        if (sharedPiggyBankRepository.existsByNameIgnoreCaseExcludingId(userId, dto.name(), piggyBankId)) {
            throw new EntityAlreadyExistsException("This piggy bank name already exists");
        }

        PiggyBankValidator.validateSharedPiggyBankGoalAmount(dto);

        piggyBank.setName(dto.name());
        piggyBank.setGoalAmount(dto.goalAmount());
        piggyBank.setGoalType(dto.goalType());

        logActivity(sharedAccountParticipantsService.getParticipants(userId), userId, piggyBankId, SharedAccountActivityLogType.PIGGY_BANK_EDITED);

        log.info("Edited Piggy Bank. New name: {}, new goal amount: {}", dto.name(), dto.goalAmount());
        return piggyBankId;
    }

    @Transactional(readOnly = true)
    public List<SharedPiggyBankDto> getAllPiggyBanks(Long userId) {
        return sharedPiggyBankRepository.findAllByUserId(userId).stream()
                .map(pb -> sharedPiggyBankMapper.mapToPiggyBankDto(pb, PiggyBankCalculator.calculateSharedPiggyBankProgress(pb)))
                .toList();
    }

    @Transactional
    public void deletePiggyBank(Long userId, Long piggyBankId) {
        SharedPiggyBank piggyBank = sharedPiggyBankManager.getSharedPiggyBankForUpdateByIdAndUserId(piggyBankId, userId);

        if (piggyBank.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidInputException("Cannot delete piggy bank with balance. Withdraw funds first.");
        }

        sharedPiggyBankRepository.delete(piggyBank);
        logActivity(sharedAccountParticipantsService.getParticipants(userId), userId, piggyBankId, SharedAccountActivityLogType.PIGGY_BANK_DELETED);

        log.info("Deleted sharedPiggyBank id={} for userId={}", piggyBankId, userId);
    }

    private void logActivity(SharedAccountParticipantsResponse participants, Long userId, Long piggyBankId, SharedAccountActivityLogType type) {
        outboxService.save("SharedAccountPiggyBank", piggyBankId.toString(), "shared-account.activity",
                new SharedAccountActivityLogEvent(participants.ownerId(), participants.memberId(), userId, piggyBankId, type, LocalDateTime.now()));
    }
}