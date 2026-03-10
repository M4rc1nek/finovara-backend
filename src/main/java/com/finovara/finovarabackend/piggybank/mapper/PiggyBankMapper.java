package com.finovara.finovarabackend.piggybank.mapper;

import com.finovara.finovarabackend.piggybank.dto.PiggyBankDTO;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class PiggyBankMapper {
    public PiggyBankDTO mapToPiggyBankDto(PiggyBank piggyBank, User user, Double progress, boolean goalCompleted) {
        return new PiggyBankDTO(
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
