package com.finovara.finovarabackend.usersetting.piggybank.completion.service.get;

import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.completion.dto.GoalCompletionDto;
import com.finovara.finovarabackend.usersetting.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.finovarabackend.usersetting.piggybank.completion.service.GoalCompletionService;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.util.service.piggybank.manager.PiggyBankManagerService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetGoalCompletionTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private PiggyBankManagerService piggyBankManagerService;

    @InjectMocks
    private GoalCompletionService goalCompletionService;

    private final String EMAIL = "test@test.com";

    @Test
    void shouldReturnGoalCompletionStrategy() {
        User user = new User();
        user.setEmail(EMAIL);

        PiggyBank piggyBank = new PiggyBank();
        PiggyBankSettings settings = new PiggyBankSettings();
        settings.setGoalCompletionStrategy(GoalCompletionStrategy.WITHDRAW_AND_KEEP);
        piggyBank.setSettings(settings);

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
        when(piggyBankManagerService.getPiggyBankByUserEmail(1L, EMAIL)).thenReturn(piggyBank);

        GoalCompletionDto result = goalCompletionService.getCompletionDto(EMAIL, 1L);

        assertEquals(GoalCompletionStrategy.WITHDRAW_AND_KEEP, result.strategy());
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        GoalCompletionDto dto = new GoalCompletionDto(GoalCompletionStrategy.WITHDRAW_AND_DELETE);

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> goalCompletionService.saveGoalCompletion(EMAIL, 1L, dto));
    }
}