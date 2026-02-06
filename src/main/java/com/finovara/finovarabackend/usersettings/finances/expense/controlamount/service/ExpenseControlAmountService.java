package com.finovara.finovarabackend.usersettings.finances.expense.controlamount.service;

import com.finovara.finovarabackend.exception.InvalidInputException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersettings.finances.expense.controlamount.dto.ExpenseControlAmountDto;
import com.finovara.finovarabackend.usersettings.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExpenseControlAmountService {

    private final UserManagerService userManagerService;

    @Transactional
    public void saveExpenseAmountControl(String email, ExpenseControlAmountDto expenseControlAmountDto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        expenseSettings.setExpenseAmountThresholdEnabled(expenseControlAmountDto.expenseAmountThresholdEnabled());
        expenseSettings.setBlockedAmount(expenseControlAmountDto.blockedAmount());
    }

    @Transactional
    public ExpenseControlAmountDto getExpenseAmountControl(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        return new ExpenseControlAmountDto(expenseSettings.isExpenseAmountThresholdEnabled(), expenseSettings.getBlockedAmount());
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
