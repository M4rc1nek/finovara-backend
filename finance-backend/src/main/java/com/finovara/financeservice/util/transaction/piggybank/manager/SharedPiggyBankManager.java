package com.finovara.financeservice.util.transaction.piggybank.manager;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.sharedaccount.piggybank.model.SharedPiggyBank;
import com.finovara.financeservice.sharedaccount.piggybank.repository.SharedPiggyBankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SharedPiggyBankManager {
    private final SharedPiggyBankRepository piggyBankRepository;

    public SharedPiggyBank getPiggyBankByUserId(Long piggyBankId, Long userId) {
        return piggyBankRepository.findByIdAndUserId(piggyBankId, userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Piggy Bank not found"));
    }
}
