package com.finovara.financeservice.util.transaction.piggybank.manager;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.piggybank.repository.PiggyBankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PiggyBankManagerService {
    private final PiggyBankRepository piggyBankRepository;

    public PiggyBank getPiggyBankByUserId(Long piggyBankId, Long userId) {
        return piggyBankRepository.findByIdAndUserId(piggyBankId, userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Piggy Bank not found"));
    }
}
