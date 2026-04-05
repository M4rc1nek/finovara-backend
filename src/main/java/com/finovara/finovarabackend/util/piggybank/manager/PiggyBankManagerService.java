package com.finovara.finovarabackend.util.piggybank.manager;

import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.util.piggybank.exception.notfound.PiggyBankNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PiggyBankManagerService {
    private final PiggyBankRepository piggyBankRepository;

    public PiggyBank getPiggyBankByUserEmail(Long piggyBankId, String email) {
        return piggyBankRepository.findByIdAndUserAssignedEmail(piggyBankId, email)
                .orElseThrow(() -> new PiggyBankNotFoundException("Piggy Bank not found"));
    }
}
