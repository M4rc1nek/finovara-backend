package com.finovara.financeservice.wallet.reservation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.reservation.dto.FundReservationDto;
import com.finovara.financeservice.wallet.reservation.dto.UnreserveReservationDto;
import com.finovara.financeservice.wallet.reservation.model.FundReservation;
import com.finovara.financeservice.wallet.reservation.repository.FundReservationRepository;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FundReservationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long WALLET_ID = 5L;
    private static final Long RESERVATION_ID = 20L;
    private static final String AUTH_CODE = "123456";

    @Mock
    private FundReservationCommandService commandService;
    @Mock
    private FundReservationRepository fundReservationRepository;
    @Mock
    private WalletManagerService walletManagerService;
    @Mock
    private AuthBackendClient authBackendClient;
    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;
    @Mock
    private FundReservationDto fundReservationDto;
    @Mock
    private UnreserveReservationDto unreserveReservationDto;
    @Mock
    private Wallet wallet;

    @InjectMocks
    private FundReservationService service;

    @Nested
    class CreateReservation {

        @Test
        void shouldReturnReservationIdWhenAuthorizationCodeIsValid() {
            when(fundReservationDto.authorizationCode()).thenReturn(AUTH_CODE);
            when(commandService.create(USER_ID, fundReservationDto)).thenReturn(RESERVATION_ID);

            Long result = service.createReservation(USER_ID, fundReservationDto);

            assertEquals(RESERVATION_ID, result);
        }

        @Test
        void shouldConfirmAuthorizationCodeWhenCreatingReservation() {
            when(fundReservationDto.authorizationCode()).thenReturn(AUTH_CODE);
            when(commandService.create(USER_ID, fundReservationDto)).thenReturn(RESERVATION_ID);

            service.createReservation(USER_ID, fundReservationDto);

            verify(additionalAuthorizationCodeResolver).resolve(AUTH_CODE);
            verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
        }

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeIsInvalid() {
            when(fundReservationDto.authorizationCode()).thenReturn(AUTH_CODE);
            doThrow(new IllegalStateException("Invalid code")).when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class, () -> service.createReservation(USER_ID, fundReservationDto));

            verifyNoInteractions(commandService);
        }

        @Test
        void shouldThrowExceptionWhenCommandServiceFails() {
            when(fundReservationDto.authorizationCode()).thenReturn(AUTH_CODE);
            when(commandService.create(USER_ID, fundReservationDto)).thenThrow(new InvalidInputException("Insufficient funds"));

            assertThrows(InvalidInputException.class, () -> service.createReservation(USER_ID, fundReservationDto));
        }
    }

    @Nested
    class UnreserveReservation {

        @Test
        void shouldDelegateToCommandServiceWhenAuthorizationCodeIsValid() {
            when(unreserveReservationDto.authorizationCode()).thenReturn(AUTH_CODE);

            service.unreserveReservation(USER_ID, RESERVATION_ID, unreserveReservationDto);

            verify(additionalAuthorizationCodeResolver).resolve(AUTH_CODE);
            verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
            verify(commandService).unreserve(USER_ID, RESERVATION_ID, unreserveReservationDto);
        }

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeIsInvalid() {
            when(unreserveReservationDto.authorizationCode()).thenReturn(AUTH_CODE);
            doThrow(new IllegalStateException("Invalid code")).when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class, () -> service.unreserveReservation(USER_ID, RESERVATION_ID, unreserveReservationDto));

            verifyNoInteractions(commandService);
        }

        @Test
        void shouldThrowExceptionWhenCommandServiceFails() {
            when(unreserveReservationDto.authorizationCode()).thenReturn(AUTH_CODE);
            doThrow(new RequestedEntityNotFoundException("Reservation not found")).when(commandService)
                    .unreserve(USER_ID, RESERVATION_ID, unreserveReservationDto);

            assertThrows(RequestedEntityNotFoundException.class, () -> service.unreserveReservation(USER_ID, RESERVATION_ID, unreserveReservationDto));
        }
    }

    @Nested
    class ManualDeleteReservation {

        @Test
        void shouldDelegateToCommandServiceWhenAuthorizationCodeIsValid() {
            service.manualDeleteReservation(USER_ID, RESERVATION_ID, AUTH_CODE);

            verify(additionalAuthorizationCodeResolver).resolve(AUTH_CODE);
            verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
            verify(commandService).delete(USER_ID, RESERVATION_ID);
        }

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeIsInvalid() {
            doThrow(new IllegalStateException("Invalid code")).when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class, () -> service.manualDeleteReservation(USER_ID, RESERVATION_ID, AUTH_CODE));

            verifyNoInteractions(commandService);
        }

        @Test
        void shouldThrowExceptionWhenCommandServiceFails() {
            doThrow(new RequestedEntityNotFoundException("Reservation not found")).when(commandService).delete(USER_ID, RESERVATION_ID);

            assertThrows(RequestedEntityNotFoundException.class, () -> service.manualDeleteReservation(USER_ID, RESERVATION_ID, AUTH_CODE));
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteAllReservationsWhenWalletHasReservations() {
            List<FundReservation> reservations = List.of(
                    FundReservation.builder().id(1L).walletId(WALLET_ID).build(),
                    FundReservation.builder().id(2L).walletId(WALLET_ID).build());
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(fundReservationRepository.findAllByWalletId(WALLET_ID)).thenReturn(reservations);

            service.deleteByUserId(USER_ID);

            verify(fundReservationRepository).deleteAll(reservations);
        }

        @Test
        void shouldDeleteEmptyCollectionWhenWalletHasNoReservations() {
            List<FundReservation> reservations = List.of();
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(fundReservationRepository.findAllByWalletId(WALLET_ID)).thenReturn(reservations);

            service.deleteByUserId(USER_ID);

            verify(fundReservationRepository).deleteAll(reservations);
        }

        @Test
        void shouldThrowExceptionWhenWalletDoesNotExist() {
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenThrow(new RequestedEntityNotFoundException("Wallet not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.deleteByUserId(USER_ID));

            verifyNoInteractions(fundReservationRepository);
        }
    }
}