package com.finovara.finovarabackend.usersetting.piggybank.rondup.service.get;

import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.usersetting.piggybank.roundup.dto.RoundUpDto;
import com.finovara.finovarabackend.usersetting.piggybank.roundup.service.RoundUpService;
import com.finovara.finovarabackend.util.service.piggybank.manager.PiggyBankManagerService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetRoundUpTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private PiggyBankManagerService piggyBankManagerService;

    @InjectMocks
    private RoundUpService roundUpService;

    private PiggyBank piggyBank;
    private final String EMAIL = "test@test.com";
    private final Long PIGGY_BANK_ID = 1L;

    @BeforeEach
    void setup() {
        piggyBank = new PiggyBank();
        PiggyBankSettings settings = new PiggyBankSettings();
        piggyBank.setSettings(settings);

        when(piggyBankManagerService.getPiggyBankByUserEmail(PIGGY_BANK_ID, EMAIL)).thenReturn(piggyBank);
    }

    @Test
    void shouldReturnRoundUpActiveTrue() {
        piggyBank.getSettings().setRoundUpActive(true);

        RoundUpDto result = roundUpService.getRoundUp(EMAIL, PIGGY_BANK_ID);

        assertEquals(true, result.roundUpActive());
    }

    @Test
    void shouldReturnRoundUpActiveFalse() {
        piggyBank.getSettings().setRoundUpActive(false);

        RoundUpDto result = roundUpService.getRoundUp(EMAIL, PIGGY_BANK_ID);

        assertEquals(false, result.roundUpActive());
    }
}