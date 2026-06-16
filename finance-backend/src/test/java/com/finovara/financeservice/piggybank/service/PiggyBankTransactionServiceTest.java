package com.finovara.financeservice.piggybank.service;

import com.finovara.contracts.event.activity.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.event.notification.piggybank.PiggyBankProgressEvent;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.piggybank.repository.PiggyBankRepository;
import com.finovara.financeservice.settings.piggybank.completion.service.GoalCompletionService;
import com.finovara.financeservice.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PiggyBankTransactionServiceTest {

    @InjectMocks
    private PiggyBankTransactionService service;

    @Mock
    private WalletManagerService walletManagerService;
    @Mock
    private PiggyBankRepository piggyBankRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private PiggyBankManagerService piggyBankManagerService;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private GoalCompletionService goalCompletionService;
    private Wallet wallet;
    private PiggyBank piggyBank;

    private final Long userId = 1L;
    private final Long piggyBankId = 10L;

    @BeforeEach
    void setUp() {
        wallet = Wallet.create(userId);
        piggyBank = new PiggyBank();

        piggyBank.setAmount(BigDecimal.ZERO);
        piggyBank.setGoalAmount(new BigDecimal("1000"));
    }

    @Nested
    class AddBalanceToPiggyBankTests {

        @Test
        void shouldAddBalanceSuccessfully() {
            wallet.deposit(new BigDecimal("500"));
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);

            service.addBalanceToPiggyBank(userId, piggyBankId, new BigDecimal("100"), PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY);

            assertEquals(new BigDecimal("400"), wallet.getBalance());
            assertEquals(new BigDecimal("100"), piggyBank.getAmount());

            verify(walletRepository).save(wallet);
            verify(piggyBankRepository).save(piggyBank);

            ArgumentCaptor<PiggyBankActivityEvent> activityCaptor = ArgumentCaptor.forClass(PiggyBankActivityEvent.class);

            verify(kafkaTemplate).send(eq("activity.piggybank"), activityCaptor.capture());
            assertEquals(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY, activityCaptor.getValue().type());

            ArgumentCaptor<PiggyBankProgressEvent> progressCaptor = ArgumentCaptor.forClass(PiggyBankProgressEvent.class);

            verify(kafkaTemplate).send(eq("piggybank.calculate-progress"), progressCaptor.capture());
            assertEquals(10L, progressCaptor.getValue().piggyBankId());
        }

        @Test
        void shouldCallGoalCompletionWhenGoalReached() {
            wallet.deposit(new BigDecimal("500"));
            piggyBank.setAmount(new BigDecimal("950"));
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);

            service.addBalanceToPiggyBank(userId, piggyBankId, new BigDecimal("50"), PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY);

            assertEquals(new BigDecimal("1000"), piggyBank.getAmount());
            verify(goalCompletionService).handleGoalCompletion(userId);
        }

        @Test
        void shouldThrowWhenInsufficientFunds() {
            wallet.deposit(new BigDecimal("50"));
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);

            assertThrows(InvalidInputException.class, () -> service.addBalanceToPiggyBank(userId, piggyBankId, new BigDecimal("100"), PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY));

            verifyNoInteractions(piggyBankRepository);
        }

    }

    @Nested
    class RemoveBalanceFromPiggyBankTests {

        @Test
        void shouldRemoveBalanceSuccessfully() {
            wallet.deposit(new BigDecimal("300"));
            piggyBank.setAmount(new BigDecimal("200"));
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);

            service.removeBalanceFromPiggyBank(userId, piggyBankId, new BigDecimal("100"));

            assertEquals(new BigDecimal("400"), wallet.getBalance());
            assertEquals(new BigDecimal("100"), piggyBank.getAmount());

            verify(walletRepository).save(wallet);
            verify(piggyBankRepository).save(piggyBank);

            verify(kafkaTemplate, times(2)).send(anyString(), any());

            verifyNoInteractions(goalCompletionService);
        }

        @Test
        void shouldThrowWhenInsufficientPiggyBankFunds() {
            wallet.deposit(new BigDecimal("300"));
            piggyBank.setAmount(new BigDecimal("50"));
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);

            assertThrows(InvalidInputException.class, () -> service.removeBalanceFromPiggyBank(userId, piggyBankId, new BigDecimal("100")));

            verifyNoInteractions(walletRepository, piggyBankRepository);
        }

    }
}