package com.finovara.finovarabackend.usersettings.finances.expense.controlamount.service;

import com.finovara.finovarabackend.exception.InvalidInputException;
import com.finovara.finovarabackend.exception.NotAuthorizedException;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersettings.finances.expense.controlamount.dto.ExpenseControlAmountDto;
import com.finovara.finovarabackend.util.service.expense.ExpenseManagerService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExpenseControlAmountService {

    private final UserManagerService userManagerService;
    private final ExpenseManagerService expenseManagerService;

    @Transactional
    public void addExpenseAmountControl(String email, Long expenseId, ExpenseControlAmountDto expenseControlAmountDto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        Expense expense = expenseManagerService.getExpenseByUserIdOrThrow(expenseId, user.getId());

        if (!expense.getUserAssigned().getId().equals(user.getId())) {
            throw new NotAuthorizedException("Not your Expense");
        }

        expense.setExpenseAmountThresholdEnabled(expenseControlAmountDto.expenseAmountThresholdEnabled());
        expense.setBlockedAmount(expenseControlAmountDto.blockedAmount());
    }

    @Transactional
    public ExpenseControlAmountDto getExpenseAmountControl(String email, Long expenseId) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        Expense expense = expenseManagerService.getExpenseByUserIdOrThrow(expenseId, user.getId());

        if (!expense.getUserAssigned().getId().equals(user.getId())) {
            throw new NotAuthorizedException("Not your expense");
        }

        return new ExpenseControlAmountDto(expenseId, expense.isExpenseAmountThresholdEnabled(), expense.getBlockedAmount());
    }

    @Transactional
    public void saveExpenseAmountControl(String email, List<ExpenseControlAmountDto> settings) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        for (ExpenseControlAmountDto expenseControlAmountDto : settings) {
            Expense expense = expenseManagerService.getExpenseByIdOrThrow(expenseControlAmountDto.expenseId());

            if (!expense.getUserAssigned().getId().equals(user.getId())) {
                throw new NotAuthorizedException("Not your expense");
            }

            expense.setExpenseAmountThresholdEnabled(expenseControlAmountDto.expenseAmountThresholdEnabled());
            expense.setBlockedAmount(expenseControlAmountDto.blockedAmount());
        }

    }

    public void handleExpenseAmountControl(String email, Long expenseId, BigDecimal newAmount) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        Expense expense = expenseManagerService.getExpenseByUserIdOrThrow(expenseId, user.getId());

        if (expense.getBlockedAmount() == null) {
            expense.setBlockedAmount(BigDecimal.ZERO);
        }

        BigDecimal blockedAmount = Optional.ofNullable(expense.getBlockedAmount()).orElse(BigDecimal.ZERO);

        if (expense.isExpenseAmountThresholdEnabled() && newAmount.compareTo(blockedAmount) > 0) {
            throw new InvalidInputException("You have exceeded the amount, your amount: " + newAmount + " , amount blocked: " + blockedAmount);
        }
    }

}
