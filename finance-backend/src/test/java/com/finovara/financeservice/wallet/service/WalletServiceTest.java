package com.finovara.financeservice.wallet.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.notification.event.wallet.WalletBalanceChangedEvent;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.dto.WalletDto;
import com.finovara.financeservice.wallet.dto.WalletResponse;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.repository.WalletRepository;
import com.finovara.financeservice.wallet.reservation.dto.FundReservationDto;
import com.finovara.financeservice.wallet.reservation.model.FundReservation;
import com.finovara.financeservice.wallet.reservation.repository.FundReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long WALLET_ID = 10L;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletManagerService walletManagerService;

    @Mock
    private FundReservationRepository fundReservationRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private Wallet wallet;

    @InjectMocks
    private WalletService walletService;

    @Nested
    class AddBalanceToWallet {

        @Test
        void shouldReturnUpdatedWalletDtoWhenWalletExists() {
            BigDecimal amount = BigDecimal.valueOf(100);
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(wallet.getBalance()).thenReturn(BigDecimal.valueOf(150));
            when(wallet.getReservedAmount()).thenReturn(BigDecimal.valueOf(20));

            WalletDto result = walletService.addBalanceToWallet(USER_ID, amount);

            assertNotNull(result);
            assertEquals(WALLET_ID, result.id());
            assertEquals(USER_ID, result.userId());
            assertEquals(BigDecimal.valueOf(150), result.balance());
            assertEquals(BigDecimal.valueOf(130), result.availableAmount());
        }

        @Test
        void shouldCallDepositWithExactAmountWhenAddingBalance() {
            BigDecimal amount = BigDecimal.valueOf(75);
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getBalance()).thenReturn(BigDecimal.ZERO);
            when(wallet.getReservedAmount()).thenReturn(BigDecimal.ZERO);

            walletService.addBalanceToWallet(USER_ID, amount);

            verify(wallet, times(1)).deposit(amount);
        }

        @Test
        void shouldNotSendKafkaEventWhenAddingBalance() {
            BigDecimal amount = BigDecimal.TEN;
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getBalance()).thenReturn(BigDecimal.TEN);
            when(wallet.getReservedAmount()).thenReturn(BigDecimal.ZERO);

            walletService.addBalanceToWallet(USER_ID, amount);

            verify(kafkaTemplate, never()).send(anyString(), any());
        }

        @Test
        void shouldThrowExceptionWhenWalletDoesNotExist() {
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("Wallet not found"));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> walletService.addBalanceToWallet(USER_ID, BigDecimal.TEN));
        }
    }

    @Nested
    class RemoveBalanceFromWallet {

        @Test
        void shouldReturnUpdatedWalletDtoWhenSufficientFunds() {
            BigDecimal amount = BigDecimal.valueOf(30);
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(wallet.getBalance()).thenReturn(BigDecimal.valueOf(100), BigDecimal.valueOf(70));
            when(wallet.getReservedAmount()).thenReturn(BigDecimal.ZERO);

            WalletDto result = walletService.removeBalanceFromWallet(USER_ID, amount);

            assertNotNull(result);
            assertEquals(BigDecimal.valueOf(70), result.balance());
        }

        @Test
        void shouldCallWithdrawWithExactAmountWhenRemovingBalance() {
            BigDecimal amount = BigDecimal.valueOf(30);
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getBalance()).thenReturn(BigDecimal.valueOf(100));
            when(wallet.getReservedAmount()).thenReturn(BigDecimal.ZERO);

            walletService.removeBalanceFromWallet(USER_ID, amount);

            verify(wallet, times(1)).withdraw(amount);
        }

        @Test
        void shouldPublishWalletBalanceChangedEventWhenWithdrawing() {
            BigDecimal amount = BigDecimal.valueOf(30);
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getBalance()).thenReturn(BigDecimal.valueOf(100), BigDecimal.valueOf(70));
            when(wallet.getReservedAmount()).thenReturn(BigDecimal.ZERO);

            walletService.removeBalanceFromWallet(USER_ID, amount);

            verify(kafkaTemplate, times(1))
                    .send(eq("wallet.balance-changed"), any(WalletBalanceChangedEvent.class));
        }

        @Test
        void shouldThrowExceptionWhenWalletDoesNotExist() {
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("Wallet not found"));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> walletService.removeBalanceFromWallet(USER_ID, BigDecimal.TEN));
        }

        @Test
        void shouldNotSendKafkaEventWhenWithdrawThrowsException() {
            BigDecimal amount = BigDecimal.valueOf(1000);
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getBalance()).thenReturn(BigDecimal.TEN);
            doThrow(new RuntimeException("Insufficient funds"))
                    .when(wallet).withdraw(amount);

            assertThrows(RuntimeException.class, () -> walletService.removeBalanceFromWallet(USER_ID, amount));
            verify(kafkaTemplate, never()).send(anyString(), any());
        }
    }

    @Nested
    class GetWalletWithReservations {

        @Test
        void shouldReturnExistingWalletWithReservationsWhenWalletExists() {
            FundReservation reservation = mock(FundReservation.class);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(wallet.getBalance()).thenReturn(BigDecimal.valueOf(200));
            when(wallet.getReservedAmount()).thenReturn(BigDecimal.valueOf(50));
            when(fundReservationRepository.findByWalletId(WALLET_ID)).thenReturn(List.of(reservation));
            when(reservation.getId()).thenReturn(5L);
            when(reservation.getCategory()).thenReturn(ExpenseCategory.SAVINGS);
            when(reservation.getAmount()).thenReturn(BigDecimal.valueOf(50));

            WalletResponse result = walletService.getWalletWithReservations(USER_ID);

            assertNotNull(result);
            assertEquals(WALLET_ID, result.id());
            assertEquals(BigDecimal.valueOf(150), result.availableAmount());
            assertEquals(1, result.reservations().size());
            FundReservationDto dto = result.reservations().getFirst();
            assertEquals(5L, dto.id());
            assertEquals(ExpenseCategory.SAVINGS, dto.category());
            assertEquals(BigDecimal.valueOf(50), dto.amount());
        }

        @Test
        void shouldCreateNewWalletWhenWalletDoesNotExist() {
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(wallet.getBalance()).thenReturn(BigDecimal.ZERO);
            when(wallet.getReservedAmount()).thenReturn(BigDecimal.ZERO);
            when(fundReservationRepository.findByWalletId(WALLET_ID)).thenReturn(List.of());

            WalletResponse result = walletService.getWalletWithReservations(USER_ID);

            assertNotNull(result);
            verify(walletRepository, times(1)).save(any(Wallet.class));
            assertTrue(result.reservations().isEmpty());
        }

        @Test
        void shouldReturnEmptyReservationsListWhenNoneExist() {
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(wallet.getBalance()).thenReturn(BigDecimal.TEN);
            when(wallet.getReservedAmount()).thenReturn(BigDecimal.ZERO);
            when(fundReservationRepository.findByWalletId(WALLET_ID)).thenReturn(List.of());

            WalletResponse result = walletService.getWalletWithReservations(USER_ID);

            assertTrue(result.reservations().isEmpty());
        }

        @Test
        void shouldCalculateAvailableBalanceAsBalanceMinusReserved() {
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(wallet.getBalance()).thenReturn(BigDecimal.valueOf(500));
            when(wallet.getReservedAmount()).thenReturn(BigDecimal.valueOf(120));
            when(fundReservationRepository.findByWalletId(WALLET_ID)).thenReturn(List.of());

            WalletResponse result = walletService.getWalletWithReservations(USER_ID);

            assertEquals(BigDecimal.valueOf(380), result.availableAmount());
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldCallRepositoryDeleteByUserIdWhenInvoked() {
            walletService.deleteByUserId(USER_ID);

            verify(walletRepository, times(1)).deleteByUserId(USER_ID);
        }

        @Test
        void shouldNotThrowExceptionWhenDeletingNonExistentWallet() {
            doNothing().when(walletRepository).deleteByUserId(USER_ID);

            walletService.deleteByUserId(USER_ID);

            verify(walletRepository, times(1)).deleteByUserId(USER_ID);
        }
    }
}