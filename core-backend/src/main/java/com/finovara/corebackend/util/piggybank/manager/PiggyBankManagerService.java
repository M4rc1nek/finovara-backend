package com.finovara.corebackend.util.piggybank.manager;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.corebackend.piggybank.model.PiggyBank;
import com.finovara.corebackend.piggybank.repository.PiggyBankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PiggyBankManagerService {
    private final PiggyBankRepository piggyBankRepository;

    public PiggyBank getPiggyBankByUserId(Long piggyBankId, Long userId) {
        return piggyBankRepository.findByIdAndUserAssignedId(piggyBankId, userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Piggy Bank not found"));
    }

    public PiggyBank getPiggyBankByUserEmail(Long piggyBankId, String email) {
        return piggyBankRepository.findByIdAndUserAssignedEmail(piggyBankId, email)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Piggy Bank not found"));
    }
}
