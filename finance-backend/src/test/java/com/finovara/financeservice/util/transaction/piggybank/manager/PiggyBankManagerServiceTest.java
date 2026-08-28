package com.finovara.financeservice.util.transaction.piggybank.manager;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.piggybank.repository.PiggyBankRepository;
import com.finovara.financeservice.util.transaction.piggybank.manager.PiggyBankManagerService;
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
class PiggyBankManagerServiceTest {

    @Mock
    private PiggyBankRepository piggyBankRepository;

    @InjectMocks
    private PiggyBankManagerService piggyBankManagerService;

    @Test
    void shouldReturnPiggyBankWhenExistsForUser() {
        Long piggyBankId = 1L;
        Long userId = 100L;
        PiggyBank piggyBank = new PiggyBank();

        when(piggyBankRepository.findByIdAndUserId(piggyBankId, userId)).thenReturn(Optional.of(piggyBank));

        PiggyBank result = piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId);

        assertEquals(piggyBank, result);

        verify(piggyBankRepository).findByIdAndUserId(piggyBankId, userId);
    }

    @Test
    void shouldThrowExceptionWhenPiggyBankDoesNotExist() {
        Long piggyBankId = 1L;
        Long userId = 100L;

        when(piggyBankRepository.findByIdAndUserId(piggyBankId, userId)).thenReturn(Optional.empty());

        assertThrows(RequestedEntityNotFoundException.class, () -> piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId));

        verify(piggyBankRepository).findByIdAndUserId(piggyBankId, userId);
    }
}