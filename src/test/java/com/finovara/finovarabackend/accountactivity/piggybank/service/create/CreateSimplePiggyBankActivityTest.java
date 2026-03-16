package com.finovara.finovarabackend.accountactivity.piggybank.service.create;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.repository.PiggyBankActivityRepository;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.config.TimeConfig;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.model.PiggyBankGoalType;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class CreateSimplePiggyBankActivityTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private PiggyBankActivityRepository piggyBankActivityRepository;

    @Mock
    private TimeConfig timeConfig;

    @InjectMocks
    private PiggyBankActivityService piggyBankActivityService;

    private final String EMAIL = "test@mail.com";

    @Test
    void shouldCreateSimplePiggyBankActivitySuccessfully() {

        User user = new User();
        user.setId(1L);

        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setName("Vacation Fund");
        piggyBank.setGoalType(PiggyBankGoalType.ELECTRONICS);
        piggyBank.setGoalAmount(new BigDecimal("2000"));

        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), ZoneId.of("UTC"));

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
        when(timeConfig.clock()).thenReturn(fixedClock);
        piggyBankActivityService.createSimplePiggyBankActivity(EMAIL, piggyBank, PiggyBankActivityType.ADDED_PIGGY_BANK);

        verify(piggyBankActivityRepository).save(argThat(activity ->
                activity.getUserAssigned().equals(user) &&
                        activity.getPiggyBankName().equals("Vacation Fund") &&
                        activity.getActivityType() == PiggyBankActivityType.ADDED_PIGGY_BANK &&
                        activity.getGoalType() == PiggyBankGoalType.ELECTRONICS &&
                        activity.getGoalAmount().compareTo(new BigDecimal("2000")) == 0
        ));
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {

        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setName("Vacation Fund");
        piggyBank.setGoalType(PiggyBankGoalType.ELECTRONICS);
        piggyBank.setGoalAmount(new BigDecimal("2000"));

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () ->
                piggyBankActivityService.createSimplePiggyBankActivity(
                        EMAIL,
                        piggyBank,
                        PiggyBankActivityType.ADDED_PIGGY_BANK
                )
        );

        verify(piggyBankActivityRepository, never()).save(any());
    }
}