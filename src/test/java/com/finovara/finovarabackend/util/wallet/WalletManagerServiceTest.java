package com.finovara.finovarabackend.util.wallet;

import com.finovara.finovarabackend.exception.notfound.WalletNotFoundException;
import com.finovara.finovarabackend.wallet.model.Wallet;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletManagerServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletManagerService walletManagerService;

    @Test
    void shouldReturnWalletWhenExists() {
        Long userId = 1L;

        Wallet wallet = new Wallet();

        when(walletRepository.findByUserAssignedId(userId)).thenReturn(Optional.of(wallet));

        Wallet result = walletManagerService.getWalletByUserIdOrThrow(userId);

        assertEquals(wallet, result);
        verify(walletRepository).findByUserAssignedId(userId);
    }

    @Test
    void shouldThrowExceptionWhenWalletNotFound() {
        Long userId = 1L;

        when(walletRepository.findByUserAssignedId(userId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> walletManagerService.getWalletByUserIdOrThrow(userId));

        verify(walletRepository).findByUserAssignedId(userId);
    }
}