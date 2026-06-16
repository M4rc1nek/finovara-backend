package com.finovara.financeservice.util.wallet;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.repository.WalletRepository;
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

        Wallet wallet = Wallet.create(1L);

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        Wallet result = walletManagerService.getWalletByUserIdOrThrow(userId);

        assertEquals(wallet, result);
        verify(walletRepository).findByUserId(userId);
    }

    @Test
    void shouldThrowExceptionWhenWalletNotFound() {
        Long userId = 1L;

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(RequestedEntityNotFoundException.class, () -> walletManagerService.getWalletByUserIdOrThrow(userId));

        verify(walletRepository).findByUserId(userId);
    }
}