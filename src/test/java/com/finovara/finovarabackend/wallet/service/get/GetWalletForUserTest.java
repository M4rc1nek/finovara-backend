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

    private final Long USER_ID = 1L;

    @Test
    void shouldReturnExistingWallet() {
        User user = new User();
        user.setId(USER_ID);

        Wallet wallet = new Wallet();
        wallet.setId(4L);
        wallet.setBalance(new BigDecimal("100"));

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
        when(walletRepository.findByUserAssignedId(USER_ID)).thenReturn(Optional.of(wallet));

        WalletDto result = walletService.getWalletForUser(USER_ID);

        assertEquals(new BigDecimal("100"), result.balance());
        assertEquals(4L, result.id());
        assertEquals(USER_ID, result.userId());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void shouldCreateWalletWhenNotExist() {
        User user = new User();
        user.setId(USER_ID);

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
        when(walletRepository.findByUserAssignedId(USER_ID)).thenReturn(Optional.empty());

        WalletDto result = walletService.getWalletForUser(USER_ID);

        assertEquals(BigDecimal.ZERO, result.balance());
        assertEquals(USER_ID, result.userId());
        verify(walletRepository).save(any(Wallet.class));
    }
}