package com.finovara.corebackend.usersetting.piggybank.roundup.service;

import com.finovara.activityservice.contracts.model.activity.SettingActivityStatus;
import com.finovara.activityservice.contracts.event.settings.SettingsActivityEvent;
import com.finovara.corebackend.exception.notfound.WalletNotFoundException;
import com.finovara.corebackend.expense.model.Expense;
import com.finovara.corebackend.piggybank.dto.PiggyBankDto;
import com.finovara.corebackend.piggybank.model.PiggyBank;
import com.finovara.activityservice.contracts.model.transaction.PiggyBankGoalType;
import com.finovara.corebackend.piggybank.repository.PiggyBankRepository;
import com.finovara.corebackend.piggybank.service.PiggyBankManagementService;
import com.finovara.corebackend.user.exception.notfound.UserNotFoundException;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.corebackend.usersetting.piggybank.completion.service.GoalCompletionService;
import com.finovara.corebackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.corebackend.usersetting.piggybank.roundup.dto.RoundUpDto;
import com.finovara.corebackend.util.expense.ExpenseManagerService;
import com.finovara.corebackend.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.corebackend.util.user.service.UserManagerService;
import com.finovara.corebackend.wallet.model.Wallet;
import com.finovara.corebackend.wallet.repository.WalletRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
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
    private KafkaTemplate<String, Object> kafkaTemplate;
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

        wallet = Wallet.create(user);
        wallet.deposit(BigDecimal.valueOf(500));

        expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(123.45));

        piggyBank = new PiggyBank();
        piggyBank.setAmount(BigDecimal.valueOf(200));
        piggyBank.setSettings(new PiggyBankSettings());

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

            ArgumentCaptor<SettingsActivityEvent> eventCaptor = ArgumentCaptor.forClass(SettingsActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.settings"), eventCaptor.capture());
            assertEquals(active ? SettingActivityStatus.ENABLED : SettingActivityStatus.DISABLED, eventCaptor.getValue().status());
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