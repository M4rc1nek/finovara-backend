package com.finovara.finovarabackend.notification.service.piggybank;

import com.finovara.finovarabackend.notification.dto.NotificationResponse;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PiggyBankReachedTest {

    @Mock
    private PiggyBankRepository piggyBankRepository;

    @InjectMocks
    private PiggyBankReachedService piggyBankReachedService;

    private Long userId;
    private PiggyBank piggyBank;

    @BeforeEach
    void setUp() {
        userId = 1L;

        piggyBank = new PiggyBank();
        piggyBank.setId(10L);
        piggyBank.setName("Test PiggyBank");

        when(piggyBankRepository.findAllByUserAssignedId(userId)).thenReturn(List.of(piggyBank));
    }

    @Test
    void shouldReturnNotificationWhenGoalReached() {
        piggyBank.setAmount(BigDecimal.valueOf(100));
        piggyBank.setGoalAmount(BigDecimal.valueOf(100));

        List<NotificationResponse> result = piggyBankReachedService.getNotifications(userId);

        assertEquals(1, result.size());
    }

    @Test
    void shouldNotReturnNotificationWenBelowGoal() {
        piggyBank.setAmount(BigDecimal.valueOf(50));
        piggyBank.setGoalAmount(BigDecimal.valueOf(100));

        List<NotificationResponse> result = piggyBankReachedService.getNotifications(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnNotificationWhenAboveGoal() {
        piggyBank.setAmount(BigDecimal.valueOf(120));
        piggyBank.setGoalAmount(BigDecimal.valueOf(100));

        List<NotificationResponse> result = piggyBankReachedService.getNotifications(userId);

        assertEquals(1, result.size());
    }

    @Test
    void shouldReturnOnlyReachedPiggyBanksWhenMultipleExist() {
        piggyBank.setId(10L);
        piggyBank.setName("PiggyBank A");
        piggyBank.setAmount(BigDecimal.valueOf(100));
        piggyBank.setGoalAmount(BigDecimal.valueOf(100));

        PiggyBank piggyBank2 = new PiggyBank();
        piggyBank2.setId(20L);
        piggyBank2.setName("PiggyBank B");
        piggyBank2.setAmount(BigDecimal.valueOf(50));
        piggyBank2.setGoalAmount(BigDecimal.valueOf(100));

        when(piggyBankRepository.findAllByUserAssignedId(userId)).thenReturn(List.of(piggyBank, piggyBank2));

        List<NotificationResponse> result = piggyBankReachedService.getNotifications(userId);

        assertEquals(1, result.size());
    }

    @Test
    void shouldReturnEmptyListWhenNoPiggyBanks() {
        when(piggyBankRepository.findAllByUserAssignedId(userId)).thenReturn(List.of());

        List<NotificationResponse> result = piggyBankReachedService.getNotifications(userId);

        assertTrue(result.isEmpty());
    }
}