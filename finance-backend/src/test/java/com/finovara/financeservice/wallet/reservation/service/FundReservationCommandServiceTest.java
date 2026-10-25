package com.finovara.financeservice.wallet.reservation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.reservation.dto.FundReservationDto;
import com.finovara.financeservice.wallet.reservation.dto.UnreserveReservationDto;
import com.finovara.financeservice.wallet.reservation.model.FundReservation;
import com.finovara.financeservice.wallet.reservation.repository.FundReservationRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FundReservationCommandServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long WALLET_ID = 5L;
    private static final Long RESERVATION_ID = 20L;
    private static final BigDecimal RESERVED_AMOUNT = new BigDecimal("100.00");

    @Mock
    private WalletManagerService walletManagerService;
    @Mock
    private FundReservationRepository fundReservationRepository;
    @Mock
    private Wallet wallet;
    @Mock
    private FundReservationDto fundReservationDto;
    @Mock
    private UnreserveReservationDto unreserveReservationDto;

    @InjectMocks
    private FundReservationCommandService service;

    private FundReservation reservation;

    @BeforeEach
    void setUp() {
        reservation = FundReservation.builder()
                .id(RESERVATION_ID)
                .amount(RESERVED_AMOUNT)
                .walletId(WALLET_ID)
                .build();
    }

    @Nested
    class Create {

        @Test
        void shouldReturnReservationIdWhenWalletHasEnoughFunds() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(fundReservationDto.amount()).thenReturn(RESERVED_AMOUNT);
            when(fundReservationRepository.save(any(FundReservation.class))).thenReturn(reservation);

            Long result = service.create(USER_ID, fundReservationDto);

            assertEquals(RESERVATION_ID, result);
        }

        @Test
        void shouldReserveAmountOnWalletWhenCreatingReservation() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(fundReservationDto.amount()).thenReturn(RESERVED_AMOUNT);
            when(fundReservationRepository.save(any(FundReservation.class))).thenReturn(reservation);

            service.create(USER_ID, fundReservationDto);

            verify(wallet).reserve(RESERVED_AMOUNT);
        }

        @Test
        void shouldSaveReservationWithAmountAndWalletIdWhenCreatingReservation() {
            ArgumentCaptor<FundReservation> captor = ArgumentCaptor.forClass(FundReservation.class);
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(fundReservationDto.amount()).thenReturn(RESERVED_AMOUNT);
            when(fundReservationRepository.save(any(FundReservation.class))).thenReturn(reservation);

            service.create(USER_ID, fundReservationDto);

            verify(fundReservationRepository).save(captor.capture());
            assertEquals(RESERVED_AMOUNT, captor.getValue().getAmount());
            assertEquals(WALLET_ID, captor.getValue().getWalletId());
        }

        @Test
        void shouldThrowExceptionWhenWalletDoesNotExist() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenThrow(new RequestedEntityNotFoundException("Wallet not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.create(USER_ID, fundReservationDto));

            verifyNoInteractions(fundReservationRepository);
        }

        @Test
        void shouldThrowExceptionWhenWalletCannotReserveAmount() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            when(fundReservationDto.amount()).thenReturn(RESERVED_AMOUNT);
            doThrow(new InvalidInputException("Insufficient funds")).when(wallet).reserve(RESERVED_AMOUNT);

            assertThrows(InvalidInputException.class, () -> service.create(USER_ID, fundReservationDto));

            verifyNoInteractions(fundReservationRepository);
        }
    }

    @Nested
    class Unreserve {

        @BeforeEach
        void setUp() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getId()).thenReturn(WALLET_ID);
        }

        @Test
        void shouldReduceReservationAmountWhenUnreservingPartially() {
            when(unreserveReservationDto.amount()).thenReturn(new BigDecimal("40.00"));
            when(fundReservationRepository.findByIdAndWalletId(RESERVATION_ID, WALLET_ID)).thenReturn(Optional.of(reservation));

            service.unreserve(USER_ID, RESERVATION_ID, unreserveReservationDto);

            assertEquals(0, new BigDecimal("60.00").compareTo(reservation.getAmount()));
            verify(wallet).unreserve(new BigDecimal("40.00"));
            verify(fundReservationRepository, never()).delete(any(FundReservation.class));
        }

        @Test
        void shouldDeleteReservationWhenUnreservingWholeAmount() {
            when(unreserveReservationDto.amount()).thenReturn(RESERVED_AMOUNT);
            when(fundReservationRepository.findByIdAndWalletId(RESERVATION_ID, WALLET_ID)).thenReturn(Optional.of(reservation));

            service.unreserve(USER_ID, RESERVATION_ID, unreserveReservationDto);

            verify(wallet).unreserve(RESERVED_AMOUNT);
            verify(fundReservationRepository).delete(reservation);
        }

        @Test
        void shouldKeepReservationAmountUnchangedWhenUnreservingZero() {
            when(unreserveReservationDto.amount()).thenReturn(BigDecimal.ZERO);
            when(fundReservationRepository.findByIdAndWalletId(RESERVATION_ID, WALLET_ID)).thenReturn(Optional.of(reservation));

            service.unreserve(USER_ID, RESERVATION_ID, unreserveReservationDto);

            assertEquals(0, RESERVED_AMOUNT.compareTo(reservation.getAmount()));
            verify(fundReservationRepository, never()).delete(any(FundReservation.class));
        }

        @Test
        void shouldThrowExceptionWhenUnreservingMoreThanReserved() {
            when(unreserveReservationDto.amount()).thenReturn(new BigDecimal("100.01"));
            when(fundReservationRepository.findByIdAndWalletId(RESERVATION_ID, WALLET_ID)).thenReturn(Optional.of(reservation));

            assertThrows(InvalidInputException.class, () -> service.unreserve(USER_ID, RESERVATION_ID, unreserveReservationDto));

            assertEquals(0, RESERVED_AMOUNT.compareTo(reservation.getAmount()));
            verify(wallet, never()).unreserve(any(BigDecimal.class));
            verify(fundReservationRepository, never()).delete(any(FundReservation.class));
        }

        @Test
        void shouldThrowExceptionWhenReservationDoesNotBelongToWallet() {
            when(fundReservationRepository.findByIdAndWalletId(RESERVATION_ID, WALLET_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.unreserve(USER_ID, RESERVATION_ID, unreserveReservationDto));

            verify(wallet, never()).unreserve(any(BigDecimal.class));
            verify(fundReservationRepository, never()).delete(any(FundReservation.class));
        }
    }

    @Nested
    class UnreserveWhenWalletDoesNotExist {

        @Test
        void shouldThrowExceptionWhenWalletDoesNotExist() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenThrow(new RequestedEntityNotFoundException("Wallet not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.unreserve(USER_ID, RESERVATION_ID, unreserveReservationDto));

            verifyNoInteractions(fundReservationRepository);
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldUnreserveWholeAmountAndDeleteReservationWhenReservationExists() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(fundReservationRepository.findByIdAndWalletId(RESERVATION_ID, WALLET_ID)).thenReturn(Optional.of(reservation));

            service.delete(USER_ID, RESERVATION_ID);

            verify(wallet).unreserve(RESERVED_AMOUNT);
            verify(fundReservationRepository).delete(reservation);
        }

        @Test
        void shouldThrowExceptionWhenReservationDoesNotBelongToWallet() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(fundReservationRepository.findByIdAndWalletId(RESERVATION_ID, WALLET_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.delete(USER_ID, RESERVATION_ID));

            verify(wallet, never()).unreserve(any(BigDecimal.class));
            verify(fundReservationRepository, never()).delete(any(FundReservation.class));
        }

        @Test
        void shouldThrowExceptionWhenWalletDoesNotExist() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenThrow(new RequestedEntityNotFoundException("Wallet not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.delete(USER_ID, RESERVATION_ID));

            verifyNoInteractions(fundReservationRepository);
        }
    }
}