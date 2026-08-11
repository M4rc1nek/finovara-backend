package com.finovara.financeservice.util.settings;

import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
import com.finovara.financeservice.exception.conflict.ConfirmationRequiredException;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class ExpenseAnomalyDetector {

    public BigDecimal calculateAnomalyThreshold(List<BigDecimal> lastAmounts, BigDecimal multiplier) {
        BigDecimal averageAmountExpense = lastAmounts.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(lastAmounts.size()), 2, RoundingMode.HALF_UP);

        return averageAmountExpense.multiply(multiplier);
    }

    public void requirePasswordConfirmation(Long userId, ConfirmPasswordDto confirmPasswordDto, AuthBackendClient authBackendClient) {
        if (confirmPasswordDto == null || confirmPasswordDto.password() == null) {
            throw new ConfirmationRequiredException("Unusual expense detected. Password confirmation required.");
        }

        authBackendClient.verifyPassword(userId, confirmPasswordDto);
    }
}