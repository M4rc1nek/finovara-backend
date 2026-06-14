package com.finovara.authbackend.util.piggybank.manager;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.authbackend.piggybank.model.PiggyBank;
import com.finovara.authbackend.piggybank.repository.PiggyBankRepository;
import com.finovara.authbackend.util.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PiggyBankManagerService {
    private final PiggyBankRepository piggyBankRepository;
    private final UserManagerService userManagerService;

    public PiggyBank getPiggyBankByUserId(Long piggyBankId, Long userId) {
        return piggyBankRepository.findByIdAndUserId(piggyBankId, userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Piggy Bank not found"));
    }

    public PiggyBank getPiggyBankByUserEmail(Long piggyBankId, String email) {
        Long userId = userManagerService.getUserByEmailOrThrow(email).getId();
        return piggyBankRepository.findByIdAndUserId(piggyBankId, userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Piggy Bank not found"));
    }
}
