package com.finovara.finovarabackend.piggybank.service;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.exception.conflict.NameAlreadyExistsException;
import com.finovara.finovarabackend.piggybank.dto.PiggyBankDto;
import com.finovara.finovarabackend.piggybank.mapper.PiggyBankMapper;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.model.PiggyBankGoalType;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.factory.SettingsFactory;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.usersetting.piggybank.repository.PiggyBankSettingsRepository;
import com.finovara.finovarabackend.util.piggybank.PiggyBankCalculator;
import com.finovara.finovarabackend.util.piggybank.PiggyBankCheckGoalCompletion;
import com.finovara.finovarabackend.util.piggybank.PiggyBankValidator;
import com.finovara.finovarabackend.util.piggybank.exception.notfound.PiggyBankNotFoundException;
import com.finovara.finovarabackend.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PiggyBankManagementService {

    private final UserManagerService userManagerService;
    private final PiggyBankRepository piggyBankRepository;
    private final PiggyBankManagerService piggyBankManagerService;
    private final PiggyBankActivityService piggyBankActivityService;
    private final PiggyBankSettingsRepository piggyBankSettingsRepository;
    private final SettingsFactory settingsFactory;
    private final PiggyBankMapper piggyBankMapper;

    @Transactional
    public Long addPiggyBank(PiggyBankDto piggyBankDto, String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        long currentPiggyBanks = piggyBankRepository.countPiggyBanksByUserId(user.getId());

        int maxPiggyBanks = 5;
        if (currentPiggyBanks >= maxPiggyBanks) {
            throw new InvalidInputException("you have reached the maximum number of piggy banks: " + maxPiggyBanks);
        }

        if (piggyBankRepository.existsByNameAndUserAssignedId(piggyBankDto.name(), user.getId())) {
            throw new NameAlreadyExistsException("This piggy bank name already exists");
        }

        PiggyBankValidator.validateGoalAmount(piggyBankDto);

        PiggyBank piggyBank = PiggyBank.builder()
                .name(piggyBankDto.name())
                .amount(BigDecimal.ZERO)
                .createdAt(LocalDate.now())
                .userAssigned(user)
                .goalAmount(piggyBankDto.goalAmount())
                .goalType(piggyBankDto.goalType())
                .build();

        PiggyBank saved = piggyBankRepository.save(piggyBank);
        piggyBankActivityService.createSimplePiggyBankActivity(email, piggyBank, PiggyBankActivityType.ADDED_PIGGY_BANK);
        PiggyBankSettings settings = settingsFactory.createDefaultPiggyBankSettings(saved);
        piggyBankSettingsRepository.save(settings);

        return saved.getId();
    }

    @Transactional
    public Long editPiggyBank(String email, PiggyBankDto piggyBankDto, Long piggyBankId) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        PiggyBank piggyBank = piggyBankManagerService.getPiggyBankByUserEmail(piggyBankId, email);

        //safety check
        if (piggyBank.getUserAssigned() == null || !piggyBank.getUserAssigned().getId().equals(user.getId())) {
            throw new PiggyBankNotFoundException("Piggy bank not found for this user");
        }

        if (piggyBankRepository.existsByNameAndUserAssignedId(piggyBankDto.name(), user.getId())
                && !piggyBank.getName().equals(piggyBankDto.name())) {
            throw new NameAlreadyExistsException("This piggy bank name already exists");
        }

        PiggyBankValidator.validateGoalAmount(piggyBankDto);

        String previousName = piggyBank.getName();
        BigDecimal previousGoalAmount = piggyBank.getGoalAmount();
        PiggyBankGoalType previousGoalType = piggyBank.getGoalType();

        piggyBank.setName(piggyBankDto.name());
        piggyBank.setGoalAmount(piggyBankDto.goalAmount());
        piggyBank.setGoalType(piggyBankDto.goalType());

        piggyBankActivityService.createEditPiggyBankActivity(email, piggyBank, PiggyBankActivityType.EDITED_PIGGY_BANK, previousGoalAmount, previousGoalType, previousName);
        PiggyBank saved = piggyBankRepository.save(piggyBank);

        return saved.getId();
    }

    public List<PiggyBankDto> getAllPiggyBanks(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        List<PiggyBank> piggyBanks = piggyBankRepository.findAllByUserAssignedId(user.getId());

        return piggyBanks.stream()
                .map(piggyBank -> piggyBankMapper.mapToPiggyBankDto(piggyBank, user, PiggyBankCalculator.calculateProgress(piggyBank),
                        PiggyBankCheckGoalCompletion.isGoalCompleted(piggyBank)))
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
}
