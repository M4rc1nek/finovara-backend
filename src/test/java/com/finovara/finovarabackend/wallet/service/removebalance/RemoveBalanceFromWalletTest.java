package com.finovara.finovarabackend.wallet.service.removebalance;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import com.finovara.finovarabackend.util.wallet.WalletManagerService;
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

    private final Long USER_ID = 1L;

    @Test
    void ShouldRemoveBalanceSuccessfully() {
        User user = new User();
        user.setId(USER_ID);

        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("100"));

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
        when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);

        WalletDto result = walletService.removeBalanceFromWallet(USER_ID, new BigDecimal("50"));

        assertEquals(new BigDecimal("50"), result.balance());
        verify(walletRepository).save(wallet);
    }

    @Test
    void shouldThrowExceptionWhenInsufficientFunds() {
        Wallet wallet = Wallet.builder().balance(new BigDecimal("30")).build();

        when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);

        assertThrows(InvalidInputException.class, () -> walletService.removeBalanceFromWallet(USER_ID, new BigDecimal("50")));

        verify(walletRepository, never()).save(any());
        verifyNoInteractions(userManagerService);
    }

    @Test
    void shouldThrowExceptionWhenAmountIsNegative() {
        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("100"));

        when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);

        assertThrows(IllegalArgumentException.class, () -> walletService.removeBalanceFromWallet(USER_ID, new BigDecimal("-10")));

        verify(walletRepository, never()).save(any());
        verifyNoInteractions(userManagerService);
    }
}