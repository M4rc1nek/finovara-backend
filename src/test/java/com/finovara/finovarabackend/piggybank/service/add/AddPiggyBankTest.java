package com.finovara.finovarabackend.piggybank.service.add;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.exception.conflict.NameAlreadyExistsException;
import com.finovara.finovarabackend.piggybank.dto.PiggyBankDTO;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.model.PiggyBankGoalType;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.piggybank.service.PiggyBankService;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.factory.SettingsFactory;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.usersetting.piggybank.repository.PiggyBankSettingsRepository;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddPiggyBankTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private PiggyBankRepository piggyBankRepository;
    @Mock
    private PiggyBankActivityService piggyBankActivityService;
    @Mock
    private PiggyBankSettingsRepository piggyBankSettingsRepository;
    @Mock
    private SettingsFactory settingsFactory;

    @InjectMocks
    private PiggyBankService piggyBankService;

    @Test
    void shouldAddPiggyBankSuccessfully() {
        String email = "test@email.com";
        User user = new User();
        user.setId(1L);

        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setId(1L);
        piggyBank.setUserAssigned(user);

        PiggyBankSettings settings = new PiggyBankSettings();

        PiggyBankDTO dto = new PiggyBankDTO(null, null, "PiggyBank", BigDecimal.valueOf(100),
                null, PiggyBankGoalType.GIFTS, BigDecimal.valueOf(230), null, null);

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(piggyBankRepository.countPiggyBanksByUserId(user.getId())).thenReturn(0L);
        when(piggyBankRepository.existsByNameAndUserAssignedId(dto.name(), user.getId())).thenReturn(false);
        when(piggyBankRepository.save(any(PiggyBank.class))).thenReturn(piggyBank);
        when(settingsFactory.createDefaultPiggyBankSettings(any())).thenReturn(settings);

        piggyBankService.addPiggyBank(dto, email);

        verify(piggyBankRepository).save(any(PiggyBank.class));
        verify(piggyBankActivityService).createSimplePiggyBankActivity(eq(email), any(PiggyBank.class), eq(PiggyBankActivityType.ADDED_PIGGY_BANK));
        verify(settingsFactory).createDefaultPiggyBankSettings(any(PiggyBank.class));
        verify(piggyBankSettingsRepository).save(settings);
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        String email = "test@email.com";
        PiggyBankDTO dto = new PiggyBankDTO(null, null, "PiggyBank", BigDecimal.valueOf(100),
                null, PiggyBankGoalType.GIFTS, BigDecimal.valueOf(230), null, null);

        when(userManagerService.getUserByEmailOrThrow(email))
                .thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> piggyBankService.addPiggyBank(dto, email));

        verify(piggyBankRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenMaxPiggyBanksReached() {
        String email = "test@email.com";
        User user = new User();
        user.setId(1L);

        PiggyBankDTO dto = new PiggyBankDTO(null, null, "PiggyBank", BigDecimal.valueOf(100),
                null, PiggyBankGoalType.GIFTS, BigDecimal.valueOf(230), null, null);

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(piggyBankRepository.countPiggyBanksByUserId(user.getId())).thenReturn(5L);

        assertThrows(InvalidInputException.class, () -> piggyBankService.addPiggyBank(dto, email));
        verify(piggyBankRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenNameAlreadyExists() {
        String email = "test@email.com";
        User user = new User();
        user.setId(1L);

        PiggyBankDTO dto = new PiggyBankDTO(null, null, "PiggyBank", BigDecimal.valueOf(100),
                null, PiggyBankGoalType.GIFTS, BigDecimal.valueOf(230), null, null);

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(piggyBankRepository.countPiggyBanksByUserId(user.getId())).thenReturn(0L);
        when(piggyBankRepository.existsByNameAndUserAssignedId(dto.name(), user.getId())).thenReturn(true);

        assertThrows(NameAlreadyExistsException.class, () -> piggyBankService.addPiggyBank(dto, email));
        verify(piggyBankRepository, never()).save(any());
    }
}