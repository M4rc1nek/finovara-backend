package com.finovara.financeservice.wallet.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.mainaccount.transaction.event.wallet.WalletBalanceChangedEvent;
import com.finovara.contracts.util.model.ExpenseCategory;
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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long WALLET_ID = 10L;
    private static final Long RESERVATION_ID = 5L;
    private static final Long OTHER_RESERVATION_ID = 6L;
    private static final String BALANCE_CHANGED_TOPIC = "wallet.balance-changed";

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

        private static final BigDecimal AMOUNT = BigDecimal.valueOf(50);

        @Test
        void shouldThrowExceptionWhenWalletDoesNotExist() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenThrow(new RequestedEntityNotFoundException("Wallet not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> walletService.addBalanceToWallet(USER_ID, AMOUNT));

            verifyNoInteractions(wallet);
            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        void shouldThrowExceptionWhenDepositFails() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            doThrow(new IllegalArgumentException("Invalid amount")).when(wallet).deposit(AMOUNT);

            assertThrows(IllegalArgumentException.class, () -> walletService.addBalanceToWallet(USER_ID, AMOUNT));

            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        void shouldReturnAvailableAmountEqualToBalanceWhenNothingIsReserved() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(wallet.getBalance()).thenReturn(BigDecimal.valueOf(75));
            when(wallet.getReservedAmount()).thenReturn(BigDecimal.ZERO);

            WalletDto result = walletService.addBalanceToWallet(USER_ID, AMOUNT);

            assertThat(result.availableAmount()).isEqualByComparingTo(BigDecimal.valueOf(75));
        }

        @Test
        void shouldReturnZeroAvailableAmountWhenWholeBalanceIsReserved() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(wallet.getBalance()).thenReturn(BigDecimal.valueOf(100));
            when(wallet.getReservedAmount()).thenReturn(BigDecimal.valueOf(100));

            WalletDto result = walletService.addBalanceToWallet(USER_ID, AMOUNT);

            assertThat(result.availableAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Nested
        class WhenWalletExists {

            @BeforeEach
            void setUp() {
                when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
                when(wallet.getId()).thenReturn(WALLET_ID);
                when(wallet.getBalance()).thenReturn(BigDecimal.valueOf(150));
                when(wallet.getReservedAmount()).thenReturn(BigDecimal.valueOf(20));
            }

            @Test
            void shouldReturnWalletDtoWithCalculatedAvailableAmountWhenWalletExists() {
                WalletDto result = walletService.addBalanceToWallet(USER_ID, AMOUNT);

                assertThat(result).isEqualTo(new WalletDto(WALLET_ID, USER_ID, BigDecimal.valueOf(150), BigDecimal.valueOf(20), BigDecimal.valueOf(130)));
            }

            @Test
            void shouldFetchWalletForUpdateWhenAddingBalance() {
                walletService.addBalanceToWallet(USER_ID, AMOUNT);

                verify(walletManagerService).getWalletForUpdateOrThrow(USER_ID);
            }

            @Test
            void shouldDepositExactAmountWhenAddingBalance() {
                walletService.addBalanceToWallet(USER_ID, AMOUNT);

                verify(wallet, times(1)).deposit(AMOUNT);
            }

            @Test
            void shouldNotWithdrawWhenAddingBalance() {
                walletService.addBalanceToWallet(USER_ID, AMOUNT);

                verify(wallet, never()).withdraw(any(BigDecimal.class));
            }

            @Test
            void shouldNotPublishKafkaEventWhenAddingBalance() {
                walletService.addBalanceToWallet(USER_ID, AMOUNT);

                verifyNoInteractions(kafkaTemplate);
            }

            @Test
            void shouldNotAccessRepositoriesDirectlyWhenAddingBalance() {
                walletService.addBalanceToWallet(USER_ID, AMOUNT);

                verifyNoInteractions(walletRepository);
                verifyNoInteractions(fundReservationRepository);
            }
        }
    }

    @Nested
    class RemoveBalanceFromWallet {

        private static final BigDecimal AMOUNT = BigDecimal.valueOf(30);

        @Test
        void shouldThrowExceptionWhenWalletDoesNotExist() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenThrow(new RequestedEntityNotFoundException("Wallet not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> walletService.removeBalanceFromWallet(USER_ID, AMOUNT));

            verifyNoInteractions(wallet);
            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        void shouldThrowExceptionWhenWithdrawFails() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getBalance()).thenReturn(BigDecimal.TEN);
            doThrow(new IllegalStateException("Insufficient funds")).when(wallet).withdraw(AMOUNT);

            assertThrows(IllegalStateException.class, () -> walletService.removeBalanceFromWallet(USER_ID, AMOUNT));

            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        void shouldThrowExceptionWhenKafkaPublishingFails() {
            when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
            when(wallet.getBalance()).thenReturn(BigDecimal.valueOf(100), BigDecimal.valueOf(70));
            doThrow(new IllegalStateException("Kafka unavailable")).when(kafkaTemplate).send(eq(BALANCE_CHANGED_TOPIC), any(WalletBalanceChangedEvent.class));

            assertThrows(IllegalStateException.class, () -> walletService.removeBalanceFromWallet(USER_ID, AMOUNT));

            verify(wallet).withdraw(AMOUNT);
        }

        @Nested
        class WhenWithdrawalSucceeds {

            @BeforeEach
            void setUp() {
                when(walletManagerService.getWalletForUpdateOrThrow(USER_ID)).thenReturn(wallet);
                when(wallet.getId()).thenReturn(WALLET_ID);
                when(wallet.getBalance()).thenReturn(BigDecimal.valueOf(100), BigDecimal.valueOf(70));
                when(wallet.getReservedAmount()).thenReturn(BigDecimal.valueOf(20));
            }

            @Test
            void shouldReturnWalletDtoWithUpdatedBalanceWhenSufficientFunds() {
                WalletDto result = walletService.removeBalanceFromWallet(USER_ID, AMOUNT);

                assertThat(result).isEqualTo(new WalletDto(WALLET_ID, USER_ID, BigDecimal.valueOf(70), BigDecimal.valueOf(20), BigDecimal.valueOf(50)));
            }

            @Test
            void shouldFetchWalletForUpdateWhenRemovingBalance() {
                walletService.removeBalanceFromWallet(USER_ID, AMOUNT);

                verify(walletManagerService).getWalletForUpdateOrThrow(USER_ID);
            }

            @Test
            void shouldWithdrawExactAmountWhenRemovingBalance() {
                walletService.removeBalanceFromWallet(USER_ID, AMOUNT);

                verify(wallet, times(1)).withdraw(AMOUNT);
            }

            @Test
            void shouldNotDepositWhenRemovingBalance() {
                walletService.removeBalanceFromWallet(USER_ID, AMOUNT);

                verify(wallet, never()).deposit(any(BigDecimal.class));
            }

            @Test
            void shouldPublishWalletBalanceChangedEventWhenWithdrawing() {
                walletService.removeBalanceFromWallet(USER_ID, AMOUNT);

                verify(kafkaTemplate, times(1)).send(eq(BALANCE_CHANGED_TOPIC), any(WalletBalanceChangedEvent.class));
            }

            @Test
            void shouldPublishEventAfterWithdrawingWhenRemovingBalance() {
                walletService.removeBalanceFromWallet(USER_ID, AMOUNT);

                InOrder inOrder = inOrder(wallet, kafkaTemplate);
                inOrder.verify(wallet).withdraw(AMOUNT);
                inOrder.verify(kafkaTemplate).send(eq(BALANCE_CHANGED_TOPIC), any(WalletBalanceChangedEvent.class));
            }

            @Test
            void shouldNotAccessRepositoriesDirectlyWhenRemovingBalance() {
                walletService.removeBalanceFromWallet(USER_ID, AMOUNT);

                verifyNoInteractions(walletRepository);
                verifyNoInteractions(fundReservationRepository);
            }
        }
    }

    @Nested
    class GetWalletWithReservations {

        @Test
        void shouldReturnWalletResponseWithReservationsWhenWalletExists() {
            FundReservation reservation = mock(FundReservation.class);
            when(reservation.getId()).thenReturn(RESERVATION_ID);
            when(reservation.getCategory()).thenReturn(ExpenseCategory.SAVINGS);
            when(reservation.getAmount()).thenReturn(BigDecimal.valueOf(50));
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(wallet.getBalance()).thenReturn(BigDecimal.valueOf(200));
            when(wallet.getReservedAmount()).thenReturn(BigDecimal.valueOf(50));
            when(fundReservationRepository.findByWalletId(WALLET_ID)).thenReturn(List.of(reservation));

            WalletResponse result = walletService.getWalletWithReservations(USER_ID);

            WalletResponse expected = new WalletResponse(WALLET_ID, USER_ID, BigDecimal.valueOf(200), BigDecimal.valueOf(50), BigDecimal.valueOf(150),
                    List.of(new FundReservationDto(RESERVATION_ID, ExpenseCategory.SAVINGS, BigDecimal.valueOf(50), null)));
            assertThat(result).isEqualTo(expected);
        }

        @Test
        void shouldMapAllReservationsInOrderWhenWalletHasMultipleReservations() {
            FundReservation first = mock(FundReservation.class);
            FundReservation second = mock(FundReservation.class);
            when(first.getId()).thenReturn(RESERVATION_ID);
            when(first.getCategory()).thenReturn(ExpenseCategory.SAVINGS);
            when(first.getAmount()).thenReturn(BigDecimal.valueOf(30));
            when(second.getId()).thenReturn(OTHER_RESERVATION_ID);
            when(second.getCategory()).thenReturn(ExpenseCategory.FOOD);
            when(second.getAmount()).thenReturn(BigDecimal.valueOf(20));
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(wallet.getBalance()).thenReturn(BigDecimal.valueOf(200));
            when(wallet.getReservedAmount()).thenReturn(BigDecimal.valueOf(50));
            when(fundReservationRepository.findByWalletId(WALLET_ID)).thenReturn(List.of(first, second));

            WalletResponse result = walletService.getWalletWithReservations(USER_ID);

            assertThat(result.reservations()).containsExactly(
                    new FundReservationDto(RESERVATION_ID, ExpenseCategory.SAVINGS, BigDecimal.valueOf(30), null),
                    new FundReservationDto(OTHER_RESERVATION_ID, ExpenseCategory.FOOD, BigDecimal.valueOf(20), null));
        }

        @Test
        void shouldReturnEmptyReservationsWhenWalletHasNoReservations() {
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(wallet.getBalance()).thenReturn(BigDecimal.TEN);
            when(wallet.getReservedAmount()).thenReturn(BigDecimal.ZERO);
            when(fundReservationRepository.findByWalletId(WALLET_ID)).thenReturn(List.of());

            WalletResponse result = walletService.getWalletWithReservations(USER_ID);

            assertThat(result.reservations()).isEmpty();
        }

        @Test
        void shouldCalculateAvailableAmountAsBalanceMinusReservedWhenWalletExists() {
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(wallet.getBalance()).thenReturn(BigDecimal.valueOf(500));
            when(wallet.getReservedAmount()).thenReturn(BigDecimal.valueOf(120));
            when(fundReservationRepository.findByWalletId(WALLET_ID)).thenReturn(List.of());

            WalletResponse result = walletService.getWalletWithReservations(USER_ID);

            assertThat(result.availableAmount()).isEqualByComparingTo(BigDecimal.valueOf(380));
        }

        @Test
        void shouldReturnZeroAvailableAmountWhenWholeBalanceIsReserved() {
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(wallet.getBalance()).thenReturn(BigDecimal.valueOf(100));
            when(wallet.getReservedAmount()).thenReturn(BigDecimal.valueOf(100));
            when(fundReservationRepository.findByWalletId(WALLET_ID)).thenReturn(List.of());

            WalletResponse result = walletService.getWalletWithReservations(USER_ID);

            assertThat(result.availableAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldNotSaveWalletWhenWalletAlreadyExists() {
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(wallet.getBalance()).thenReturn(BigDecimal.TEN);
            when(wallet.getReservedAmount()).thenReturn(BigDecimal.ZERO);
            when(fundReservationRepository.findByWalletId(WALLET_ID)).thenReturn(List.of());

            walletService.getWalletWithReservations(USER_ID);

            verify(walletRepository, never()).save(any(Wallet.class));
        }

        @Test
        void shouldQueryReservationsByWalletIdWhenWalletExists() {
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(wallet.getBalance()).thenReturn(BigDecimal.TEN);
            when(wallet.getReservedAmount()).thenReturn(BigDecimal.ZERO);
            when(fundReservationRepository.findByWalletId(WALLET_ID)).thenReturn(List.of());

            walletService.getWalletWithReservations(USER_ID);

            verify(fundReservationRepository).findByWalletId(WALLET_ID);
        }

        @Test
        void shouldCreateAndSaveNewWalletWhenWalletDoesNotExist() {
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(wallet.getBalance()).thenReturn(BigDecimal.ZERO);
            when(wallet.getReservedAmount()).thenReturn(BigDecimal.ZERO);
            when(fundReservationRepository.findByWalletId(WALLET_ID)).thenReturn(List.of());

            walletService.getWalletWithReservations(USER_ID);

            verify(walletRepository, times(1)).save(any(Wallet.class));
        }

        @Test
        void shouldReturnEmptyWalletResponseWhenWalletDoesNotExist() {
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
            when(wallet.getId()).thenReturn(WALLET_ID);
            when(wallet.getBalance()).thenReturn(BigDecimal.ZERO);
            when(wallet.getReservedAmount()).thenReturn(BigDecimal.ZERO);
            when(fundReservationRepository.findByWalletId(WALLET_ID)).thenReturn(List.of());

            WalletResponse result = walletService.getWalletWithReservations(USER_ID);

            assertThat(result).isEqualTo(new WalletResponse(WALLET_ID, USER_ID, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of()));
        }

        @Test
        void shouldThrowExceptionWhenWalletRepositoryLookupFails() {
            when(walletRepository.findByUserId(USER_ID)).thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> walletService.getWalletWithReservations(USER_ID));

            verifyNoInteractions(fundReservationRepository);
        }

        @Test
        void shouldThrowExceptionWhenSavingNewWalletFails() {
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(walletRepository.save(any(Wallet.class))).thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> walletService.getWalletWithReservations(USER_ID));

            verifyNoInteractions(fundReservationRepository);
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteWalletByUserIdWhenInvoked() {
            walletService.deleteByUserId(USER_ID);

            verify(walletRepository, times(1)).deleteByUserId(USER_ID);
        }

        @Test
        void shouldNotInteractWithOtherDependenciesWhenDeletingWallet() {
            walletService.deleteByUserId(USER_ID);

            verifyNoInteractions(walletManagerService);
            verifyNoInteractions(fundReservationRepository);
            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryDeleteFails() {
            doThrow(new IllegalStateException("db failed")).when(walletRepository).deleteByUserId(USER_ID);

            assertThrows(IllegalStateException.class, () -> walletService.deleteByUserId(USER_ID));
        }
    }
}