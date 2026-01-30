package com.finovara.finovarabackend.util.service.piggybank;

import com.finovara.finovarabackend.exception.PiggyBankNotFoundException;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PiggyBankManagerService {
    private final PiggyBankRepository piggyBankRepository;

    public PiggyBank getPiggyBankOrThrow(Long piggyBankId) {
        return piggyBankRepository.findById(piggyBankId)
                .orElseThrow(() -> new PiggyBankNotFoundException("Piggy Bank not found"));
    }

    public PiggyBank getPiggyBankByUserEmail(Long piggyBankId, String email) {
        return piggyBankRepository.findByIdAndUserAssignedEmail(piggyBankId, email)
                .orElseThrow(() -> new PiggyBankNotFoundException("Piggy Bank not found"));
    }
}
