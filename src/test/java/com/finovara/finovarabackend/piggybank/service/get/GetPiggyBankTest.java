package com.finovara.finovarabackend.piggybank.service.get;

import com.finovara.finovarabackend.piggybank.dto.PiggyBankDTO;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.piggybank.service.PiggyBankService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetPiggyBankTest {

    @InjectMocks
    private PiggyBankService piggyBankService;
    @Mock
    private UserManagerService userManagerService;
    @Mock
    private PiggyBankRepository piggyBankRepository;

    @Test
    void shouldReturnAllPiggyBanksForUser() {
        String email = "test@email.com";

        User user = new User();
        user.setId(1L);

        PiggyBank piggy1 = new PiggyBank();
        piggy1.setId(10L);
        piggy1.setName("Piggy 1");
        piggy1.setAmount(new BigDecimal("100"));

        PiggyBank piggy2 = new PiggyBank();
        piggy2.setId(11L);
        piggy2.setName("Piggy 2");
        piggy2.setAmount(new BigDecimal("200"));

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(piggyBankRepository.findAllByUserAssignedEmail(email)).thenReturn(List.of(piggy1, piggy2));

        List<PiggyBankDTO> result = piggyBankService.getAllPiggyBanks(email);

        assertEquals(2, result.size());
        assertEquals("Piggy 1", result.get(0).name());
        assertEquals("Piggy 2", result.get(1).name());
        assertEquals(new BigDecimal("100"), result.get(0).amount());
        assertEquals(new BigDecimal("200"), result.get(1).amount());
        assertEquals(1L, result.get(0).userId());
        assertEquals(1L, result.get(1).userId());

        verify(userManagerService).getUserByEmailOrThrow(email);
        verify(piggyBankRepository).findAllByUserAssignedEmail(email);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoPiggyBanks() {
        String email = "test@email.com";

        User user = new User();
        user.setId(1L);

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(piggyBankRepository.findAllByUserAssignedEmail(email)).thenReturn(List.of());

        List<PiggyBankDTO> result = piggyBankService.getAllPiggyBanks(email);

        assertEquals(0, result.size());

        verify(userManagerService).getUserByEmailOrThrow(email);
        verify(piggyBankRepository).findAllByUserAssignedEmail(email);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        String email = "notfound@email.com";

        when(userManagerService.getUserByEmailOrThrow(email))
                .thenThrow(new RuntimeException("User not found"));

        assertThrows(RuntimeException.class, () -> piggyBankService.getAllPiggyBanks(email));

        verify(userManagerService).getUserByEmailOrThrow(email);
        verifyNoInteractions(piggyBankRepository);
    }
}