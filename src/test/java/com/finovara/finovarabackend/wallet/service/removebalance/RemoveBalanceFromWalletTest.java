package com.finovara.finovarabackend.wallet.service.removebalance;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import com.finovara.finovarabackend.util.service.wallet.WalletManagerService;
import com.finovara.finovarabackend.wallet.dto.WalletDto;
import com.finovara.finovarabackend.wallet.model.Wallet;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import com.finovara.finovarabackend.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemoveBalanceFromWalletTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private UserManagerService userManagerService;
    @Mock
    private WalletManagerService walletManagerService;

    @InjectMocks
    private WalletService walletService;

    private final String EMAIL = "test@mail.com";

    @Test
    void ShouldRemoveBalanceSuccessfully() {
        User user = new User();
        user.setId(1L);

        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("100"));

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
        when(walletManagerService.getWalletByUserEmailOrThrow(EMAIL)).thenReturn(wallet);

        WalletDto result = walletService.removeBalanceFromWallet(EMAIL, new BigDecimal("50"));

        assertEquals(new BigDecimal("50"), result.balance());
        verify(walletRepository).save(wallet);
    }

    @Test
    void shouldThrowExceptionWhenInsufficientFunds() {
        Wallet wallet = Wallet.builder().balance(new BigDecimal("30")).build();

        when(walletManagerService.getWalletByUserEmailOrThrow(EMAIL)).thenReturn(wallet);

        assertThrows(InvalidInputException.class, () -> walletService.removeBalanceFromWallet(EMAIL, new BigDecimal("50")));

        verify(walletRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenAmountIsNegative() {
        assertThrows(InvalidInputException.class, () -> walletService.removeBalanceFromWallet(EMAIL, new BigDecimal("-10")));

        verify(walletRepository, never()).save(any());
    }
}