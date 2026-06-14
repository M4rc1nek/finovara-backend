package com.finovara.authbackend.usersetting.piggybank.completion.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.authbackend.piggybank.model.PiggyBank;
import com.finovara.authbackend.piggybank.repository.PiggyBankRepository;
import com.finovara.authbackend.usersetting.piggybank.completion.dto.GoalCompletionDto;
import com.finovara.authbackend.usersetting.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.authbackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.authbackend.util.piggybank.manager.PiggyBankManagerService;
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
            PiggyBank piggyBank = createPiggyBank(null, null, null);

            when(piggyBankManagerService.getPiggyBankByUserId(1L, USER_ID)).thenReturn(piggyBank);

            GoalCompletionDto dto = new GoalCompletionDto(GoalCompletionStrategy.WITHDRAW_AND_KEEP);

            assertThrows(InvalidInputException.class, () -> goalCompletionService.addGoalCompletion(1L, USER_ID, dto));
        }

        @Test
        void shouldSaveGoalCompletionStrategy() {
            PiggyBank piggyBank = createPiggyBank(BigDecimal.valueOf(100), BigDecimal.valueOf(1000), null);

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
            PiggyBank piggyBank = createPiggyBank(null, null, null);

            when(piggyBankManagerService.getPiggyBankByUserId(1L, USER_ID)).thenReturn(piggyBank);

            GoalCompletionDto dto = new GoalCompletionDto(GoalCompletionStrategy.WITHDRAW_AND_DELETE);

            goalCompletionService.saveGoalCompletion(USER_ID, 1L, dto);

            assertEquals(GoalCompletionStrategy.WITHDRAW_AND_DELETE, piggyBank.getSettings().getGoalCompletionStrategy());
        }

    }

    @Nested
    class HandleGoalCompletion {

        @Test
        void shouldNotCallCoreWhenGoalNotReached() {
            Wallet wallet = createWallet();
            PiggyBank piggyBank = createPiggyBank(BigDecimal.valueOf(100), BigDecimal.valueOf(200), GoalCompletionStrategy.WITHDRAW_AND_KEEP);

            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of(piggyBank));

            goalCompletionService.handleGoalCompletion(USER_ID);

            verify(goalCompletionCore, never()).apply(anyLong(), any(), any(), any());

            verify(walletRepository).save(wallet);
        }

        @Test
        void shouldCallCoreWhenGoalReached() {
            Wallet wallet = createWallet();
            PiggyBank piggyBank = createPiggyBank(BigDecimal.valueOf(200), BigDecimal.valueOf(200), GoalCompletionStrategy.WITHDRAW_AND_KEEP);

            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of(piggyBank));

            goalCompletionService.handleGoalCompletion(USER_ID);

            verify(goalCompletionCore).apply(eq(USER_ID), eq(piggyBank), eq(wallet), eq(GoalCompletionStrategy.WITHDRAW_AND_KEEP));

            verify(walletRepository).save(wallet);
        }

        @Test
        void shouldUseNoneWhenStrategyNull() {
            Wallet wallet = createWallet();
            PiggyBank piggyBank = createPiggyBank(BigDecimal.valueOf(200), BigDecimal.valueOf(200), null);

            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of(piggyBank));

            goalCompletionService.handleGoalCompletion(USER_ID);

            verify(goalCompletionCore).apply(eq(USER_ID), eq(piggyBank), eq(wallet), eq(GoalCompletionStrategy.NONE));
        }

        @Test
        void shouldHandleMultiplePiggyBanks() {
            Wallet wallet = createWallet();

            PiggyBank first = createPiggyBank(BigDecimal.valueOf(200), BigDecimal.valueOf(200), GoalCompletionStrategy.WITHDRAW_AND_KEEP);

            PiggyBank second = createPiggyBank(BigDecimal.valueOf(200), BigDecimal.valueOf(200), GoalCompletionStrategy.NONE);

            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of(first, second));

            goalCompletionService.handleGoalCompletion(USER_ID);

            verify(goalCompletionCore, times(2)).apply(anyLong(), any(), any(), any());

            verify(walletRepository).save(wallet);
        }

    }

    @Nested
    class GetGoalCompletion {

        @Test
        void shouldReturnStrategy() {
            PiggyBank piggyBank = createPiggyBank(BigDecimal.valueOf(200), BigDecimal.valueOf(200), GoalCompletionStrategy.WITHDRAW_AND_KEEP);

            when(piggyBankManagerService.getPiggyBankByUserId(1L, USER_ID)).thenReturn(piggyBank);

            GoalCompletionDto result = goalCompletionService.getCompletionDto(USER_ID, 1L);

            assertEquals(GoalCompletionStrategy.WITHDRAW_AND_KEEP, result.strategy());
        }

    }

    private Wallet createWallet() {
        Wallet wallet = Wallet.create(USER_ID);
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
