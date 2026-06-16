package com.finovara.financeservice.settings.piggybank.roundup.service;

import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.event.activity.settings.SettingsActivityEvent;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.piggybank.dto.PiggyBankDto;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.contracts.model.transaction.PiggyBankGoalType;
import com.finovara.financeservice.piggybank.repository.PiggyBankRepository;
import com.finovara.financeservice.piggybank.service.PiggyBankManagementService;
import com.finovara.financeservice.settings.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.financeservice.settings.piggybank.completion.service.GoalCompletionService;
import com.finovara.financeservice.settings.piggybank.model.PiggyBankSettings;
import com.finovara.financeservice.settings.piggybank.roundup.dto.RoundUpDto;
import com.finovara.financeservice.util.expense.ExpenseManagerService;
import com.finovara.financeservice.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.repository.WalletRepository;
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
    @BeforeEach
    void setup() {
        wallet = Wallet.create(userId);
        wallet.deposit(BigDecimal.valueOf(500));

        expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(123.45));

        piggyBank = new PiggyBank();
        piggyBank.setAmount(BigDecimal.valueOf(200));
        piggyBank.setSettings(new PiggyBankSettings());
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

            when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

            piggyBank.getSettings().setRoundUpActive(true);
        }

        @Test
        void shouldApplyRoundUpWhenPiggyBankActive() {
            when(piggyBankRepository.findAllByUserId(userId)).thenReturn(List.of(piggyBank));

            roundUpService.handleExpenseForRoundUp(userId, 1L, PiggyBankAutomationMode.APPLY);

            verify(roundUpCore).process(eq(userId), eq(piggyBank), eq(wallet), any(), eq(PiggyBankAutomationMode.APPLY));
            verify(goalCompletionService).handleGoalCompletion(userId);
        }

        @Test
        void shouldRollbackRoundUpWhenPiggyBankActive() {
            when(piggyBankRepository.findAllByUserId(userId)).thenReturn(List.of(piggyBank));

            roundUpService.handleExpenseForRoundUp(userId, 1L, PiggyBankAutomationMode.ROLLBACK);

            verify(roundUpCore).process(eq(userId), eq(piggyBank), eq(wallet), any(), eq(PiggyBankAutomationMode.ROLLBACK));
            verify(goalCompletionService).handleGoalCompletion(userId);
        }

        @Test
        void shouldNotApplyRoundUpWhenNoPiggyBanksAvailable() {
            when(piggyBankRepository.findAllByUserId(userId)).thenReturn(List.of());

            roundUpService.handleExpenseForRoundUp(userId, 1L, PiggyBankAutomationMode.APPLY);

            verifyNoInteractions(roundUpCore);
        }

        @Test
        void shouldThrowExceptionWhenWalletMissing() {
            when(piggyBankRepository.findAllByUserId(userId)).thenReturn(List.of());

            when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> roundUpService.handleExpenseForRoundUp(userId, 1L, PiggyBankAutomationMode.APPLY));

            verifyNoInteractions(roundUpCore);
        }

        @Test
        void shouldNotApplyRoundUpWhenPiggyBankInactive() {
            piggyBank.getSettings().setRoundUpActive(false);

            when(piggyBankRepository.findAllByUserId(userId)).thenReturn(List.of(piggyBank));

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

    }
}