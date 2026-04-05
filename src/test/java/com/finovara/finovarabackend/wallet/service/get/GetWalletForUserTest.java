package com.finovara.finovarabackend.wallet.service.get;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetWalletForUserTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private UserManagerService userManagerService;

    @InjectMocks
    private WalletService walletService;

    private final String EMAIL = "test@mail.com";

    @Test
    void shouldReturnExistingWallet() {
        User user = new User();
        user.setId(1L);

        Wallet wallet = new Wallet();
        wallet.setId(4L);
        wallet.setBalance(new BigDecimal("100"));

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
        when(walletRepository.findByUserAssignedEmail(EMAIL)).thenReturn(Optional.of(wallet));

        WalletDto result = walletService.getWalletForUser(EMAIL);

        assertEquals(new BigDecimal("100"), result.balance());
        assertEquals(4L, result.id());
        assertEquals(1L, result.userId());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void shouldCreateWalletWhenNotExist() {
        User user = new User();
        user.setId(1L);

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
        when(walletRepository.findByUserAssignedEmail(EMAIL)).thenReturn(Optional.empty());

        WalletDto result = walletService.getWalletForUser(EMAIL);

        assertEquals(BigDecimal.ZERO, result.balance());
        assertEquals(1L, result.userId());
        verify(walletRepository).save(any(Wallet.class));
    }
}