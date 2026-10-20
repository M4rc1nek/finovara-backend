package com.finovara.financeservice.util.sharedaccount.wallet;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.sharedaccount.wallet.model.SharedWallet;
import com.finovara.financeservice.sharedaccount.wallet.repository.SharedWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SharedWalletManager {
    private final SharedWalletRepository sharedWalletRepository;

    public SharedWallet getWalletOrThrow(Long callerId) {
        return sharedWalletRepository.findByOwnerIdOrMemberId(callerId, callerId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Shared wallet not found for userId=" + callerId));
    }

    public SharedWallet getWalletForUpdateOrThrow(Long callerId) {
        return sharedWalletRepository.findByOwnerIdOrMemberIdForUpdate(callerId, callerId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Shared wallet not found for userId=" + callerId));
    }

}

