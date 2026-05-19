package com.finovara.finovarabackend.usersetting.finances.expense.controlamount.service;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.controlamount.dto.ControlAmountDto;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ControlAmountService {

    private final UserManagerService userManagerService;
    private final SettingsActivityService settingsActivityService;

    @Transactional
    public void saveExpenseAmountControl(Long userId, ControlAmountDto controlAmountDto) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        BigDecimal blockedAmount = Optional.ofNullable(controlAmountDto.blockedAmount()).orElse(BigDecimal.ZERO);

        expenseSettings.setAmountThresholdEnabled(controlAmountDto.expenseAmountThresholdEnabled());
        expenseSettings.setBlockedAmount(blockedAmount);
        if(expenseSettings.isAmountThresholdEnabled()){
            settingsActivityService.createSettingActivity(userId, SettingActivityStatus.ENABLED, SettingType.EXPENSE_CONTROL_AMOUNT);
        }else {
            settingsActivityService.createSettingActivity(userId, SettingActivityStatus.DISABLED, SettingType.EXPENSE_CONTROL_AMOUNT);
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
