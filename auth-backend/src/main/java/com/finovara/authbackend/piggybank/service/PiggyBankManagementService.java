package com.finovara.authbackend.piggybank.service;

import com.finovara.contracts.event.activity.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.authbackend.piggybank.dto.PiggyBankDto;
import com.finovara.authbackend.piggybank.mapper.PiggyBankMapper;
import com.finovara.authbackend.piggybank.model.PiggyBank;
import com.finovara.authbackend.piggybank.repository.PiggyBankRepository;
import com.finovara.authbackend.usersetting.factory.SettingsFactory;
import com.finovara.authbackend.usersetting.finances.recurring.repository.RecurringSettingsRepository;
import com.finovara.authbackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.authbackend.usersetting.piggybank.repository.PiggyBankSettingsRepository;
import com.finovara.authbackend.util.piggybank.PiggyBankCalculator;
import com.finovara.authbackend.util.piggybank.PiggyBankCheckGoalCompletion;
import com.finovara.authbackend.util.piggybank.PiggyBankValidator;
import com.finovara.authbackend.util.piggybank.manager.PiggyBankManagerService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PiggyBankManagementService {

    private final PiggyBankRepository piggyBankRepository;
    private final PiggyBankManagerService piggyBankManagerService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PiggyBankSettingsRepository piggyBankSettingsRepository;
    private final RecurringSettingsRepository recurringSettingsRepository;
    private final SettingsFactory settingsFactory;
    private final PiggyBankMapper piggyBankMapper;

    @Transactional
    public Long addPiggyBank(PiggyBankDto piggyBankDto, Long userId) {
        long currentPiggyBanks = piggyBankRepository.countPiggyBanksByUserId(userId);

        int maxPiggyBanks = 5;
        if (currentPiggyBanks >= maxPiggyBanks) {
            throw new InvalidInputException("you have reached the maximum number of piggy banks: " + maxPiggyBanks);
        }

        if (piggyBankRepository.existsByNameIgnoreCase(userId, piggyBankDto.name())) {
            throw new EntityAlreadyExistsException("This piggy bank name already exists");
        }

        PiggyBankValidator.validateGoalAmount(piggyBankDto);

        PiggyBank piggyBank = PiggyBank.builder()
                .name(piggyBankDto.name())
                .amount(BigDecimal.ZERO)
                .createdAt(LocalDate.now())
                .userId(userId)
                .goalAmount(piggyBankDto.goalAmount())
                .goalType(piggyBankDto.goalType())
                .build();

        PiggyBank saved = piggyBankRepository.save(piggyBank);
        kafkaTemplate.send("activity.piggybank", new PiggyBankActivityEvent(userId, PiggyBankActivityType.ADDED_PIGGY_BANK, piggyBank.getName(), piggyBank.getGoalType(), piggyBank.getGoalAmount(), null, LocalDateTime.now()));
        PiggyBankSettings settings = settingsFactory.createDefaultPiggyBankSettings(saved);
        piggyBankSettingsRepository.save(settings);

        return saved.getId();
    }

    @Transactional
    public Long editPiggyBank(Long userId, PiggyBankDto piggyBankDto, Long piggyBankId) {
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);

        if (piggyBankRepository.existsByNameIgnoreCase(userId, piggyBankDto.name())
                && !piggyBank.getName().equalsIgnoreCase(piggyBankDto.name())) {
            throw new EntityAlreadyExistsException("This piggy bank name already exists");
        }

        PiggyBankValidator.validateGoalAmount(piggyBankDto);

        piggyBank.setName(piggyBankDto.name());
        piggyBank.setGoalAmount(piggyBankDto.goalAmount());
        piggyBank.setGoalType(piggyBankDto.goalType());

        kafkaTemplate.send("activity.piggybank", new PiggyBankActivityEvent(userId, PiggyBankActivityType.EDITED_PIGGY_BANK, piggyBank.getName(), piggyBank.getGoalType(), piggyBank.getGoalAmount(), null, LocalDateTime.now()));
        PiggyBank saved = piggyBankRepository.save(piggyBank);

        return saved.getId();
    }

    public List<PiggyBankDto> getAllPiggyBanks(Long userId) {
        List<PiggyBank> piggyBanks = piggyBankRepository.findAllByUserId(userId);

        return piggyBanks.stream()
                .map(piggyBank -> piggyBankMapper.mapToPiggyBankDto(piggyBank, PiggyBankCalculator.calculateProgress(piggyBank),
                        PiggyBankCheckGoalCompletion.isGoalCompleted(piggyBank)))
                .toList();
    }

    @Transactional
    public void deletePiggyBank(Long userId, Long piggyBankId) {
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);

        if (piggyBank == null || piggyBank.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidInputException("Cannot delete piggy bank with balance.  Withdraw funds first.");
        }
        recurringSettingsRepository.findByUserIdAndPiggyBankId(userId, piggyBankId)
                .ifPresent(settings -> {
                    settings.setEnable(false);
                    settings.setPiggyBankId(null);
                    settings.setNextExecutionDate(null);
                });

        kafkaTemplate.send("activity.piggybank", new PiggyBankActivityEvent(userId, PiggyBankActivityType.DELETED_PIGGY_BANK, piggyBank.getName(), piggyBank.getGoalType(), piggyBank.getGoalAmount(), null, LocalDateTime.now()));
        piggyBankRepository.delete(piggyBank);
    }
}
