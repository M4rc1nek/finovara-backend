package com.finovara.corebackend.wallet.service;

import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.util.user.service.UserManagerService;
import com.finovara.corebackend.util.wallet.WalletManagerService;
import com.finovara.corebackend.wallet.dto.WalletDto;
import com.finovara.corebackend.wallet.model.Wallet;
import com.finovara.corebackend.wallet.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepository;
    private final UserManagerService userManagerService;
    private final WalletManagerService walletManagerService;

    @Transactional
    public WalletDto addBalanceToWallet(Long userId, BigDecimal amount) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(userId);

        wallet.deposit(amount);
        log.info("Adding balance to wallet for userId: {}", userId);

        return returnNewWalletDto(user, wallet);
    }

    @Transactional
    public WalletDto removeBalanceFromWallet(Long userId, BigDecimal amount) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(userId);

        wallet.withdraw(amount);
        log.info("Withdrawing balance from wallet for userId: {}", userId);

        return returnNewWalletDto(user, wallet);
    }

    @Transactional
    public WalletDto getWalletForUser(Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);

        Wallet wallet = walletRepository.findByUserAssignedId(userId)
                .orElseGet(() -> walletRepository.save(Wallet.create(user)));

        return returnNewWalletDto(user, wallet);
    }

    private WalletDto returnNewWalletDto(User user, Wallet wallet) {
        return new WalletDto(
                wallet.getId(),
                user.getId(),
                wallet.getBalance());
    }

}