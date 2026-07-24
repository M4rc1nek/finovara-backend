package com.finovara.financeservice.sharedaccount.participants;

import com.finovara.financeservice.sharedaccount.wallet.dto.SharedWalletDto;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SharedAccountParticipantsService {
    private final SharedWalletService sharedWalletService;

    public SharedAccountParticipantsResponse getParticipants(Long userId) {
        SharedWalletDto wallet = sharedWalletService.getWallet(userId);
        return new SharedAccountParticipantsResponse(wallet.ownerId(), wallet.memberId());
    }
}
