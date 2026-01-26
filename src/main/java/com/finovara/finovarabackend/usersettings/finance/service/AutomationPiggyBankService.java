package com.finovara.finovarabackend.usersettings.finance.service;

import com.finovara.finovarabackend.exception.PiggyBankNotFoundException;
import com.finovara.finovarabackend.exception.UserNotFoundException;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersettings.finance.dto.AutomationPiggyBankDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AutomationPiggyBankService {

    private final UserRepository userRepository;
    private final PiggyBankRepository piggyBankRepository;

    @Transactional
    public void createAutomation(String email, Long piggyBankId, AutomationPiggyBankDto automationPiggyBankDto) {
        User user = getUserOrThrow(email);
        PiggyBank piggyBank = getPiggyBankOrThrow(piggyBankId);

        if (!piggyBank.getUserAssigned().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Not your piggy bank");
        }

        piggyBank.setAutomationActive(automationPiggyBankDto.isAutomationActive());

        if (piggyBank.isAutomationActive()) {
            validatePercentage(automationPiggyBankDto);
            piggyBank.setAutomationPercentage(automationPiggyBankDto.percentage());
        } else {
            piggyBank.setAutomationPercentage(BigDecimal.ZERO);
        }

    }

    @Transactional
    public AutomationPiggyBankDto getAutomation(String email, Long piggyBankId) {
        User user = getUserOrThrow(email);
        PiggyBank piggyBank = getPiggyBankOrThrow(piggyBankId);

        if (!piggyBank.getUserAssigned().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Not your piggy bank");
        }

        return new AutomationPiggyBankDto(
                piggyBank.isAutomationActive(),
                piggyBank.getAutomationPercentage()
        );
    }

    private void validatePercentage(AutomationPiggyBankDto automationPiggyBankDto) {
        if (automationPiggyBankDto.isAutomationActive()) {
            if (automationPiggyBankDto.percentage() == null) {
                throw new IllegalArgumentException("Percentage is required");
            }

            if (automationPiggyBankDto.percentage().compareTo(BigDecimal.ZERO) <= 0 ||
                    automationPiggyBankDto.percentage().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Percentage must be between 1 and 100");
            }
        }

    }

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private PiggyBank getPiggyBankOrThrow(Long piggyBankId) {
        return piggyBankRepository.findById(piggyBankId)
                .orElseThrow(() -> new PiggyBankNotFoundException("PiggyBank not found"));
    }

}
