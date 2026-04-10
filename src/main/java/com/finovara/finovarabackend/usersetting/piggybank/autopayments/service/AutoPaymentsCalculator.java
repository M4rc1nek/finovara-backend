package com.finovara.finovarabackend.usersetting.piggybank.autopayments.service;

import com.finovara.finovarabackend.usersetting.piggybank.autopayments.dto.AutoPaymentsDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AutoPaymentsCalculator {

    public BigDecimal calculate(BigDecimal revenue, BigDecimal percentage) {
        return revenue.multiply(percentage).divide(BigDecimal.valueOf(100));
    }

    public void validate(AutoPaymentsDto dto) {
        if (dto.isAutomationActive() && dto.percentage() == null) {
            throw new IllegalArgumentException("Percentage is required");
        }
    }
}