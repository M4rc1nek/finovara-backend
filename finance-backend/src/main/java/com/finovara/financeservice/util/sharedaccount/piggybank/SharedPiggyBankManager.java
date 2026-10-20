package com.finovara.financeservice.util.sharedaccount.piggybank;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.sharedaccount.piggybank.model.SharedPiggyBank;
import com.finovara.financeservice.sharedaccount.piggybank.repository.SharedPiggyBankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SharedPiggyBankManager {
    private final SharedPiggyBankRepository piggyBankRepository;

    public SharedPiggyBank getSharedPiggyBankByIdAndUserId(Long piggyBankId, Long userId) {
        return piggyBankRepository.findByIdAndOwnerIdOrMemberId(piggyBankId, userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Piggy Bank not found"));
    }
}

