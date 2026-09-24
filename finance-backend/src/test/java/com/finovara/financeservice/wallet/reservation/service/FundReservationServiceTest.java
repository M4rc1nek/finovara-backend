package com.finovara.financeservice.wallet.reservation.service;

import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.reservation.dto.FundReservationDto;
import com.finovara.financeservice.wallet.reservation.dto.UnreserveReservationDto;
import com.finovara.financeservice.wallet.reservation.model.FundReservation;
import com.finovara.financeservice.wallet.reservation.repository.FundReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundReservationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long WALLET_ID = 10L;
    private static final Long RESERVATION_ID = 100L;
    private static final String AUTHORIZATION_CODE = "aFH6mh1ksJ6";

    @Mock
    private WalletManagerService walletManagerService;

    @Mock
    private FundReservationRepository fundReservationRepository;

    @Mock
    private Wallet wallet;

    @Mock
    private FundReservation reservation;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @InjectMocks
    private FundReservationService fundReservationService;

    @BeforeEach
    void setUp() {
        when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
    }

    @Nested
    class CreateReservation {

        @Test
        void shouldReturnSavedReservationIdWhenValidInput() {
            FundReservationDto dto = new FundReservationDto(null, ExpenseCategory.SAVINGS, BigDecimal.valueOf(50), AUTHORIZATION_CODE);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(fundReservationRepository.save(any(FundReservation.class))).thenReturn(reservation);
            when(reservation.getId()).thenReturn(RESERVATION_ID);

            Long result = fundReservationService.createReservation(USER_ID, dto);

            verify(authBackendClient).confirmAuthorizationCode(USER_ID, additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE));
            assertEquals(RESERVATION_ID, result);
        }

        @Test
        void shouldCallWalletReserveWithCorrectAmountWhenCreatingReservation() {
            BigDecimal amount = BigDecimal.valueOf(75);
            FundReservationDto dto = new FundReservationDto(null, ExpenseCategory.SAVINGS, amount, AUTHORIZATION_CODE);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(fundReservationRepository.save(any(FundReservation.class))).thenReturn(reservation);

            fundReservationService.createReservation(USER_ID, dto);

            verify(authBackendClient).confirmAuthorizationCode(USER_ID, additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE));
            verify(wallet, times(1)).reserve(amount);
        }

        @Test
        void shouldSaveReservationWithWalletIdFromWallet() {
            FundReservationDto dto = new FundReservationDto(null, ExpenseCategory.SAVINGS, BigDecimal.TEN, AUTHORIZATION_CODE);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(fundReservationRepository.save(any(FundReservation.class))).thenAnswer(invocation -> {
                FundReservation saved = invocation.getArgument(0);
                assertEquals(WALLET_ID, saved.getWalletId());
                assertEquals(ExpenseCategory.SAVINGS, saved.getCategory());
                assertEquals(BigDecimal.TEN, saved.getAmount());
                return reservation;
            });

            fundReservationService.createReservation(USER_ID, dto);

            verify(authBackendClient).confirmAuthorizationCode(USER_ID, additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE));
            verify(fundReservationRepository, times(1)).save(any(FundReservation.class));
        }

        @Test
        void shouldThrowExceptionWhenWalletDoesNotExist() {
            FundReservationDto dto = new FundReservationDto(null, ExpenseCategory.SAVINGS, BigDecimal.TEN, AUTHORIZATION_CODE);
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("Wallet not found"));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> fundReservationService.createReservation(USER_ID, dto));
            verify(fundReservationRepository, never()).save(any(FundReservation.class));
        }
    }

    @Nested
    class UnreserveReservation {

        @Test
        void shouldUnreserveAmountWhenAmountIsLessThanReserved() {
            UnreserveReservationDto unreserveDto = new UnreserveReservationDto(BigDecimal.valueOf(20), AUTHORIZATION_CODE);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(fundReservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
            when(reservation.getWalletId()).thenReturn(WALLET_ID);
            when(reservation.getAmount()).thenReturn(BigDecimal.valueOf(50));

            fundReservationService.unreserveReservation(USER_ID, RESERVATION_ID, unreserveDto);

            verify(authBackendClient).confirmAuthorizationCode(USER_ID, additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE));
            verify(wallet, times(1)).unreserve(BigDecimal.valueOf(20));
        }

        @Test
        void shouldSetRemainingAmountAfterUnreserving() {
            UnreserveReservationDto unreserveDto = new UnreserveReservationDto(BigDecimal.valueOf(20), AUTHORIZATION_CODE);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(fundReservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
            when(reservation.getWalletId()).thenReturn(WALLET_ID);
            when(reservation.getAmount()).thenReturn(BigDecimal.valueOf(50));

            fundReservationService.unreserveReservation(USER_ID, RESERVATION_ID, unreserveDto);

            verify(authBackendClient).confirmAuthorizationCode(USER_ID, additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE));
            verify(reservation, times(1)).setAmount(BigDecimal.valueOf(30));
            verify(fundReservationRepository, times(1)).save(reservation);
        }

        @Test
        void shouldAllowUnreservingExactReservedAmount() {
            UnreserveReservationDto unreserveDto = new UnreserveReservationDto(BigDecimal.valueOf(50), AUTHORIZATION_CODE);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(fundReservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
            when(reservation.getWalletId()).thenReturn(WALLET_ID);
            when(reservation.getAmount()).thenReturn(BigDecimal.valueOf(50));

            fundReservationService.unreserveReservation(USER_ID, RESERVATION_ID, unreserveDto);

            verify(authBackendClient).confirmAuthorizationCode(USER_ID, additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE));
            verify(reservation, times(1)).setAmount(BigDecimal.ZERO);
        }

        @Test
        void shouldThrowInvalidInputExceptionWhenAmountExceedsReservedAmount() {
            UnreserveReservationDto unreserveDto = new UnreserveReservationDto(BigDecimal.valueOf(100), AUTHORIZATION_CODE);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(fundReservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
            when(reservation.getWalletId()).thenReturn(WALLET_ID);
            when(reservation.getAmount()).thenReturn(BigDecimal.valueOf(50));

            assertThrows(InvalidInputException.class, () -> fundReservationService.unreserveReservation(USER_ID, RESERVATION_ID, unreserveDto));
        }

        @Test
        void shouldNotCallWalletUnreserveWhenAmountExceedsReservedAmount() {
            UnreserveReservationDto unreserveDto = new UnreserveReservationDto(BigDecimal.valueOf(100), AUTHORIZATION_CODE);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(fundReservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
            when(reservation.getWalletId()).thenReturn(WALLET_ID);
            when(reservation.getAmount()).thenReturn(BigDecimal.valueOf(50));

            assertThrows(InvalidInputException.class, () -> fundReservationService.unreserveReservation(USER_ID, RESERVATION_ID, unreserveDto));
            verify(wallet, never()).unreserve(any(BigDecimal.class));
            verify(fundReservationRepository, never()).save(any(FundReservation.class));
        }

        @Test
        void shouldThrowExceptionWhenReservationNotFound() {
            UnreserveReservationDto unreserveDto = new UnreserveReservationDto(BigDecimal.TEN, AUTHORIZATION_CODE);
            when(fundReservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> fundReservationService.unreserveReservation(USER_ID, RESERVATION_ID, unreserveDto));
        }

        @Test
        void shouldThrowExceptionWhenReservationBelongsToDifferentWallet() {
            UnreserveReservationDto unreserveDto = new UnreserveReservationDto(BigDecimal.TEN, AUTHORIZATION_CODE);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(fundReservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
            when(reservation.getWalletId()).thenReturn(999L);

            assertThrows(RequestedEntityNotFoundException.class, () -> fundReservationService.unreserveReservation(USER_ID, RESERVATION_ID, unreserveDto));
        }

        @Test
        void shouldThrowExceptionWhenWalletDoesNotExist() {
            UnreserveReservationDto unreserveDto = new UnreserveReservationDto(BigDecimal.TEN, AUTHORIZATION_CODE);
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("Wallet not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> fundReservationService.unreserveReservation(USER_ID, RESERVATION_ID, unreserveDto));
        }
    }

    @Nested
    class CancelReservation {

        @Test
        void shouldUnreserveReservationAmountWhenCancelling() {
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(fundReservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
            when(reservation.getWalletId()).thenReturn(WALLET_ID);
            when(reservation.getAmount()).thenReturn(BigDecimal.valueOf(40));

            fundReservationService.cancelReservation(USER_ID, RESERVATION_ID, AUTHORIZATION_CODE);

            verify(authBackendClient).confirmAuthorizationCode(USER_ID, additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE));
            verify(wallet, times(1)).unreserve(BigDecimal.valueOf(40));
        }

        @Test
        void shouldDeleteReservationWhenCancelling() {
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(fundReservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
            when(reservation.getWalletId()).thenReturn(WALLET_ID);
            when(reservation.getAmount()).thenReturn(BigDecimal.valueOf(40));

            fundReservationService.cancelReservation(USER_ID, RESERVATION_ID, AUTHORIZATION_CODE);

            verify(authBackendClient).confirmAuthorizationCode(USER_ID, additionalAuthorizationCodeResolver.resolve(AUTHORIZATION_CODE));
            verify(fundReservationRepository, times(1)).delete(reservation);
        }

        @Test
        void shouldThrowExceptionWhenReservationNotFound() {
            when(fundReservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> fundReservationService.cancelReservation(USER_ID, RESERVATION_ID, AUTHORIZATION_CODE));
            verify(fundReservationRepository, never()).delete(any(FundReservation.class));
        }

        @Test
        void shouldThrowExceptionWhenReservationBelongsToDifferentWallet() {
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(fundReservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
            when(reservation.getWalletId()).thenReturn(999L);

            assertThrows(RequestedEntityNotFoundException.class, () -> fundReservationService.cancelReservation(USER_ID, RESERVATION_ID, AUTHORIZATION_CODE));
            verify(fundReservationRepository, never()).delete(any(FundReservation.class));
        }

        @Test
        void shouldThrowExceptionWhenWalletDoesNotExist() {
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("Wallet not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> fundReservationService.cancelReservation(USER_ID, RESERVATION_ID, AUTHORIZATION_CODE));
            verify(fundReservationRepository, never()).findById(any(Long.class));
        }
    }
}