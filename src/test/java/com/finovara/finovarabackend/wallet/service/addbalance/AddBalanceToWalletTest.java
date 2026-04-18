package com.finovara.finovarabackend.wallet.service.addbalance;

import com.finovara.finovarabackend.wallet.service.WalletService;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import com.finovara.finovarabackend.util.wallet.WalletManagerService;
import com.finovara.finovarabackend.wallet.model.Wallet;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.wallet.dto.WalletDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AddBalanceToWalletTest {

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
    void shouldAddBalanceSuccessfully() {
        User user = new User();
        user.setId(USER_ID);

        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("100"));

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
        when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);

        WalletDto result = walletService.addBalanceToWallet(USER_ID, new BigDecimal("50"));

        assertEquals(new BigDecimal("150"), result.balance());
        verify(walletRepository).save(wallet);
    }

    @Test
    void shouldThrowExceptionWhenAmountIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> walletService.addBalanceToWallet(USER_ID, new BigDecimal("-10")));

        verify(walletRepository, never()).save(any());
    }
}