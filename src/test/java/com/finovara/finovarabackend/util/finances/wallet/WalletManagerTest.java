package com.finovara.finovarabackend.util.finances.wallet;

import com.finovara.finovarabackend.exception.notfound.WalletNotFoundException;
import com.finovara.finovarabackend.wallet.model.Wallet;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import com.finovara.finovarabackend.util.wallet.WalletManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletManagerTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletManagerService walletManagerService;

    @Test
    void shouldReturnWalletWhenUserIdExists() {
        Wallet wallet = new Wallet();
        wallet.setId(1L);

        when(walletRepository.findByUserAssignedId(1L)).thenReturn(Optional.of(wallet));

        Wallet result = walletManagerService.getWalletByUserIdOrThrow(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    void shouldThrowWalletNotFoundExceptionWhenUserIdDoesNotExist() {
        when(walletRepository.findByUserAssignedId(1L)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> walletManagerService.getWalletByUserIdOrThrow(1L));
    }
}