package com.finovara.financeservice.wallet.reservation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.wallet.reservation.dto.FundReservationDto;
import com.finovara.financeservice.wallet.reservation.dto.UnreserveReservationDto;
import com.finovara.financeservice.wallet.reservation.repository.FundReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FundReservationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long RESERVATION_ID = 10L;
    private static final String CODE = "123456";

    @Mock
    private FundReservationCommandService commandService;

    @Mock
    private FundReservationRepository fundReservationRepository;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @InjectMocks
    private FundReservationService service;

    @Nested
    class CreateReservation {

        @Mock
        private FundReservationDto dto;

        @BeforeEach
        void setUp() {
            when(dto.authorizationCode()).thenReturn(CODE);
        }

        @Test
        void shouldReturnReservationIdWhenAuthorizationIsConfirmed() {
            when(commandService.create(USER_ID, dto)).thenReturn(RESERVATION_ID);

            Long result = service.createReservation(USER_ID, dto);

            assertEquals(RESERVATION_ID, result);
        }

        @Test
        void shouldResolveCodeAndConfirmAuthorizationWhenCreatingReservation() {
            when(commandService.create(USER_ID, dto)).thenReturn(RESERVATION_ID);

            service.createReservation(USER_ID, dto);

            verify(additionalAuthorizationCodeResolver).resolve(CODE);
            verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
        }

        @Test
        void shouldDelegateCreationToCommandServiceWhenAuthorizationIsConfirmed() {
            when(commandService.create(USER_ID, dto)).thenReturn(RESERVATION_ID);

            service.createReservation(USER_ID, dto);

            verify(commandService).create(USER_ID, dto);
        }

        @Test
        void shouldThrowExceptionWhenAuthorizationConfirmationFails() {
            doThrow(new IllegalStateException("Invalid code"))
                    .when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class, () -> service.createReservation(USER_ID, dto));
        }

        @Test
        void shouldNotCreateReservationWhenAuthorizationConfirmationFails() {
            doThrow(new IllegalStateException("Invalid code"))
                    .when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class, () -> service.createReservation(USER_ID, dto));

            verifyNoInteractions(commandService);
        }

        @Test
        void shouldThrowExceptionWhenCodeResolvingFails() {
            when(additionalAuthorizationCodeResolver.resolve(CODE))
                    .thenThrow(new IllegalArgumentException("Unresolvable code"));

            assertThrows(IllegalArgumentException.class, () -> service.createReservation(USER_ID, dto));
        }

        @Test
        void shouldNotConfirmAuthorizationNorCreateReservationWhenCodeResolvingFails() {
            when(additionalAuthorizationCodeResolver.resolve(CODE))
                    .thenThrow(new IllegalArgumentException("Unresolvable code"));

            assertThrows(IllegalArgumentException.class, () -> service.createReservation(USER_ID, dto));

            verifyNoInteractions(authBackendClient);
            verifyNoInteractions(commandService);
        }

        @Test
        void shouldThrowExceptionWhenCommandServiceFails() {
            when(commandService.create(USER_ID, dto)).thenThrow(new IllegalStateException("Failure"));

            assertThrows(IllegalStateException.class, () -> service.createReservation(USER_ID, dto));
        }
    }

    @Nested
    class UnreserveReservation {

        @Mock
        private UnreserveReservationDto dto;

        @BeforeEach
        void setUp() {
            when(dto.authorizationCode()).thenReturn(CODE);
        }

        @Test
        void shouldConfirmAuthorizationAndDelegateUnreserveWhenCodeIsValid() {
            service.unreserveReservation(USER_ID, RESERVATION_ID, dto);

            verify(additionalAuthorizationCodeResolver).resolve(CODE);
            verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
            verify(commandService).unreserve(USER_ID, RESERVATION_ID, dto);
        }

        @Test
        void shouldThrowExceptionWhenAuthorizationConfirmationFailsOnUnreserve() {
            doThrow(new IllegalStateException("Invalid code"))
                    .when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class,
                    () -> service.unreserveReservation(USER_ID, RESERVATION_ID, dto));
        }

        @Test
        void shouldNotUnreserveWhenAuthorizationConfirmationFails() {
            doThrow(new IllegalStateException("Invalid code"))
                    .when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class,
                    () -> service.unreserveReservation(USER_ID, RESERVATION_ID, dto));

            verifyNoInteractions(commandService);
        }

        @Test
        void shouldThrowExceptionWhenCodeResolvingFailsOnUnreserve() {
            when(additionalAuthorizationCodeResolver.resolve(CODE))
                    .thenThrow(new IllegalArgumentException("Unresolvable code"));

            assertThrows(IllegalArgumentException.class,
                    () -> service.unreserveReservation(USER_ID, RESERVATION_ID, dto));
        }

        @Test
        void shouldNotConfirmAuthorizationNorUnreserveWhenCodeResolvingFails() {
            when(additionalAuthorizationCodeResolver.resolve(CODE))
                    .thenThrow(new IllegalArgumentException("Unresolvable code"));

            assertThrows(IllegalArgumentException.class,
                    () -> service.unreserveReservation(USER_ID, RESERVATION_ID, dto));

            verifyNoInteractions(authBackendClient);
            verifyNoInteractions(commandService);
        }

        @Test
        void shouldThrowExceptionWhenCommandServiceFailsOnUnreserve() {
            doThrow(new IllegalStateException("Failure"))
                    .when(commandService).unreserve(USER_ID, RESERVATION_ID, dto);

            assertThrows(IllegalStateException.class,
                    () -> service.unreserveReservation(USER_ID, RESERVATION_ID, dto));
        }
    }

    @Nested
    class ManualDeleteReservation {

        @Test
        void shouldConfirmAuthorizationAndDelegateDeleteWhenCodeIsValid() {
            service.manualDeleteReservation(USER_ID, RESERVATION_ID, CODE);

            verify(additionalAuthorizationCodeResolver).resolve(CODE);
            verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
            verify(commandService).delete(USER_ID, RESERVATION_ID);
        }

        @Test
        void shouldThrowExceptionWhenAuthorizationConfirmationFailsOnDelete() {
            doThrow(new IllegalStateException("Invalid code"))
                    .when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class,
                    () -> service.manualDeleteReservation(USER_ID, RESERVATION_ID, CODE));
        }

        @Test
        void shouldNotDeleteWhenAuthorizationConfirmationFails() {
            doThrow(new IllegalStateException("Invalid code"))
                    .when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class,
                    () -> service.manualDeleteReservation(USER_ID, RESERVATION_ID, CODE));

            verifyNoInteractions(commandService);
        }

        @Test
        void shouldThrowExceptionWhenCodeResolvingFailsOnDelete() {
            when(additionalAuthorizationCodeResolver.resolve(CODE))
                    .thenThrow(new IllegalArgumentException("Unresolvable code"));

            assertThrows(IllegalArgumentException.class,
                    () -> service.manualDeleteReservation(USER_ID, RESERVATION_ID, CODE));
        }

        @Test
        void shouldNotConfirmAuthorizationNorDeleteWhenCodeResolvingFails() {
            when(additionalAuthorizationCodeResolver.resolve(CODE))
                    .thenThrow(new IllegalArgumentException("Unresolvable code"));

            assertThrows(IllegalArgumentException.class,
                    () -> service.manualDeleteReservation(USER_ID, RESERVATION_ID, CODE));

            verifyNoInteractions(authBackendClient);
            verifyNoInteractions(commandService);
        }

        @Test
        void shouldThrowExceptionWhenCommandServiceFailsOnDelete() {
            doThrow(new IllegalStateException("Failure"))
                    .when(commandService).delete(USER_ID, RESERVATION_ID);

            assertThrows(IllegalStateException.class,
                    () -> service.manualDeleteReservation(USER_ID, RESERVATION_ID, CODE));
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteAllReservationsWhenUserDataIsDeleted() {
            service.deleteByUserId(USER_ID);

            verify(fundReservationRepository).deleteAllByUserId(USER_ID);
        }

        @Test
        void shouldNotRequireAuthorizationWhenUserDataIsDeleted() {
            service.deleteByUserId(USER_ID);

            verifyNoInteractions(authBackendClient);
            verifyNoInteractions(additionalAuthorizationCodeResolver);
            verifyNoInteractions(commandService);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFailsOnUserDataDeletion() {
            doThrow(new IllegalStateException("Failure"))
                    .when(fundReservationRepository).deleteAllByUserId(USER_ID);

            assertThrows(IllegalStateException.class, () -> service.deleteByUserId(USER_ID));
        }

        @Test
        void shouldNotDeleteReservationsOfOtherUsersWhenUserDataIsDeleted() {
            service.deleteByUserId(USER_ID);

            verify(fundReservationRepository, never()).deleteAllByUserId(2L);
        }
    }
}