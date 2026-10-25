package com.finovara.financeservice.wallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.mainaccount.transaction.event.wallet.WalletBalanceChangedEvent;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.dto.WalletDto;
import com.finovara.financeservice.wallet.dto.WalletResponse;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.repository.WalletRepository;
import com.finovara.financeservice.wallet.reservation.dto.FundReservationDto;
import com.finovara.financeservice.wallet.reservation.model.FundReservation;
import com.finovara.financeservice.wallet.reservation.repository.FundReservationRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long WALLET_ID = 5L;
    private static final BigDecimal BALANCE = new BigDecimal("500.00");
    private static final BigDecimal RESERVED = new BigDecimal("100.00");
    private static final BigDecimal AVAILABLE = new BigDecimal("400.00");
    private static final BigDecimal AMOUNT = new BigDecimal("50.00");
    private static final BigDecimal BALANCE_AFTER_WITHDRAWAL = new BigDecimal("450.00");

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
    @Mock
    private FundReservation firstReservation;
    @Mock
    private FundReservation secondReservation;

    @InjectMocks
    private WalletService service;

    @Nested
    class AddBalanceToWallet {

        @Test
        void shouldReturnWalletDtoWhenWalletExists() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(wallet.getBalance()).thenReturn(BALANCE);
            when(wallet.getReservedAmount()).thenReturn(RESERVED);

            WalletDto result = service.addBalanceToWallet(USER_ID, AMOUNT);

            assertEquals(new WalletDto(WALLET_ID, USER_ID, BALANCE, RESERVED, AVAILABLE), result);
        }

        @Test
        void shouldDepositAmountWhenWalletExists() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getBalance()).thenReturn(BALANCE);
            when(wallet.getReservedAmount()).thenReturn(RESERVED);

            service.addBalanceToWallet(USER_ID, AMOUNT);

            verify(wallet).deposit(AMOUNT);
        }

        @Test
        void shouldNotSendKafkaEventWhenAddingBalance() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getBalance()).thenReturn(BALANCE);
            when(wallet.getReservedAmount()).thenReturn(RESERVED);

            service.addBalanceToWallet(USER_ID, AMOUNT);

            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        void shouldThrowExceptionWhenWalletDoesNotExist() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenThrow(new RequestedEntityNotFoundException("Wallet not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.addBalanceToWallet(USER_ID, AMOUNT));

            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        void shouldThrowExceptionWhenDepositFails() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            doThrow(new InvalidInputException("Invalid amount")).when(wallet).deposit(AMOUNT);

            assertThrows(InvalidInputException.class, () -> service.addBalanceToWallet(USER_ID, AMOUNT));

            verifyNoInteractions(kafkaTemplate);
        }
    }

    @Nested
    class RemoveBalanceFromWallet {

        @Test
        void shouldReturnWalletDtoWithUpdatedBalanceWhenWalletExists() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(wallet.getBalance()).thenReturn(BALANCE, BALANCE_AFTER_WITHDRAWAL);
            when(wallet.getReservedAmount()).thenReturn(RESERVED);

            WalletDto result = service.removeBalanceFromWallet(USER_ID, AMOUNT);

            assertEquals(new WalletDto(WALLET_ID, USER_ID, BALANCE_AFTER_WITHDRAWAL, RESERVED, BALANCE_AFTER_WITHDRAWAL.subtract(RESERVED)), result);
        }

        @Test
        void shouldWithdrawAmountWhenWalletExists() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getBalance()).thenReturn(BALANCE, BALANCE_AFTER_WITHDRAWAL);
            when(wallet.getReservedAmount()).thenReturn(RESERVED);

            service.removeBalanceFromWallet(USER_ID, AMOUNT);

            verify(wallet).withdraw(AMOUNT);
        }

        @Test
        void shouldSendBalanceChangedEventWhenWalletExists() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getBalance()).thenReturn(BALANCE, BALANCE_AFTER_WITHDRAWAL);
            when(wallet.getReservedAmount()).thenReturn(RESERVED);

            service.removeBalanceFromWallet(USER_ID, AMOUNT);

            verify(kafkaTemplate).send(eq("wallet.balance-changed"), any(WalletBalanceChangedEvent.class));
        }

        @Test
        void shouldThrowExceptionWhenWalletDoesNotExist() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenThrow(new RequestedEntityNotFoundException("Wallet not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.removeBalanceFromWallet(USER_ID, AMOUNT));

            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        void shouldThrowExceptionWhenWithdrawalFails() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            doThrow(new InvalidInputException("Insufficient funds")).when(wallet).withdraw(AMOUNT);

            assertThrows(InvalidInputException.class, () -> service.removeBalanceFromWallet(USER_ID, AMOUNT));

            verifyNoInteractions(kafkaTemplate);
        }
    }

    @Nested
    class LockWalletForUpdate {

        @Test
        void shouldLockWalletWhenWalletExists() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);

            service.lockWalletForUpdate(USER_ID);

            verify(walletManagerService).getWalletForUpdateOrThrow(USER_ID);
        }

        @Test
        void shouldThrowExceptionWhenWalletDoesNotExist() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenThrow(new RequestedEntityNotFoundException("Wallet not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.lockWalletForUpdate(USER_ID));
        }
    }

    @Nested
    class GetWalletWithReservations {

        @BeforeEach
        void setUp() {
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(wallet.getBalance()).thenReturn(BALANCE);
            when(wallet.getReservedAmount()).thenReturn(RESERVED);
        }

        @Test
        void shouldReturnWalletResponseWithReservationsWhenWalletExists() {
            BigDecimal firstAmount = new BigDecimal("60.00");
            BigDecimal secondAmount = new BigDecimal("40.00");
            when(firstReservation.getId()).thenReturn(1L);
            when(firstReservation.getAmount()).thenReturn(firstAmount);
            when(secondReservation.getId()).thenReturn(2L);
            when(secondReservation.getAmount()).thenReturn(secondAmount);
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
            when(fundReservationRepository.findAllByWalletId(WALLET_ID)).thenReturn(List.of(firstReservation, secondReservation));

            WalletResponse result = service.getWalletWithReservations(USER_ID);

            WalletResponse expected = new WalletResponse(WALLET_ID, USER_ID, BALANCE, RESERVED, AVAILABLE,
                    List.of(new FundReservationDto(1L, null, firstAmount, null), new FundReservationDto(2L, null, secondAmount, null)));
            assertEquals(expected, result);
        }

        @Test
        void shouldReturnEmptyReservationsWhenWalletHasNoReservations() {
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
            when(fundReservationRepository.findAllByWalletId(WALLET_ID)).thenReturn(List.of());

            WalletResponse result = service.getWalletWithReservations(USER_ID);

            assertEquals(new WalletResponse(WALLET_ID, USER_ID, BALANCE, RESERVED, AVAILABLE, List.of()), result);
        }

        @Test
        void shouldNotSaveWalletWhenWalletAlreadyExists() {
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
            when(fundReservationRepository.findAllByWalletId(WALLET_ID)).thenReturn(List.of());

            service.getWalletWithReservations(USER_ID);

            verify(walletRepository, never()).save(any(Wallet.class));
        }

        @Test
        void shouldCreateAndSaveWalletWhenWalletDoesNotExist() {
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
            when(fundReservationRepository.findAllByWalletId(WALLET_ID)).thenReturn(List.of());

            WalletResponse result = service.getWalletWithReservations(USER_ID);

            verify(walletRepository).save(any(Wallet.class));
            assertEquals(new WalletResponse(WALLET_ID, USER_ID, BALANCE, RESERVED, AVAILABLE, List.of()), result);
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteWalletWhenWalletExists() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);

            service.deleteByUserId(USER_ID);

            verify(walletRepository).delete(wallet);
        }

        @Test
        void shouldThrowExceptionWhenWalletDoesNotExist() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenThrow(new RequestedEntityNotFoundException("Wallet not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.deleteByUserId(USER_ID));

            verifyNoInteractions(walletRepository);
        }
    }
}