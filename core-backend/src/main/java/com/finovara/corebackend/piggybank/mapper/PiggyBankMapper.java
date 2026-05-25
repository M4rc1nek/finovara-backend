package com.finovara.corebackend.piggybank.mapper;

import com.finovara.corebackend.piggybank.dto.PiggyBankDto;
import com.finovara.corebackend.piggybank.model.PiggyBank;
import com.finovara.corebackend.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class PiggyBankMapper {
    public PiggyBankDto mapToPiggyBankDto(PiggyBank piggyBank, User user, Double progress, boolean goalCompleted) {
        return new PiggyBankDto(
                piggyBank.getId(),
                user.getId(),
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
