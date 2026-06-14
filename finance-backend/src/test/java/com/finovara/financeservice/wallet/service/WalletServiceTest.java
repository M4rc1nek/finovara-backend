package com.finovara.authbackend.wallet.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.authbackend.util.wallet.WalletManagerService;
import com.finovara.authbackend.wallet.dto.WalletDto;
import com.finovara.authbackend.wallet.model.Wallet;
import com.finovara.authbackend.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {
    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletManagerService walletManagerService;

    @InjectMocks
    private WalletService walletService;

    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;
    }

    @Nested
    class GetWalletForUser {
        @Test
        void shouldReturnExistingWallet() {
            Wallet wallet = Wallet.create(userId);
            wallet.deposit(new BigDecimal("100"));

            when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

            WalletDto result = walletService.getWalletForUser(userId);

            assertEquals(new BigDecimal("100"), result.balance());
            assertEquals(wallet.getId(), result.id());
            assertEquals(userId, result.userId());

            verify(walletRepository, never()).save(any());
        }

        @Test
        void shouldCreateWalletWhenNotExist() {
            Wallet newWallet = Wallet.create(userId);

            when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(walletRepository.save(any(Wallet.class))).thenReturn(newWallet);

            WalletDto result = walletService.getWalletForUser(userId);

            assertEquals(BigDecimal.ZERO, result.balance());
            assertEquals(userId, result.userId());

            verify(walletRepository).save(any(Wallet.class));
        }
    }

    @Nested
    class AddBalanceToWallet {

        @Test
        void shouldAddBalanceSuccessfully() {
            Wallet wallet = Wallet.create(userId);
            wallet.deposit(new BigDecimal("100"));

            when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);

            WalletDto result = walletService.addBalanceToWallet(userId, new BigDecimal("50"));

            assertEquals(new BigDecimal("150"), result.balance());
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNegative() {
            Wallet wallet = Wallet.create(userId);

            when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);

            assertThrows(InvalidInputException.class, () -> walletService.addBalanceToWallet(userId, new BigDecimal("-10")));

            verifyNoInteractions(walletRepository);
        }

    }

    @Nested
    class RemoveBalanceFromWallet {
        @Test
        void shouldRemoveBalanceSuccessfully() {
            Wallet wallet = Wallet.create(userId);
            wallet.deposit(new BigDecimal("100"));

            when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);

            WalletDto result = walletService.removeBalanceFromWallet(userId, new BigDecimal("50"));

            assertEquals(new BigDecimal("50"), result.balance());
        }

        @Test
        void shouldThrowExceptionWhenInsufficientFunds() {
            Wallet wallet = Wallet.create(userId);
            wallet.deposit(new BigDecimal("30"));

            when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);

            assertThrows(InvalidInputException.class, () -> walletService.removeBalanceFromWallet(userId, new BigDecimal("50")));

            verifyNoInteractions(walletRepository);
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNegative() {
            Wallet wallet = Wallet.create(userId);
            wallet.deposit(new BigDecimal("100"));

            when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);

            assertThrows(InvalidInputException.class, () -> walletService.removeBalanceFromWallet(userId, new BigDecimal("-10")));

            verifyNoInteractions(walletRepository);
        }
    }
}