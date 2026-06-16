package com.finovara.financeservice.util.wallet;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WalletManagerService {
    private final WalletRepository walletRepository;

    public Wallet getWalletByUserIdOrThrow(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Wallet not found for this user"));
    }
}
