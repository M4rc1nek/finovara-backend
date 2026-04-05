package com.finovara.finovarabackend.usersetting.piggybank.completion.service.add;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.completion.dto.GoalCompletionDto;
import com.finovara.finovarabackend.usersetting.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.finovarabackend.usersetting.piggybank.completion.service.GoalCompletionService;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddGoalCompletionTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private PiggyBankManagerService piggyBankManagerService;

    @Mock
    private PiggyBankRepository piggyBankRepository;

    @InjectMocks
    private GoalCompletionService goalCompletionService;

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
    void shouldThrowExceptionWhenGoalNotSet() {
        piggyBank.setGoalAmount(null);

        GoalCompletionDto dto = new GoalCompletionDto(GoalCompletionStrategy.WITHDRAW_AND_KEEP);

        assertThrows(InvalidInputException.class, () -> goalCompletionService.addGoalCompletion(1L, EMAIL, dto));
    }

    @Test
    void shouldSaveGoalCompletionStrategy() {
        piggyBank.setGoalAmount(BigDecimal.valueOf(1000));

        GoalCompletionDto dto = new GoalCompletionDto(GoalCompletionStrategy.WITHDRAW_AND_KEEP);

        goalCompletionService.addGoalCompletion(1L, EMAIL, dto);

        assertEquals(GoalCompletionStrategy.WITHDRAW_AND_KEEP, piggyBank.getSettings().getGoalCompletionStrategy());

        verify(piggyBankRepository).save(piggyBank);
    }
}