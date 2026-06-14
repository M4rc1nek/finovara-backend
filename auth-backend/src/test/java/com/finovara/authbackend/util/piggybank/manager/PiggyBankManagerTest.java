package com.finovara.authbackend.util.piggybank.manager;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.authbackend.piggybank.model.PiggyBank;
import com.finovara.authbackend.piggybank.repository.PiggyBankRepository;
import com.finovara.authbackend.user.model.User;
import com.finovara.authbackend.util.user.service.UserManagerService;
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
class PiggyBankManagerTest {

    private static final Long USER_ID = 1L;

    @Mock
    private PiggyBankRepository piggyBankRepository;

    @Mock
    private UserManagerService userManagerService;

    @InjectMocks
    private PiggyBankManagerService piggyBankManagerService;

    @Test
    void shouldReturnPiggyBankWhenIdAndEmailExist() {
        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setId(1L);

        User user = new User();
        user.setId(USER_ID);

        when(userManagerService.getUserByEmailOrThrow("test@example.com")).thenReturn(user);
        when(piggyBankRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(piggyBank));

        PiggyBank result = piggyBankManagerService.getPiggyBankByUserEmail(1L, "test@example.com");

        assertEquals(1L, result.getId());
    }

    @Test
    void shouldThrowPiggyBankNotFoundExceptionWhenIdAndEmailDoNotExist() {
        User user = new User();
        user.setId(USER_ID);

        when(userManagerService.getUserByEmailOrThrow("test@example.com")).thenReturn(user);
        when(piggyBankRepository.findByIdAndUserId(1L, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(RequestedEntityNotFoundException.class, () -> piggyBankManagerService.getPiggyBankByUserEmail(1L, "test@example.com"));
    }
}
