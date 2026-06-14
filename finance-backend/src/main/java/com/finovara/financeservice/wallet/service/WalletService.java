package com.finovara.financeservice.wallet.service;

import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.dto.WalletDto;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.repository.WalletRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepository;
    private final WalletManagerService walletManagerService;

    @Transactional
    public WalletDto addBalanceToWallet(Long userId, BigDecimal amount) {
        Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(userId);

        wallet.deposit(amount);
        log.info("Adding balance to wallet for userId: {}", userId);

        return returnNewWalletDto(userId, wallet);
    }

    @Transactional
    public WalletDto removeBalanceFromWallet(Long userId, BigDecimal amount) {
        Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(userId);

        wallet.withdraw(amount);
        log.info("Withdrawing balance from wallet for userId: {}", userId);

        return returnNewWalletDto(userId, wallet);
    }

    @Transactional
    public WalletDto getWalletForUser(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(Wallet.create(userId)));

        return returnNewWalletDto(userId, wallet);
    }

    private WalletDto returnNewWalletDto(Long userId, Wallet wallet) {
        return new WalletDto(
                wallet.getId(),
                userId,
                wallet.getBalance());
    }

}
