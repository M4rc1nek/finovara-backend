package com.finovara.finovarabackend.wallet.service;

import com.finovara.finovarabackend.exception.InvalidInputException;
import com.finovara.finovarabackend.exception.UserNotFoundException;
import com.finovara.finovarabackend.exception.WalletNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.wallet.dto.WalletDTO;
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
    private final UserRepository userRepository;

    public WalletDTO addBalanceToWallet(String email, BigDecimal amount) {
        return modifyWalletBalance(email, amount, BigDecimal::add);
    }

    public WalletDTO removeBalanceFromWallet(String email, BigDecimal amount) {
        Wallet wallet = getWalletOrThrow(email);
        if (wallet == null || wallet.getBalance().compareTo(amount) < 0) {
            throw new InvalidInputException("Insufficient funds");
        }
        return modifyWalletBalance(email, amount, BigDecimal::subtract);
    }

    public WalletDTO getWalletForUser(String email) {
        User user = getUserByEmailOrThrow(email);

        Wallet wallet = walletRepository.findByUserAssignedEmail(email).orElse(null);
        if (wallet == null) {
            wallet = Wallet.builder()
                    .balance(BigDecimal.ZERO)
                    .userAssigned(user).build();
            walletRepository.save(wallet);
        }

        return returnNewWalletDTO(user, wallet);
    }

    private WalletDTO modifyWalletBalance(String email, BigDecimal amount, BiFunction<BigDecimal, BigDecimal, BigDecimal> operation) {
        validateAmount(amount);

        User user = getUserByEmailOrThrow(email);
        Wallet wallet = getWalletOrThrow(email);

        BigDecimal newBalance = operation.apply(wallet.getBalance(), amount);
        wallet.setBalance(newBalance);

        walletRepository.save(wallet);
        return returnNewWalletDTO(user, wallet);
    }

    private User getUserByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private Wallet getWalletOrThrow(String email) {
        return walletRepository.findByUserAssignedEmail(email)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for this user"));
    }

    private WalletDTO returnNewWalletDTO(User user, Wallet wallet) {
        return new WalletDTO(
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