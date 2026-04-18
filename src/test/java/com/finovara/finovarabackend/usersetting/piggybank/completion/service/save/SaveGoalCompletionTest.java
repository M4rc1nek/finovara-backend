package com.finovara.finovarabackend.usersetting.piggybank.completion.service.save;

import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.completion.dto.GoalCompletionDto;
import com.finovara.finovarabackend.usersetting.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.finovarabackend.usersetting.piggybank.completion.service.GoalCompletionService;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveGoalCompletionTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private PiggyBankManagerService piggyBankManagerService;

    @InjectMocks
    private GoalCompletionService goalCompletionService;

    private final Long USER_ID = 1L;
    private PiggyBank piggyBank;

    @Test
    void shouldSaveGoalCompletionStrategy() {
        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setId(1L);

        PiggyBankSettings settings = new PiggyBankSettings();
        piggyBank.setSettings(settings);

        GoalCompletionDto dto = new GoalCompletionDto(GoalCompletionStrategy.WITHDRAW_AND_DELETE);

        User user = new User();
        user.setId(USER_ID);

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
        when(piggyBankManagerService.getPiggyBankByUserId(piggyBank.getId(), USER_ID)).thenReturn(piggyBank);

        goalCompletionService.saveGoalCompletion(USER_ID, 1L, dto);

        assertEquals(GoalCompletionStrategy.WITHDRAW_AND_DELETE, piggyBank.getSettings().getGoalCompletionStrategy());
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        GoalCompletionDto dto = new GoalCompletionDto(GoalCompletionStrategy.WITHDRAW_AND_DELETE);

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> goalCompletionService.saveGoalCompletion(USER_ID, 1L, dto));
    }
}