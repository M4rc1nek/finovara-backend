package com.finovara.finovarabackend.util.manager.service.wallet;

import com.finovara.finovarabackend.exception.notfound.WalletNotFoundException;
import com.finovara.finovarabackend.wallet.model.Wallet;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import com.finovara.finovarabackend.util.service.wallet.WalletManagerService;
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
class WalletManagerServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletManagerService walletManagerService;

    @Test
    void shouldReturnWalletWhenEmailExists() {
        Wallet wallet = new Wallet();
        wallet.setId(1L);

        when(walletRepository.findByUserAssignedEmail("test@example.com")).thenReturn(Optional.of(wallet));

        Wallet result = walletManagerService.getWalletByUserEmailOrThrow("test@example.com");

        assertEquals(1L, result.getId());
    }

    @Test
    void shouldThrowWalletNotFoundExceptionWhenEmailDoesNotExist() {
        when(walletRepository.findByUserAssignedEmail("test@example.com")).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> walletManagerService.getWalletByUserEmailOrThrow("test@example.com"));
    }
}