package com.finovara.financeservice.sharedaccount.mapper.piggybank;

import com.finovara.financeservice.sharedaccount.dto.piggybank.SharedPiggyBankDto;
import com.finovara.financeservice.sharedaccount.model.piggybank.SharedPiggyBank;
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
