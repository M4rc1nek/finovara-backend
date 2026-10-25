package com.finovara.financeservice.util.sharedaccount.piggybank;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.sharedaccount.piggybank.model.SharedPiggyBank;
import com.finovara.financeservice.sharedaccount.piggybank.repository.SharedPiggyBankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SharedPiggyBankManager {
    private final SharedPiggyBankRepository sharedPiggyBankRepository;

    public SharedPiggyBank getSharedPiggyBankByIdAndUserId(Long piggyBankId, Long userId) {
        return sharedPiggyBankRepository.findByIdAndOwnerIdOrMemberId(piggyBankId, userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Piggy Bank not found"));
    }

    public SharedPiggyBank getSharedPiggyBankForUpdateByIdAndUserId(Long piggyBankId, Long userId) {
        return sharedPiggyBankRepository.findByIdAndOwnerIdOrMemberIdForUpdate(piggyBankId, userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Piggy bank not found"));
    }
}

