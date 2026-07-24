package com.finovara.financeservice.sharedaccount.piggybank.mapper;

import com.finovara.financeservice.sharedaccount.piggybank.dto.SharedPiggyBankDto;
import com.finovara.financeservice.sharedaccount.piggybank.model.SharedPiggyBank;
import org.springframework.stereotype.Component;

@Component
public class SharedPiggyBankMapper {
    public SharedPiggyBankDto mapToPiggyBankDto(SharedPiggyBank sharedPiggyBank, Double progress) {
        return new SharedPiggyBankDto(
                sharedPiggyBank.getId(),
                sharedPiggyBank.getName(),
                sharedPiggyBank.getAmount(),
                sharedPiggyBank.getCreatedAt(),
                sharedPiggyBank.getGoalType(),
                sharedPiggyBank.getGoalAmount(),
                progress
        );
    }
}
