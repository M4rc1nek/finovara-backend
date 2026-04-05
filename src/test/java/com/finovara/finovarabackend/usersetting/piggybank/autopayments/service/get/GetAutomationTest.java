package com.finovara.finovarabackend.usersetting.piggybank.autopayments.service.get;

import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.dto.AutoPaymentsDto;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.service.AutoPaymentsService;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAutomationTest {

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
        settings.setAutomationActive(true);
        settings.setAutomationPercentage(BigDecimal.valueOf(15));
        piggyBank.setSettings(settings);

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
        when(piggyBankManagerService.getPiggyBankByUserEmail(1L, EMAIL)).thenReturn(piggyBank);
    }

    @Test
    void shouldReturnCorrectAutomationSettings() {
        AutoPaymentsDto result = autoPaymentsService.getAutomation(EMAIL, 1L);

        assertEquals(true, result.isAutomationActive());
        assertEquals(BigDecimal.valueOf(15), result.percentage());
    }

    @Test
    void shouldReturnZeroPercentageIfAutomationInactive() {
        piggyBank.getSettings().setAutomationActive(false);
        piggyBank.getSettings().setAutomationPercentage(BigDecimal.ZERO);

        AutoPaymentsDto result = autoPaymentsService.getAutomation(EMAIL, 1L);

        assertEquals(false, result.isAutomationActive());
        assertEquals(BigDecimal.ZERO, result.percentage());
    }
}