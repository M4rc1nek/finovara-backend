package com.finovara.finovarabackend.usersetting.piggybank.roundup.service;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.exception.notfound.WalletNotFoundException;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.piggybank.dto.PiggyBankDto;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.model.PiggyBankGoalType;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.piggybank.service.PiggyBankManagementService;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.finovarabackend.usersetting.piggybank.completion.service.GoalCompletionService;
import com.finovara.finovarabackend.usersetting.piggybank.roundup.dto.RoundUpDto;
import com.finovara.finovarabackend.util.expense.ExpenseManagerService;
import com.finovara.finovarabackend.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import com.finovara.finovarabackend.wallet.model.Wallet;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoundUpServiceTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private PiggyBankManagerService piggyBankManagerService;
    @Mock
    private SettingsActivityService settingsActivityService;
    @Mock
    private ExpenseManagerService expenseManagerService;
    @Mock
    private PiggyBankRepository piggyBankRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private GoalCompletionService goalCompletionService;
    @Mock
    private RoundUpCore roundUpCore;
    @Mock
    private PiggyBankManagementService piggyBankManagementService;

    @InjectMocks
    private RoundUpService roundUpService;

    private final Long userId = 1L;
    private final Long piggyBankId = 1L;

    private PiggyBank piggyBank;
    private Wallet wallet;
    private Expense expense;
    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(userId);

        wallet = new Wallet();
        wallet.setBalance(BigDecimal.valueOf(500));

        expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(123.45));

        piggyBank = new PiggyBank();
        piggyBank.setAmount(BigDecimal.valueOf(200));
        piggyBank.setSettings(new com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings());

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
    }

    @Nested
    class SaveRoundUp {
        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void shouldSaveRoundUp(boolean active) {
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            RoundUpDto dto = new RoundUpDto(active);

            roundUpService.saveRoundUpPiggyBank(userId, piggyBankId, dto);

            assertEquals(active, piggyBank.getSettings().isRoundUpActive());

            verify(settingsActivityService).createSettingActivity(userId, active ? SettingActivityStatus.ENABLED : SettingActivityStatus.DISABLED, SettingType.PIGGY_BANK_ROUND_UP);
        }
    }

    @Nested
    class GetRoundUp {
        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void shouldReturnRoundUp(boolean active) {
            piggyBank.getSettings().setRoundUpActive(active);

            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            RoundUpDto result = roundUpService.getRoundUp(userId, piggyBankId);

            assertEquals(active, result.roundUpActive());
        }
    }

    @Nested
    class HandleExpense {
        @BeforeEach
        void setupHandle() {
            when(expenseManagerService.getExpenseByUserIdOrThrow(anyLong(), eq(userId))).thenReturn(expense);

            when(walletRepository.findByUserAssignedId(userId)).thenReturn(Optional.of(wallet));

            piggyBank.getSettings().setRoundUpActive(true);
        }

        @Test
        void shouldApplyRoundUpWhenPiggyBankActive() {
            when(piggyBankRepository.findAllByUserAssignedId(userId)).thenReturn(List.of(piggyBank));

            roundUpService.handleExpenseForRoundUp(userId, 1L, PiggyBankAutomationMode.APPLY);

            verify(roundUpCore).process(eq(userId), eq(piggyBank), eq(wallet), any(), eq(PiggyBankAutomationMode.APPLY));
            verify(goalCompletionService).handleGoalCompletion(userId);
        }

        @Test
        void shouldRollbackRoundUpWhenPiggyBankActive() {
            when(piggyBankRepository.findAllByUserAssignedId(userId)).thenReturn(List.of(piggyBank));

            roundUpService.handleExpenseForRoundUp(userId, 1L, PiggyBankAutomationMode.ROLLBACK);

            verify(roundUpCore).process(eq(userId), eq(piggyBank), eq(wallet), any(), eq(PiggyBankAutomationMode.ROLLBACK));
            verify(goalCompletionService).handleGoalCompletion(userId);
        }

        @Test
        void shouldNotApplyRoundUpWhenNoPiggyBanksAvailable() {
            when(piggyBankRepository.findAllByUserAssignedId(userId)).thenReturn(List.of());

            roundUpService.handleExpenseForRoundUp(userId, 1L, PiggyBankAutomationMode.APPLY);

            verifyNoInteractions(roundUpCore);
        }

        @Test
        void shouldThrowExceptionWhenWalletMissing() {
            when(piggyBankRepository.findAllByUserAssignedId(userId)).thenReturn(List.of());

            when(walletRepository.findByUserAssignedId(userId)).thenReturn(Optional.empty());

            assertThrows(WalletNotFoundException.class, () -> roundUpService.handleExpenseForRoundUp(userId, 1L, PiggyBankAutomationMode.APPLY));

            verifyNoInteractions(roundUpCore);
        }

        @Test
        void shouldNotApplyRoundUpWhenPiggyBankInactive() {
            piggyBank.getSettings().setRoundUpActive(false);

            when(piggyBankRepository.findAllByUserAssignedId(userId)).thenReturn(List.of(piggyBank));

            roundUpService.handleExpenseForRoundUp(userId, 1L, PiggyBankAutomationMode.APPLY);

            verifyNoInteractions(roundUpCore);
            verify(goalCompletionService).handleGoalCompletion(userId);
        }
    }

    @Nested
    class AddPiggyBank {

        @Test
        void shouldAddPiggyBankSuccessfully() {
            PiggyBankDto dto = new PiggyBankDto(123L, 1L, "My piggy bank", new BigDecimal("100"), null, PiggyBankGoalType.GIFTS, new BigDecimal("250"), null, false);

            when(piggyBankManagementService.addPiggyBank(dto, userId)).thenReturn(123L);

            Long result = roundUpService.addDefaultPiggyBank(dto, userId);

            assertEquals(123L, result);
            verify(piggyBankManagementService).addPiggyBank(dto, userId);
        }

        @Test
        void shouldThrowExceptionWhenUserDoesNotExist() {
            PiggyBankDto dto = new PiggyBankDto(12L, null, "My piggy bank", new BigDecimal("50"), null, PiggyBankGoalType.GIFTS, new BigDecimal("100"), null, false);

            when(userManagerService.getUserByIdOrThrow(userId)).thenThrow(new UserNotFoundException("User not found"));

            assertThrows(UserNotFoundException.class, () -> roundUpService.addDefaultPiggyBank(dto, userId));
        }
    }
}