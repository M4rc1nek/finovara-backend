package com.finovara.financeservice.util.limit.validator;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.financeservice.limit.dto.LimitDto;
import com.finovara.financeservice.sharedaccount.limit.dto.SharedLimitDto;
import com.finovara.financeservice.util.periodbalance.FinancialPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class LimitExpensesValidator {
    private final FinancialPeriodService financialPeriodService;

    public void validateCurrentExpensesDoNotExceedLimit(Long userId, LimitDto limitDto) {
        BigDecimal spent = financialPeriodService.getExpensesSum(userId, limitDto.periodType(), limitDto.category());

        if (spent.compareTo(limitDto.amount()) > 0) {
            throw new InvalidInputException("Current expenses (" + spent + ") already exceed the proposed limit (" + limitDto.amount() + ")");
        }

    }

    public void validateCurrentSharedExpensesDoNotExceedLimit(Long userId, SharedLimitDto sharedLimitDto) {
        BigDecimal spent = financialPeriodService.getExpensesSum(userId, sharedLimitDto.periodType(), sharedLimitDto.category());

        if (spent.compareTo(sharedLimitDto.amount()) > 0) {
            throw new InvalidInputException("Current expenses (" + spent + ") already exceed the proposed limit (" + sharedLimitDto.amount() + ")");
        }
    }
}
