package com.finovara.authbackend.piggybank.mapper;

import com.finovara.authbackend.piggybank.dto.PiggyBankDto;
import com.finovara.authbackend.piggybank.model.PiggyBank;
import org.springframework.stereotype.Component;

@Component
public class PiggyBankMapper {
    public PiggyBankDto mapToPiggyBankDto(PiggyBank piggyBank, Double progress, boolean goalCompleted) {
        return new PiggyBankDto(
                piggyBank.getId(),
                piggyBank.getUserId(),
                piggyBank.getName(),
                piggyBank.getAmount(),
                piggyBank.getCreatedAt(),
                piggyBank.getGoalType(),
                piggyBank.getGoalAmount(),
                progress,
                goalCompleted
        );
    }
}
