package com.finovara.finovarabackend.usersettings.finances.expense.controlamount.service;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersettings.finances.expense.controlamount.dto.ControlAmountDto;
import com.finovara.finovarabackend.usersettings.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
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

    @Transactional
    public void saveExpenseAmountControl(String email, ControlAmountDto controlAmountDto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        BigDecimal blockedAmount = Optional.ofNullable(expenseSettings.getBlockedAmount()).orElse(BigDecimal.ZERO);

        expenseSettings.setExpenseAmountThresholdEnabled(controlAmountDto.expenseAmountThresholdEnabled());
        expenseSettings.setBlockedAmount(blockedAmount);
        log.info("Saved ExpenseAmountControl settings. IsEnabled: {}, BlockedAmount: {}", controlAmountDto.expenseAmountThresholdEnabled(), controlAmountDto.blockedAmount());
    }

    @Transactional
    public ControlAmountDto getExpenseAmountControl(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        return new ControlAmountDto(expenseSettings.isExpenseAmountThresholdEnabled(), expenseSettings.getBlockedAmount());
    }

    public void handleExpenseAmountControl(String email, BigDecimal newAmount) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        if (expenseSettings.getBlockedAmount() == null) {
            expenseSettings.setBlockedAmount(BigDecimal.ZERO);
        }

        BigDecimal blockedAmount = Optional.ofNullable(expenseSettings.getBlockedAmount()).orElse(BigDecimal.ZERO);

        if (expenseSettings.isExpenseAmountThresholdEnabled() && newAmount.compareTo(blockedAmount) > 0) {
            throw new InvalidInputException("You have exceeded the amount, your amount: " + newAmount + " , amount blocked: " + blockedAmount);

        }
    }
}
