package com.finovara.finovarabackend.usersetting.piggybank.rondup.service.add;

import com.finovara.finovarabackend.piggybank.dto.PiggyBankDTO;
import com.finovara.finovarabackend.piggybank.model.PiggyBankGoalType;
import com.finovara.finovarabackend.piggybank.service.PiggyBankService;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.roundup.service.RoundUpService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddDefaultPiggyBankTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private PiggyBankService piggyBankService;

    @InjectMocks
    private RoundUpService roundUpService;

    private final String EMAIL = "test@test.com";

    @Test
    void shouldAddDefaultPiggyBank() {
        User user = new User();
        user.setEmail(EMAIL);

        PiggyBankDTO dto = new PiggyBankDTO(123L, 1L, "My piggy bank", new BigDecimal("100"),
                null, PiggyBankGoalType.GIFTS, new BigDecimal("250"), null, false);

        Long expectedId = 123L;

        when(piggyBankService.addPiggyBank(dto, EMAIL)).thenReturn(expectedId);
        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);

        Long result = roundUpService.addDefaultPiggyBank(dto, EMAIL);

        assertEquals(expectedId, result);
        verify(piggyBankService).addPiggyBank(dto, EMAIL);
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        PiggyBankDTO dto = new PiggyBankDTO(12L, null, "My piggy bank", new BigDecimal("50"),
                null, PiggyBankGoalType.GIFTS, new BigDecimal("100"), null, false);

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> roundUpService.addDefaultPiggyBank(dto, EMAIL));
    }
}