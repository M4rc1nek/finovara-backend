package com.finovara.financeservice.wallet.reservation.service;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FundReservationCommandServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long WALLET_ID = 5L;
    private static final Long RESERVATION_ID = 10L;
    private static final BigDecimal RESERVED_AMOUNT = BigDecimal.valueOf(100);

    @Mock
    private WalletManagerService walletManagerService;

    @Mock
    private FundReservationRepository fundReservationRepository;

    @Mock
    private Wallet wallet;

    @Mock
    private FundReservation reservation;

    @InjectMocks
    private FundReservationCommandService service;

    @Nested
    class Create {

        @Mock
        private FundReservationDto dto;

        @Mock
        private FundReservation savedReservation;

        @Captor
        private ArgumentCaptor<FundReservation> reservationCaptor;

        @BeforeEach
        void setUp() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(dto.amount()).thenReturn(TEN);
            when(fundReservationRepository.save(reservationCaptor.capture())).thenReturn(savedReservation);
            when(savedReservation.getId()).thenReturn(RESERVATION_ID);
        }

        @Test
        void shouldReturnSavedReservationIdWhenReservationIsCreated() {
            Long result = service.create(USER_ID, dto);

            assertEquals(RESERVATION_ID, result);
        }

        @Test
        void shouldReserveAmountOnWalletWhenReservationIsCreated() {
            service.create(USER_ID, dto);

            verify(wallet).reserve(TEN);
        }

        @Test
        void shouldSaveReservationWithDtoAmountWhenReservationIsCreated() {
            service.create(USER_ID, dto);

            assertEquals(TEN, reservationCaptor.getValue().getAmount());
        }

        @Test
        void shouldSaveReservationWithWalletIdWhenReservationIsCreated() {
            service.create(USER_ID, dto);

            assertEquals(WALLET_ID, reservationCaptor.getValue().getWalletId());
        }

        @Test
        void shouldLockWalletForUpdateWhenReservationIsCreated() {
            service.create(USER_ID, dto);

            verify(walletManagerService).getWalletForUpdateOrThrow(USER_ID);
        }
    }

    @Nested
    class CreateWhenWalletReserveFails {

        @Mock
        private FundReservationDto dto;

        @BeforeEach
        void setUp() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            when(dto.amount()).thenReturn(TEN);
            doThrow(new InvalidInputException("Insufficient funds")).when(wallet).reserve(TEN);
        }

        @Test
        void shouldThrowExceptionWhenWalletCannotReserveAmount() {
            assertThrows(InvalidInputException.class, () -> service.create(USER_ID, dto));
        }

        @Test
        void shouldNotSaveReservationWhenWalletCannotReserveAmount() {
            assertThrows(InvalidInputException.class, () -> service.create(USER_ID, dto));

            verifyNoInteractions(fundReservationRepository);
        }
    }

    @Nested
    class CreateWhenWalletNotFound {

        @Mock
        private FundReservationDto dto;

        @BeforeEach
        void setUp() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("Wallet not found"));
        }

        @Test
        void shouldThrowExceptionWhenWalletDoesNotExistOnCreate() {
            assertThrows(RequestedEntityNotFoundException.class, () -> service.create(USER_ID, dto));
        }

        @Test
        void shouldNotSaveReservationWhenWalletDoesNotExistOnCreate() {
            assertThrows(RequestedEntityNotFoundException.class, () -> service.create(USER_ID, dto));

            verifyNoInteractions(fundReservationRepository);
        }
    }

    @Nested
    class Unreserve {

        @Mock
        private UnreserveReservationDto dto;

        @BeforeEach
        void setUp() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(fundReservationRepository.findByIdAndWalletId(RESERVATION_ID, WALLET_ID))
                    .thenReturn(Optional.of(reservation));
            when(reservation.getAmount()).thenReturn(RESERVED_AMOUNT);
        }

        @Test
        void shouldDecreaseReservationAmountWhenUnreservingPartially() {
            when(dto.amount()).thenReturn(BigDecimal.valueOf(60));

            service.unreserve(USER_ID, RESERVATION_ID, dto);

            verify(reservation).setAmount(BigDecimal.valueOf(40));
        }

        @Test
        void shouldUnreserveAmountOnWalletWhenUnreservingPartially() {
            when(dto.amount()).thenReturn(BigDecimal.valueOf(60));

            service.unreserve(USER_ID, RESERVATION_ID, dto);

            verify(wallet).unreserve(BigDecimal.valueOf(60));
        }

        @Test
        void shouldNotDeleteReservationWhenUnreservingPartially() {
            when(dto.amount()).thenReturn(BigDecimal.valueOf(60));

            service.unreserve(USER_ID, RESERVATION_ID, dto);

            verify(fundReservationRepository, never()).delete(reservation);
        }

        @Test
        void shouldDecreaseReservationAmountWhenUnreservingOneLessThanReserved() {
            when(dto.amount()).thenReturn(BigDecimal.valueOf(99));

            service.unreserve(USER_ID, RESERVATION_ID, dto);

            verify(reservation).setAmount(ONE);
        }

        @Test
        void shouldDeleteReservationWhenUnreservingWholeAmount() {
            when(dto.amount()).thenReturn(RESERVED_AMOUNT);

            service.unreserve(USER_ID, RESERVATION_ID, dto);

            verify(fundReservationRepository).delete(reservation);
        }

        @Test
        void shouldUnreserveWholeAmountOnWalletWhenUnreservingWholeAmount() {
            when(dto.amount()).thenReturn(RESERVED_AMOUNT);

            service.unreserve(USER_ID, RESERVATION_ID, dto);

            verify(wallet).unreserve(RESERVED_AMOUNT);
        }

        @Test
        void shouldNotChangeReservationAmountWhenUnreservingWholeAmount() {
            when(dto.amount()).thenReturn(RESERVED_AMOUNT);

            service.unreserve(USER_ID, RESERVATION_ID, dto);

            verify(reservation, never()).setAmount(BigDecimal.ZERO);
        }

        @Test
        void shouldDeleteReservationWhenUnreservedAmountEqualsReservedAmountWithDifferentScale() {
            when(dto.amount()).thenReturn(new BigDecimal("100.00"));

            service.unreserve(USER_ID, RESERVATION_ID, dto);

            verify(fundReservationRepository).delete(reservation);
        }

        @Test
        void shouldThrowExceptionWhenUnreservingMoreThanReserved() {
            when(dto.amount()).thenReturn(BigDecimal.valueOf(101));

            assertThrows(InvalidInputException.class, () -> service.unreserve(USER_ID, RESERVATION_ID, dto));
        }

        @Test
        void shouldNotModifyWalletNorReservationWhenUnreservingMoreThanReserved() {
            when(dto.amount()).thenReturn(BigDecimal.valueOf(101));

            assertThrows(InvalidInputException.class, () -> service.unreserve(USER_ID, RESERVATION_ID, dto));

            verify(wallet, never()).unreserve(BigDecimal.valueOf(101));
            verify(fundReservationRepository, never()).delete(reservation);
            verify(reservation, never()).setAmount(BigDecimal.ZERO);
        }
    }

    @Nested
    class UnreserveWhenReservationNotFound {

        @Mock
        private UnreserveReservationDto dto;

        @BeforeEach
        void setUp() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(fundReservationRepository.findByIdAndWalletId(RESERVATION_ID, WALLET_ID))
                    .thenReturn(Optional.empty());
        }

        @Test
        void shouldThrowExceptionWhenReservationDoesNotExistOnUnreserve() {
            assertThrows(RequestedEntityNotFoundException.class,
                    () -> service.unreserve(USER_ID, RESERVATION_ID, dto));
        }

        @Test
        void shouldNotUnreserveWalletFundsWhenReservationDoesNotExistOnUnreserve() {
            assertThrows(RequestedEntityNotFoundException.class,
                    () -> service.unreserve(USER_ID, RESERVATION_ID, dto));

            verify(wallet, never()).unreserve(TEN);
        }
    }

    @Nested
    class UnreserveWhenWalletNotFound {

        @Mock
        private UnreserveReservationDto dto;

        @BeforeEach
        void setUp() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("Wallet not found"));
        }

        @Test
        void shouldThrowExceptionWhenWalletDoesNotExistOnUnreserve() {
            assertThrows(RequestedEntityNotFoundException.class,
                    () -> service.unreserve(USER_ID, RESERVATION_ID, dto));
        }

        @Test
        void shouldNotAccessRepositoryWhenWalletDoesNotExistOnUnreserve() {
            assertThrows(RequestedEntityNotFoundException.class,
                    () -> service.unreserve(USER_ID, RESERVATION_ID, dto));

            verifyNoInteractions(fundReservationRepository);
        }
    }

    @Nested
    class Delete {

        @BeforeEach
        void setUp() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(fundReservationRepository.findByIdAndWalletId(RESERVATION_ID, WALLET_ID))
                    .thenReturn(Optional.of(reservation));
            when(reservation.getAmount()).thenReturn(RESERVED_AMOUNT);
        }

        @Test
        void shouldUnreserveWholeReservationAmountWhenReservationIsDeleted() {
            service.delete(USER_ID, RESERVATION_ID);

            verify(wallet).unreserve(RESERVED_AMOUNT);
        }

        @Test
        void shouldDeleteReservationWhenReservationIsDeleted() {
            service.delete(USER_ID, RESERVATION_ID);

            verify(fundReservationRepository).delete(reservation);
        }

        @Test
        void shouldLockWalletForUpdateWhenReservationIsDeleted() {
            service.delete(USER_ID, RESERVATION_ID);

            verify(walletManagerService).getWalletForUpdateOrThrow(USER_ID);
        }

        @Test
        void shouldThrowExceptionWhenWalletFailsToUnreserveOnDelete() {
            doThrow(new InvalidInputException("Cannot unreserve")).when(wallet).unreserve(RESERVED_AMOUNT);

            assertThrows(InvalidInputException.class, () -> service.delete(USER_ID, RESERVATION_ID));
        }

        @Test
        void shouldNotDeleteReservationWhenWalletFailsToUnreserveOnDelete() {
            doThrow(new InvalidInputException("Cannot unreserve")).when(wallet).unreserve(RESERVED_AMOUNT);

            assertThrows(InvalidInputException.class, () -> service.delete(USER_ID, RESERVATION_ID));

            verify(fundReservationRepository, never()).delete(reservation);
        }
    }

    @Nested
    class DeleteWhenReservationNotFound {

        @BeforeEach
        void setUp() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(fundReservationRepository.findByIdAndWalletId(RESERVATION_ID, WALLET_ID))
                    .thenReturn(Optional.empty());
        }

        @Test
        void shouldThrowExceptionWhenReservationDoesNotExistOnDelete() {
            assertThrows(RequestedEntityNotFoundException.class,
                    () -> service.delete(USER_ID, RESERVATION_ID));
        }

        @Test
        void shouldNotUnreserveWalletFundsWhenReservationDoesNotExistOnDelete() {
            assertThrows(RequestedEntityNotFoundException.class,
                    () -> service.delete(USER_ID, RESERVATION_ID));

            verify(wallet, never()).unreserve(RESERVED_AMOUNT);
        }
    }

    @Nested
    class DeleteWhenWalletNotFound {

        @BeforeEach
        void setUp() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("Wallet not found"));
        }

        @Test
        void shouldThrowExceptionWhenWalletDoesNotExistOnDelete() {
            assertThrows(RequestedEntityNotFoundException.class,
                    () -> service.delete(USER_ID, RESERVATION_ID));
        }

        @Test
        void shouldNotAccessRepositoryWhenWalletDoesNotExistOnDelete() {
            assertThrows(RequestedEntityNotFoundException.class,
                    () -> service.delete(USER_ID, RESERVATION_ID));

            verifyNoInteractions(fundReservationRepository);
        }
    }
}