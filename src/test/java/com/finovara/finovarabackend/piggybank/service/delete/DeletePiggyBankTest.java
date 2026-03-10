package com.finovara.finovarabackend.piggybank.service.delete;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.piggybank.service.PiggyBankService;
import com.finovara.finovarabackend.util.service.piggybank.PiggyBankManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeletePiggyBankTest {

    @InjectMocks
    private PiggyBankService piggyBankService;

    @Mock
    private PiggyBankManagerService piggyBankManagerService;
    @Mock
    private PiggyBankActivityService piggyBankActivityService;
    @Mock
    private PiggyBankRepository piggyBankRepository;

    @Test
    void shouldDeletePiggyBankWhenBalanceIsZero() {
        String email = "test@email.com";
        Long piggyBankId = 1L;

        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(BigDecimal.ZERO);

        when(piggyBankManagerService.getPiggyBankByUserEmail(piggyBankId, email)).thenReturn(piggyBank);

        piggyBankService.deletePiggyBank(email, piggyBankId);

        verify(piggyBankActivityService).createSimplePiggyBankActivity(email, piggyBank, PiggyBankActivityType.DELETED_PIGGY_BANK);
        verify(piggyBankRepository).delete(piggyBank);
    }

    @Test
    void shouldThrowExceptionWhenPiggyBankIsNull() {
        String email = "test@email.com";
        Long piggyBankId = 1L;

        when(piggyBankManagerService.getPiggyBankByUserEmail(piggyBankId, email)).thenReturn(null);

        assertThrows(InvalidInputException.class, () -> piggyBankService.deletePiggyBank(email, piggyBankId));

        verify(piggyBankActivityService, never()).createSimplePiggyBankActivity(anyString(), any(), any());
        verify(piggyBankRepository, never()).delete(any());
    }

    @Test
    void shouldThrowExceptionWhenBalanceIsGreaterThanZero() {
        String email = "test@email.com";
        Long piggyBankId = 1L;

        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(new BigDecimal("100"));

        when(piggyBankManagerService.getPiggyBankByUserEmail(piggyBankId, email)).thenReturn(piggyBank);

        assertThrows(InvalidInputException.class, () -> piggyBankService.deletePiggyBank(email, piggyBankId));

        verify(piggyBankActivityService, never()).createSimplePiggyBankActivity(anyString(), any(), any());
        verify(piggyBankRepository, never()).delete(any());
    }
}