package com.finovara.corebackend.usersetting.finances.expense.controlamount.service;

import com.finovara.contracts.event.activity.settings.SettingsActivityEvent;
import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.model.activity.SettingType;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.usersetting.finances.expense.controlamount.dto.ControlAmountDto;
import com.finovara.corebackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.corebackend.util.user.service.UserManagerService;
import org.springframework.transaction.annotation.Transactional;
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

    private final UserManagerService userManagerService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public void saveExpenseAmountControl(Long userId, ControlAmountDto controlAmountDto) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

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
        User user = userManagerService.getUserByIdOrThrow(userId);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        return new ControlAmountDto(expenseSettings.isAmountThresholdEnabled(), expenseSettings.getBlockedAmount());
    }

    public void handleExpenseAmountControl(Long userId, BigDecimal newAmount) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        BigDecimal blockedAmount = Optional.ofNullable(expenseSettings.getBlockedAmount()).orElse(BigDecimal.ZERO);

        if (expenseSettings.isAmountThresholdEnabled() && newAmount.compareTo(blockedAmount) > 0) {
            throw new InvalidInputException("You have exceeded the amount, your amount: " + newAmount + " , amount blocked: " + blockedAmount);

        }
    }
}
