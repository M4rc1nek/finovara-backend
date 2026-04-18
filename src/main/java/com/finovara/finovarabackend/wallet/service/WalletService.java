package com.finovara.finovarabackend.wallet.service;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import com.finovara.finovarabackend.util.wallet.WalletManagerService;
import com.finovara.finovarabackend.wallet.dto.WalletDto;
import com.finovara.finovarabackend.wallet.model.Wallet;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.function.BiFunction;

@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepository;
    private final UserManagerService userManagerService;
    private final WalletManagerService walletManagerService;

    public WalletDto addBalanceToWallet(Long userId, BigDecimal amount) {
        return modifyWalletBalance(userId, amount, BigDecimal::add);
    }

    public WalletDto removeBalanceFromWallet(Long userId, BigDecimal amount) {
        Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(userId);
        if (wallet == null || wallet.getBalance().compareTo(amount) < 0) {
            throw new InvalidInputException("Insufficient funds");
        }
        return modifyWalletBalance(userId, amount, BigDecimal::subtract);
    }

    public WalletDto getWalletForUser(Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);

        Wallet wallet = walletRepository.findByUserAssignedId(userId).orElse(null);
        if (wallet == null) {
            wallet = Wallet.builder()
                    .balance(BigDecimal.ZERO)
                    .userAssigned(user).build();
            walletRepository.save(wallet);
        }

        return returnNewWalletDto(user, wallet);
    }

    private WalletDto modifyWalletBalance(Long userId, BigDecimal amount, BiFunction<BigDecimal, BigDecimal, BigDecimal> operation) {
        validateAmount(amount);

        User user = userManagerService.getUserByIdOrThrow(userId);
        Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(userId);

        BigDecimal newBalance = operation.apply(wallet.getBalance(), amount);
        wallet.setBalance(newBalance);

        walletRepository.save(wallet);
        return returnNewWalletDto(user, wallet);
    }

    private WalletDto returnNewWalletDto(User user, Wallet wallet) {
        return new WalletDto(
                wallet.getId(),
                user.getId(),
                wallet.getBalance());
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be non negative");
        }
    }
}