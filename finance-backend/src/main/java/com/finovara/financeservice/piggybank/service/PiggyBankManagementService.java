package com.finovara.financeservice.piggybank.service;

import com.finovara.contracts.datadeletable.UserDataDeletable;
import com.finovara.contracts.event.activity.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.event.activity.piggybank.PiggyBankEditActivityEvent;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.model.transaction.PiggyBankGoalType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.piggybank.dto.PiggyBankDto;
import com.finovara.financeservice.piggybank.goalplanner.service.GoalPlannerService;
import com.finovara.financeservice.piggybank.mapper.PiggyBankMapper;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.piggybank.repository.PiggyBankRepository;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import com.finovara.financeservice.settings.piggybank.model.PiggyBankSettings;
import com.finovara.financeservice.settings.piggybank.repository.PiggyBankSettingsRepository;
import com.finovara.financeservice.util.piggybank.PiggyBankCalculator;
import com.finovara.financeservice.util.piggybank.PiggyBankCheckGoalCompletion;
import com.finovara.financeservice.util.piggybank.PiggyBankValidator;
import com.finovara.financeservice.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.contracts.auth.dto.ConfirmAuthorizationCodeDto;
import feign.FeignException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PiggyBankManagementService implements UserDataDeletable {

    private final PiggyBankRepository piggyBankRepository;
    private final PiggyBankManagerService piggyBankManagerService;
    private final OutboxService outboxService;
    private final PiggyBankSettingsRepository piggyBankSettingsRepository;
    private final RecurringSettingsRepository recurringSettingsRepository;
    private final PiggyBankMapper piggyBankMapper;
    private final PiggyBankSettingsFactory piggyBankSettingsFactory;
    private final GoalPlannerService goalPlannerService;
    private final AuthBackendClient authBackendClient;

    @Transactional
    public Long addPiggyBank(PiggyBankDto piggyBankDto, Long userId) {
        authBackendClient.confirmAuthorizationCode(userId, new ConfirmAuthorizationCodeDto(piggyBankDto.authorizationCode()));
        
        long currentPiggyBanks = piggyBankRepository.countPiggyBanksByUserId(userId);

        if (currentPiggyBanks >= 5) {
            throw new InvalidInputException("You have reached the maximum number of piggy banks: 5");
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
        outboxService.save("PiggyBank", saved.getId().toString(), "activity.piggybank.lifecycle",
                new PiggyBankActivityEvent(userId, PiggyBankActivityType.ADDED_PIGGY_BANK, piggyBank.getName(), piggyBank.getGoalType(), piggyBank.getGoalAmount(), null, LocalDateTime.now()));
        PiggyBankSettings settings = piggyBankSettingsFactory.createDefaultPiggyBankSettings(saved);
        piggyBankSettingsRepository.save(settings);

        log.info("Added a new Piggy Bank called {}, with a goal amount of {}", piggyBankDto.name(), piggyBankDto.goalAmount());
        return saved.getId();
    }

    @Transactional
    public Long editPiggyBank(Long userId, PiggyBankDto piggyBankDto, Long piggyBankId) {
        authBackendClient.confirmAuthorizationCode(userId, new ConfirmAuthorizationCodeDto(piggyBankDto.authorizationCode()));
        
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);

        if (piggyBankRepository.existsByNameIgnoreCase(userId, piggyBankDto.name())
                && !piggyBank.getName().equalsIgnoreCase(piggyBankDto.name())) {
            throw new EntityAlreadyExistsException("This piggy bank name already exists");
        }

        PiggyBankValidator.validateGoalAmount(piggyBankDto);

        String previousName = piggyBank.getName();
        PiggyBankGoalType previousGoalType = piggyBank.getGoalType();
        BigDecimal previousGoalAmount = piggyBank.getGoalAmount();

        piggyBank.setName(piggyBankDto.name());
        piggyBank.setGoalAmount(piggyBankDto.goalAmount());
        piggyBank.setGoalType(piggyBankDto.goalType());

        PiggyBank saved = piggyBankRepository.save(piggyBank);
        goalPlannerService.checkAndMarkGoalCompletion(saved.getGoalPlanner());
        outboxService.save("PiggyBank", saved.getId().toString(), "activity.piggybank.edited",
                new PiggyBankEditActivityEvent(userId, PiggyBankActivityType.EDITED_PIGGY_BANK, piggyBank.getName(), previousName,
                        piggyBank.getGoalType(), previousGoalType, piggyBank.getGoalAmount(), previousGoalAmount, LocalDateTime.now()));


        log.info("Edited Piggy Bank. New name: {}, new goal amount: {}", piggyBankDto.name(), piggyBankDto.goalAmount());

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
    public void deletePiggyBank(Long userId, Long piggyBankId, String authorizationCode) {
        authBackendClient.confirmAuthorizationCode(userId, new ConfirmAuthorizationCodeDto(authorizationCode));
        
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);

        if (piggyBank == null || piggyBank.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidInputException("Cannot delete piggy bank with balance. Withdraw funds first.");
        }

        recurringSettingsRepository.findByUserIdAndPiggyBankId(userId, piggyBankId)
                .ifPresent(settings -> {
                    settings.setEnable(false);
                    settings.setPiggyBankId(null);
                    settings.setNextExecutionDate(null);
                });

        outboxService.save("PiggyBank", piggyBankId.toString(), "activity.piggybank.lifecycle",
                new PiggyBankActivityEvent(userId, PiggyBankActivityType.DELETED_PIGGY_BANK, piggyBank.getName(), piggyBank.getGoalType(), piggyBank.getGoalAmount(), null, LocalDateTime.now()));
        piggyBankRepository.delete(piggyBank);
        log.info("Manual deleted piggyBank for userId={}", userId);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        piggyBankRepository.deleteByUserId(userId);
        log.info("Deleted piggyBank for userId={}", userId);
    }
}