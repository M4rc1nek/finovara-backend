package com.finovara.finovarabackend.util.wallet;

import com.finovara.finovarabackend.exception.notfound.WalletNotFoundException;
import com.finovara.finovarabackend.wallet.model.Wallet;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WalletManagerService {
    private final WalletRepository walletRepository;

    public Wallet getWalletByUserIdOrThrow(Long userId) {
        return walletRepository.findByUserAssignedId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for this user"));
    }
}
