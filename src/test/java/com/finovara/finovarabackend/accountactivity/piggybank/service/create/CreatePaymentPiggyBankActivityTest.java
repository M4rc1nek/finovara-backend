package com.finovara.finovarabackend.accountactivity.piggybank.service.create;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.repository.PiggyBankActivityRepository;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.model.PiggyBankGoalType;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class CreatePaymentPiggyBankActivityTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private PiggyBankActivityRepository piggyBankActivityRepository;

    @InjectMocks
    private PiggyBankActivityService piggyBankActivityService;

    private final Long USER_ID = 1L;

    @Test
    void shouldCreatePaymentPiggyBankActivitySuccessfully() {

        User user = new User();
        user.setId(USER_ID);

        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setName("Vacation Fund");
        piggyBank.setGoalType(PiggyBankGoalType.ELECTRONICS);
        piggyBank.setGoalAmount(new BigDecimal("2000"));

        BigDecimal paidAmount = new BigDecimal("500");
        LocalDateTime now = LocalDateTime.now();

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

        piggyBankActivityService.createPaymentPiggyBankActivity(USER_ID, piggyBank, PiggyBankActivityType.ADDED_PIGGY_BANK, paidAmount);

        verify(piggyBankActivityRepository).save(argThat(activity ->
                activity.getUserAssigned().equals(user) &&
                        activity.getPiggyBankName().equals("Vacation Fund") &&
                        activity.getActivityType() == PiggyBankActivityType.ADDED_PIGGY_BANK &&
                        activity.getGoalType() == PiggyBankGoalType.ELECTRONICS &&
                        activity.getGoalAmount().compareTo(new BigDecimal("2000")) == 0 &&
                        activity.getAmountPaid().compareTo(paidAmount) == 0 &&
                        !activity.getCreatedAt().isBefore(now)
        ));
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {

        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setName("Vacation Fund");
        piggyBank.setGoalType(PiggyBankGoalType.ELECTRONICS);
        piggyBank.setGoalAmount(new BigDecimal("2000"));

        BigDecimal paidAmount = new BigDecimal("500");

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () ->
                piggyBankActivityService.createPaymentPiggyBankActivity(
                        USER_ID,
                        piggyBank,
                        PiggyBankActivityType.DELETED_PIGGY_BANK,
                        paidAmount
                )
        );

        verify(piggyBankActivityRepository, never()).save(any());
    }
}