package com.finovara.notificationservice.notification.service.piggybank;

import com.finovara.corebackend.notification.dto.NotificationResponse;
import com.finovara.corebackend.piggybank.model.PiggyBank;
import com.finovara.corebackend.piggybank.repository.PiggyBankRepository;
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
class PiggyBankWarningTest {

    @Mock
    private PiggyBankRepository piggyBankRepository;

    @InjectMocks
    private PiggyBankWarningService piggyBankWarningService;

    private Long userId;

    private PiggyBank piggyBank;

    @BeforeEach
    void setUp() {
        userId = 1L;

        piggyBank = new PiggyBank();
        piggyBank.setId(10L);
        piggyBank.setName("PiggyBank");

        when(piggyBankRepository.findAllByUserAssignedId(userId)).thenReturn(List.of(piggyBank));
    }

    @Test
    void shouldReturnNotificationWhenThresholdReached() {
        piggyBank.setAmount(BigDecimal.valueOf(80));
        piggyBank.setGoalAmount(BigDecimal.valueOf(100));

        List<NotificationResponse> result = piggyBankWarningService.getNotifications(userId);

        assertEquals(1, result.size());
    }

    @Test
    void shouldReturnNotificationWhenExactly75() {
        piggyBank.setAmount(BigDecimal.valueOf(75));
        piggyBank.setGoalAmount(BigDecimal.valueOf(100));

        List<NotificationResponse> result = piggyBankWarningService.getNotifications(userId);

        assertEquals(1, result.size());
    }

    @Test
    void shouldNotReturnNotificationWhenBelow75() {
        piggyBank.setAmount(BigDecimal.valueOf(50));
        piggyBank.setGoalAmount(BigDecimal.valueOf(100));

        List<NotificationResponse> result = piggyBankWarningService.getNotifications(userId);

        assertTrue(result.isEmpty());
    }


    @Test
    void shouldReturnEmptyListWhenNoPiggyBanks() {
        when(piggyBankRepository.findAllByUserAssignedId(userId)).thenReturn(List.of());

        List<NotificationResponse> result = piggyBankWarningService.getNotifications(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnOnlyWarningsWhenMultiplePiggyBanksExist() {
        PiggyBank piggyBankOne = new PiggyBank();
        piggyBankOne.setId(10L);
        piggyBankOne.setName("A");
        piggyBankOne.setAmount(BigDecimal.valueOf(80));
        piggyBankOne.setGoalAmount(BigDecimal.valueOf(100));

        PiggyBank piggyBankTwo = new PiggyBank();
        piggyBankTwo.setId(20L);
        piggyBankTwo.setName("B");
        piggyBankTwo.setAmount(BigDecimal.valueOf(120));
        piggyBankTwo.setGoalAmount(BigDecimal.valueOf(100));

        when(piggyBankRepository.findAllByUserAssignedId(userId)).thenReturn(List.of(piggyBankOne, piggyBankTwo));

        List<NotificationResponse> result = piggyBankWarningService.getNotifications(userId);

        assertEquals(1, result.size());
    }
}