package com.finovara.finovarabackend.usersetting.piggybank.autopayments.service.create;

import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.dto.AutoPaymentsDto;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.service.AutoPaymentsService;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.piggybank.manager.PiggyBankManagerService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAutomationTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private PiggyBankManagerService piggyBankManagerService;

    @InjectMocks
    private AutoPaymentsService autoPaymentsService;

    private final String EMAIL = "test@test.com";
    private PiggyBank piggyBank;

    @BeforeEach
    void setup() {
        User user = new User();
        user.setEmail(EMAIL);

        piggyBank = new PiggyBank();
        PiggyBankSettings settings = new PiggyBankSettings();
        piggyBank.setSettings(settings);

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
        when(piggyBankManagerService.getPiggyBankByUserEmail(1L, EMAIL)).thenReturn(piggyBank);
    }

    @Test
    void shouldActivateAutomationWithPercentage() {
        AutoPaymentsDto dto = new AutoPaymentsDto(true, BigDecimal.valueOf(20));

        autoPaymentsService.createAutomation(EMAIL, 1L, dto);

        assertTrue(piggyBank.getSettings().isAutomationActive());
        assertThat(BigDecimal.valueOf(20)).isEqualByComparingTo(piggyBank.getSettings().getAutomationPercentage());
    }

    @Test
    void shouldDeactivateAutomationAndResetPercentage() {
        AutoPaymentsDto dto = new AutoPaymentsDto(false, null);

        autoPaymentsService.createAutomation(EMAIL, 1L, dto);

        assertFalse(piggyBank.getSettings().isAutomationActive());
        assertEquals(BigDecimal.ZERO, piggyBank.getSettings().getAutomationPercentage());
    }
}