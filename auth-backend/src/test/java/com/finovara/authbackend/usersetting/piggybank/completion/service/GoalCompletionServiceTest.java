package com.finovara.authbackend.usersetting.piggybank.completion.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.authbackend.piggybank.model.PiggyBank;
import com.finovara.authbackend.piggybank.repository.PiggyBankRepository;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.authbackend.user.model.User;
import com.finovara.authbackend.usersetting.piggybank.completion.dto.GoalCompletionDto;
import com.finovara.authbackend.usersetting.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.authbackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.authbackend.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.authbackend.util.user.service.UserManagerService;
import com.finovara.authbackend.util.wallet.WalletManagerService;
import com.finovara.authbackend.wallet.model.Wallet;
import com.finovara.authbackend.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalCompletionServiceTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private PiggyBankManagerService piggyBankManagerService;
    @Mock
    private PiggyBankRepository piggyBankRepository;
    @Mock
    private WalletManagerService walletManagerService;
    @Mock
    private GoalCompletionCore goalCompletionCore;
    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private GoalCompletionService goalCompletionService;

    private static final Long USER_ID = 1L;

    @Nested
    class AddGoalCompletion {
        @Test
        void shouldThrowExceptionWhenGoalNotSet() {
            User user = createUser();
            PiggyBank piggyBank = createPiggyBank(null, null, null);

            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
            when(piggyBankManagerService.getPiggyBankByUserId(1L, USER_ID)).thenReturn(piggyBank);

            GoalCompletionDto dto = new GoalCompletionDto(GoalCompletionStrategy.WITHDRAW_AND_KEEP);

            assertThrows(InvalidInputException.class, () -> goalCompletionService.addGoalCompletion(1L, USER_ID, dto));
        }

        @Test
        void shouldSaveGoalCompletionStrategy() {
            User user = createUser();
            PiggyBank piggyBank = createPiggyBank(BigDecimal.valueOf(100), BigDecimal.valueOf(1000), null);

            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
            when(piggyBankManagerService.getPiggyBankByUserId(1L, USER_ID)).thenReturn(piggyBank);

            GoalCompletionDto dto = new GoalCompletionDto(GoalCompletionStrategy.WITHDRAW_AND_KEEP);

            goalCompletionService.addGoalCompletion(1L, USER_ID, dto);

            assertEquals(GoalCompletionStrategy.WITHDRAW_AND_KEEP, piggyBank.getSettings().getGoalCompletionStrategy());

            verify(piggyBankRepository).save(piggyBank);
        }

    }

    @Nested
    class SaveGoalCompletion {

        @Test
        void shouldSaveStrategy() {
            User user = createUser();
            PiggyBank piggyBank = createPiggyBank(null, null, null);

            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
            when(piggyBankManagerService.getPiggyBankByUserId(1L, USER_ID)).thenReturn(piggyBank);

            GoalCompletionDto dto = new GoalCompletionDto(GoalCompletionStrategy.WITHDRAW_AND_DELETE);

            goalCompletionService.saveGoalCompletion(USER_ID, 1L, dto);

            assertEquals(GoalCompletionStrategy.WITHDRAW_AND_DELETE, piggyBank.getSettings().getGoalCompletionStrategy());
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenThrow(new RequestedEntityNotFoundException("User not found"));

            GoalCompletionDto dto = new GoalCompletionDto(GoalCompletionStrategy.WITHDRAW_AND_DELETE);

            assertThrows(RequestedEntityNotFoundException.class, () -> goalCompletionService.saveGoalCompletion(USER_ID, 1L, dto));
        }

    }

    @Nested
    class HandleGoalCompletion {

        @Test
        void shouldNotCallCoreWhenGoalNotReached() {
            User user = createUser();
            Wallet wallet = createWallet();
            PiggyBank piggyBank = createPiggyBank(BigDecimal.valueOf(100), BigDecimal.valueOf(200), GoalCompletionStrategy.WITHDRAW_AND_KEEP);

            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of(piggyBank));

            goalCompletionService.handleGoalCompletion(USER_ID);

            verify(goalCompletionCore, never()).apply(anyLong(), any(), any(), any(), any());

            verify(walletRepository).save(wallet);
        }

        @Test
        void shouldCallCoreWhenGoalReached() {
            User user = createUser();
            Wallet wallet = createWallet();
            PiggyBank piggyBank = createPiggyBank(BigDecimal.valueOf(200), BigDecimal.valueOf(200), GoalCompletionStrategy.WITHDRAW_AND_KEEP);

            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of(piggyBank));

            goalCompletionService.handleGoalCompletion(USER_ID);

            verify(goalCompletionCore).apply(eq(USER_ID), eq(piggyBank), eq(wallet), eq(user), eq(GoalCompletionStrategy.WITHDRAW_AND_KEEP));

            verify(walletRepository).save(wallet);
        }

        @Test
        void shouldUseNoneWhenStrategyNull() {
            User user = createUser();
            Wallet wallet = createWallet();
            PiggyBank piggyBank = createPiggyBank(BigDecimal.valueOf(200), BigDecimal.valueOf(200), null);

            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of(piggyBank));

            goalCompletionService.handleGoalCompletion(USER_ID);

            verify(goalCompletionCore).apply(eq(USER_ID), eq(piggyBank), eq(wallet), eq(user), eq(GoalCompletionStrategy.NONE));
        }

        @Test
        void shouldHandleMultiplePiggyBanks() {
            User user = createUser();
            Wallet wallet = createWallet();

            PiggyBank first = createPiggyBank(BigDecimal.valueOf(200), BigDecimal.valueOf(200), GoalCompletionStrategy.WITHDRAW_AND_KEEP);

            PiggyBank second = createPiggyBank(BigDecimal.valueOf(200), BigDecimal.valueOf(200), GoalCompletionStrategy.NONE);

            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of(first, second));

            goalCompletionService.handleGoalCompletion(USER_ID);

            verify(goalCompletionCore, times(2)).apply(anyLong(), any(), any(), any(), any());

            verify(walletRepository).save(wallet);
        }

    }

    @Nested
    class GetGoalCompletion {

        @Test
        void shouldReturnStrategy() {
            User user = createUser();

            PiggyBank piggyBank = createPiggyBank(BigDecimal.valueOf(200), BigDecimal.valueOf(200), GoalCompletionStrategy.WITHDRAW_AND_KEEP);

            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
            when(piggyBankManagerService.getPiggyBankByUserId(1L, USER_ID)).thenReturn(piggyBank);

            GoalCompletionDto result = goalCompletionService.getCompletionDto(USER_ID, 1L);

            assertEquals(GoalCompletionStrategy.WITHDRAW_AND_KEEP, result.strategy());
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenThrow(new RequestedEntityNotFoundException("User not found"));

            GoalCompletionDto dto = new GoalCompletionDto(GoalCompletionStrategy.WITHDRAW_AND_DELETE);

            assertThrows(RequestedEntityNotFoundException.class, () -> goalCompletionService.saveGoalCompletion(USER_ID, 1L, dto));
        }

    }

    private User createUser() {
        User user = new User();
        user.setId(USER_ID);
        return user;
    }

    private Wallet createWallet() {
        User user = new User();
        user.setId(USER_ID);

        Wallet wallet = Wallet.create(userId);
        wallet.deposit(BigDecimal.valueOf(500));
        return wallet;
    }

    private PiggyBank createPiggyBank(BigDecimal amount, BigDecimal goal, GoalCompletionStrategy strategy) {
        PiggyBank piggyBank = new PiggyBank();
        PiggyBankSettings settings = new PiggyBankSettings();

        piggyBank.setAmount(amount);
        piggyBank.setGoalAmount(goal);
        settings.setGoalCompletionStrategy(strategy);

        piggyBank.setSettings(settings);
        return piggyBank;
    }
}