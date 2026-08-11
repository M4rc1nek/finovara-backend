package com.finovara.financeservice.settings.finances.expense.controlamount.service;

import com.finovara.contracts.event.activity.settings.SettingsActivityEvent;
import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.model.activity.SettingType;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.settings.finances.expense.controlamount.dto.ControlAmountDto;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.expense.repository.ExpenseSettingsRepository;
import org.springframework.transaction.annotation.Transactional;
import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ControlAmountService {

    private final ExpenseSettingsRepository expenseSettingsRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AuthBackendClient authBackendClient;
    private final AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Transactional
    public void saveExpenseAmountControl(Long userId, ControlAmountDto controlAmountDto) {
        ExpenseSettings expenseSettings = expenseSettingsRepository.findByUserId(userId);
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(controlAmountDto.authorizationCode()));

        BigDecimal blockedAmount = Optional.ofNullable(controlAmountDto.blockedAmount()).orElse(BigDecimal.ZERO);

        expenseSettings.setAmountThresholdEnabled(controlAmountDto.expenseAmountThresholdEnabled());
        expenseSettings.setBlockedAmount(blockedAmount);
        if (expenseSettings.isAmountThresholdEnabled()) {
            kafkaTemplate.send("activity.settings", new SettingsActivityEvent(userId, SettingType.EXPENSE_CONTROL_AMOUNT, SettingActivityStatus.ENABLED, LocalDateTime.now()));
        } else {
            kafkaTemplate.send("activity.settings", new SettingsActivityEvent(userId, SettingType.EXPENSE_CONTROL_AMOUNT, SettingActivityStatus.DISABLED, LocalDateTime.now()));
        }
    }

    @Transactional
    public ControlAmountDto getExpenseAmountControl(Long userId) {
        ExpenseSettings expenseSettings = expenseSettingsRepository.findByUserId(userId);

        return new ControlAmountDto(expenseSettings.isAmountThresholdEnabled(), expenseSettings.getBlockedAmount(), null);
    }

    public void handleExpenseAmountControl(Long userId, BigDecimal newAmount) {
        ExpenseSettings expenseSettings = expenseSettingsRepository.findByUserId(userId);

        BigDecimal blockedAmount = Optional.ofNullable(expenseSettings.getBlockedAmount()).orElse(BigDecimal.ZERO);

        if (expenseSettings.isAmountThresholdEnabled() && newAmount.compareTo(blockedAmount) > 0) {
            throw new InvalidInputException("You have exceeded the amount, your amount: " + newAmount + " , amount blocked: " + blockedAmount);

        }
    }
}
