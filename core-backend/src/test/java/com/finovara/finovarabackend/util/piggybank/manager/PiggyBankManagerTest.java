package com.finovara.finovarabackend.util.piggybank.manager;

import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.finovarabackend.util.piggybank.exception.notfound.PiggyBankNotFoundException;
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

    @Mock
    private PiggyBankRepository piggyBankRepository;

    @InjectMocks
    private PiggyBankManagerService piggyBankManagerService;

    @Test
    void shouldReturnPiggyBankWhenIdAndEmailExist() {
        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setId(1L);

        when(piggyBankRepository.findByIdAndUserAssignedEmail(1L, "test@example.com")).thenReturn(Optional.of(piggyBank));

        PiggyBank result = piggyBankManagerService.getPiggyBankByUserEmail(1L, "test@example.com");

        assertEquals(1L, result.getId());
    }

    @Test
    void shouldThrowPiggyBankNotFoundExceptionWhenIdAndEmailDoNotExist() {
        when(piggyBankRepository.findByIdAndUserAssignedEmail(1L, "test@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(PiggyBankNotFoundException.class, () -> piggyBankManagerService.getPiggyBankByUserEmail(1L, "test@example.com"));
    }
}