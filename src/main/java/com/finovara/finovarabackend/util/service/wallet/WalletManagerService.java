package com.finovara.finovarabackend.util.service.wallet;

import com.finovara.finovarabackend.exception.WalletNotFoundException;
import com.finovara.finovarabackend.wallet.model.Wallet;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WalletManagerService {
    private final WalletRepository walletRepository;

    public Wallet getWalletByUserEmailOrThrow(String email) {
        return walletRepository.findByUserAssignedEmail(email)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for this user"));
    }
}
