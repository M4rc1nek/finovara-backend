package com.finovara.finovarabackend.wallet.service;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import com.finovara.finovarabackend.util.wallet.WalletManagerService;
import com.finovara.finovarabackend.wallet.dto.WalletDto;
import com.finovara.finovarabackend.wallet.model.Wallet;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
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
    private UserManagerService userManagerService;

    @Mock
    private WalletManagerService walletManagerService;

    @InjectMocks
    private WalletService walletService;

    private Long userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = 1L;
        user = new User();
        user.setId(userId);
    }

    @Nested
    class GetWalletForUser {
        @Test
        void shouldReturnExistingWallet() {
            Wallet wallet = new Wallet();
            wallet.setId(4L);
            wallet.setBalance(new BigDecimal("100"));

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(walletRepository.findByUserAssignedId(userId)).thenReturn(Optional.of(wallet));

            WalletDto result = walletService.getWalletForUser(userId);

            assertEquals(new BigDecimal("100"), result.balance());
            assertEquals(4L, result.id());
            assertEquals(userId, result.userId());

            verify(walletRepository, never()).save(any());
        }

        @Test
        void shouldCreateWalletWhenNotExist() {
            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(walletRepository.findByUserAssignedId(userId)).thenReturn(Optional.empty());

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
            Wallet wallet = new Wallet();
            wallet.setBalance(new BigDecimal("100"));

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);

            WalletDto result = walletService.addBalanceToWallet(userId, new BigDecimal("50"));

            assertEquals(new BigDecimal("150"), result.balance());
            verify(walletRepository).save(wallet);
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNegative() {
            assertThrows(IllegalArgumentException.class, () -> walletService.addBalanceToWallet(userId, new BigDecimal("-10")));

            verifyNoInteractions(walletRepository);
        }

    }

    @Nested
    class RemoveBalanceFromWallet {
        @Test
        void shouldRemoveBalanceSuccessfully() {
            Wallet wallet = new Wallet();
            wallet.setBalance(new BigDecimal("100"));

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);

            WalletDto result = walletService.removeBalanceFromWallet(userId, new BigDecimal("50"));

            assertEquals(new BigDecimal("50"), result.balance());
            verify(walletRepository).save(wallet);
        }

        @Test
        void shouldThrowExceptionWhenInsufficientFunds() {
            Wallet wallet = Wallet.builder().balance(new BigDecimal("30")).build();

            when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);

            assertThrows(InvalidInputException.class, () -> walletService.removeBalanceFromWallet(userId, new BigDecimal("50")));

            verifyNoInteractions(userManagerService);
            verifyNoInteractions(walletRepository);
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNegative() {
            Wallet wallet = new Wallet();
            wallet.setBalance(new BigDecimal("100"));

            when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);

            assertThrows(IllegalArgumentException.class, () -> walletService.removeBalanceFromWallet(userId, new BigDecimal("-10")));

            verifyNoInteractions(userManagerService);
            verifyNoInteractions(walletRepository);
        }
    }
}