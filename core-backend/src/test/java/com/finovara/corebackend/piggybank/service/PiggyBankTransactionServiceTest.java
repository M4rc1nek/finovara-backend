package com.finovara.corebackend.piggybank.service;

import com.finovara.contracts.event.activity.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.corebackend.piggybank.model.PiggyBank;
import com.finovara.corebackend.piggybank.repository.PiggyBankRepository;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.usersetting.piggybank.completion.service.GoalCompletionService;
import com.finovara.corebackend.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.corebackend.util.user.service.UserManagerService;
import com.finovara.corebackend.util.wallet.WalletManagerService;
import com.finovara.corebackend.wallet.model.Wallet;
import com.finovara.corebackend.wallet.repository.WalletRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PiggyBankTransactionServiceTest {

    @InjectMocks
    private PiggyBankTransactionService piggyBankTransactionService;

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private PiggyBankRepository piggyBankRepository;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private GoalCompletionService goalCompletionService;
    @Mock
    private UserManagerService userManagerService;
    @Mock
    private PiggyBankManagerService piggyBankManagerService;
    @Mock
    private WalletManagerService walletManagerService;

    private User user;
    private Long userId;
    private PiggyBank piggyBank;
    private Long piggyBankId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        user = new User();
        userId = 1L;
        piggyBankId = 1L;
        wallet = Wallet.create(user);
        piggyBank = new PiggyBank();
    }

    @Nested
    class AddBalanceTests {
        @Test
        void shouldAddBalanceSuccessfully() {
            wallet.deposit(new BigDecimal("500"));
            piggyBank.setAmount(new BigDecimal("200"));
            piggyBank.setGoalAmount(new BigDecimal("1000"));

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);

            piggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, new BigDecimal("100"),
                    PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY);

            assertEquals(new BigDecimal("400"), wallet.getBalance());
            assertEquals(new BigDecimal("300"), piggyBank.getAmount());

            verify(walletRepository).save(wallet);
            verify(piggyBankRepository).save(piggyBank);
            ArgumentCaptor<PiggyBankActivityEvent> eventCaptor = ArgumentCaptor.forClass(PiggyBankActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.piggybank"), eventCaptor.capture());
            assertEquals(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY, eventCaptor.getValue().type());
        }

        @Test
        void shouldCallGoalCompletionWhenGoalReached() {
            wallet.deposit(new BigDecimal("500"));
            piggyBank.setAmount(new BigDecimal("900"));
            piggyBank.setGoalAmount(new BigDecimal("1000"));

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);

            piggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, new BigDecimal("100"),
                    PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY);

            assertEquals(new BigDecimal("1000"), piggyBank.getAmount());
            verify(goalCompletionService).handleGoalCompletion(userId);
        }

        @Test
        void shouldThrowWhenInsufficientFunds() {
            wallet.deposit(new BigDecimal("50"));
            piggyBank.setAmount(new BigDecimal("200"));

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);

            assertThrows(InvalidInputException.class, () ->
                    piggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, new BigDecimal("100"),
                            PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY));

            verify(piggyBankRepository, never()).save(any());
            verify(walletRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userManagerService.getUserByIdOrThrow(userId)).thenThrow(new RequestedEntityNotFoundException("x"));

            assertThrows(RequestedEntityNotFoundException.class, () -> piggyBankTransactionService.addBalanceToPiggyBank(userId, piggyBankId, new BigDecimal("100"),
                            PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY));

            verifyNoInteractions(walletRepository, piggyBankRepository);
        }
    }

    @Nested
    class RemoveBalanceTests {
        @Test
        void shouldRemoveBalanceSuccessfully() {
            wallet.deposit(new BigDecimal("300"));
            piggyBank.setAmount(new BigDecimal("200"));

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);

            piggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, new BigDecimal("100"));

            assertEquals(new BigDecimal("400"), wallet.getBalance());
            assertEquals(new BigDecimal("100"), piggyBank.getAmount());

            verify(walletRepository).save(wallet);
            verify(piggyBankRepository).save(piggyBank);
            ArgumentCaptor<PiggyBankActivityEvent> eventCaptor = ArgumentCaptor.forClass(PiggyBankActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.piggybank"), eventCaptor.capture());
            assertEquals(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK, eventCaptor.getValue().type());
        }

        @Test
        void shouldThrowWhenInsufficientPiggyBankFunds() {
            wallet.deposit(new BigDecimal("300"));
            piggyBank.setAmount(new BigDecimal("50"));

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);

            assertThrows(InvalidInputException.class, () ->
                    piggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, new BigDecimal("100")));

            verify(piggyBankRepository, never()).save(any());
            verify(walletRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenUserNotFoundRemove() {
            when(userManagerService.getUserByIdOrThrow(userId)).thenThrow(new RequestedEntityNotFoundException("x"));

            assertThrows(RequestedEntityNotFoundException.class, ()
                    -> piggyBankTransactionService.removeBalanceFromPiggyBank(userId, piggyBankId, new BigDecimal("100")));

            verifyNoInteractions(walletRepository, piggyBankRepository);
        }
    }
}