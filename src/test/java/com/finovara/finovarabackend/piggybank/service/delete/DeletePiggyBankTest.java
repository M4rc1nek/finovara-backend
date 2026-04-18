package com.finovara.finovarabackend.piggybank.service.delete;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.piggybank.service.PiggyBankManagementService;
import com.finovara.finovarabackend.util.piggybank.manager.PiggyBankManagerService;
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
    private PiggyBankManagementService piggyBankManagementService;

    @Mock
    private PiggyBankManagerService piggyBankManagerService;
    @Mock
    private PiggyBankActivityService piggyBankActivityService;
    @Mock
    private PiggyBankRepository piggyBankRepository;

    @Test
    void shouldDeletePiggyBankWhenBalanceIsZero() {
        Long userId = 1L;
        Long piggyBankId = 1L;

        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(BigDecimal.ZERO);

        when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

        piggyBankManagementService.deletePiggyBank(userId, piggyBankId);

        verify(piggyBankActivityService).createSimplePiggyBankActivity(userId, piggyBank, PiggyBankActivityType.DELETED_PIGGY_BANK);
        verify(piggyBankRepository).delete(piggyBank);
    }

    @Test
    void shouldThrowExceptionWhenPiggyBankIsNull() {
        Long userId = 1L;
        Long piggyBankId = 1L;

        when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(null);

        assertThrows(InvalidInputException.class, () -> piggyBankManagementService.deletePiggyBank(userId, piggyBankId));

        verify(piggyBankActivityService, never()).createSimplePiggyBankActivity(anyLong(), any(), any());
        verify(piggyBankRepository, never()).delete(any());
    }

    @Test
    void shouldThrowExceptionWhenBalanceIsGreaterThanZero() {
        Long userId = 1L;
        Long piggyBankId = 1L;

        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(new BigDecimal("100"));

        when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

        assertThrows(InvalidInputException.class, () -> piggyBankManagementService.deletePiggyBank(userId, piggyBankId));

        verify(piggyBankActivityService, never()).createSimplePiggyBankActivity(anyLong(), any(), any());
        verify(piggyBankRepository, never()).delete(any());
    }
}