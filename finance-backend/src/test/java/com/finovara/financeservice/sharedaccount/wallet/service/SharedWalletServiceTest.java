package com.finovara.financeservice.sharedaccount.service.wallet;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.financeservice.sharedaccount.wallet.dto.SharedWalletDto;
import com.finovara.financeservice.sharedaccount.wallet.model.SharedWallet;
import com.finovara.financeservice.sharedaccount.wallet.repository.SharedWalletRepository;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedWalletServiceTest {

    @Mock
    private SharedWalletRepository sharedWalletRepository;

    @Mock
    private SharedWallet sharedWallet;

    private SharedWalletService sharedWalletService;

    @BeforeEach
    void setUp() {
        sharedWalletService = new SharedWalletService(sharedWalletRepository);
    }

    @Nested
    class CreateSharedWallet {

        @Test
        void shouldReturnExistingWalletWhenSharedWalletAlreadyExists() {
            Long ownerId = 1L;
            Long memberId = 2L;
            when(sharedWalletRepository.findByOwnerIdOrMemberId(ownerId, ownerId)).thenReturn(Optional.of(sharedWallet));
            when(sharedWallet.getId()).thenReturn(10L);
            when(sharedWallet.getOwnerId()).thenReturn(ownerId);
            when(sharedWallet.getMemberId()).thenReturn(memberId);
            when(sharedWallet.getBalance()).thenReturn(BigDecimal.ZERO);

            SharedWalletDto result = sharedWalletService.createSharedWallet(ownerId, memberId);

            assertThat(result.id()).isEqualTo(10L);
            assertThat(result.ownerId()).isEqualTo(ownerId);
            assertThat(result.memberId()).isEqualTo(memberId);
            assertThat(result.balance()).isEqualTo(BigDecimal.ZERO);
            verify(sharedWalletRepository, never()).save(any());
        }

        @Test
        void shouldCreateNewWalletWhenSharedWalletDoesNotExist() {
            Long ownerId = 1L;
            Long memberId = 2L;
            when(sharedWalletRepository.findByOwnerIdOrMemberId(ownerId, ownerId)).thenReturn(Optional.empty());
            when(sharedWalletRepository.save(any(SharedWallet.class))).thenReturn(sharedWallet);
            when(sharedWallet.getId()).thenReturn(5L);
            when(sharedWallet.getOwnerId()).thenReturn(ownerId);
            when(sharedWallet.getMemberId()).thenReturn(memberId);
            when(sharedWallet.getBalance()).thenReturn(BigDecimal.ZERO);

            SharedWalletDto result = sharedWalletService.createSharedWallet(ownerId, memberId);

            assertThat(result.id()).isEqualTo(5L);
            assertThat(result.ownerId()).isEqualTo(ownerId);
            assertThat(result.memberId()).isEqualTo(memberId);
            verify(sharedWalletRepository).save(any(SharedWallet.class));
        }
    }

    @Nested
    class AddBalanceToWallet {

        @Test
        void shouldAddBalanceToWalletWhenWalletExists() {
            Long callerId = 1L;
            BigDecimal amount = BigDecimal.TEN;
            when(sharedWalletRepository.findByOwnerIdOrMemberId(callerId, callerId)).thenReturn(Optional.of(sharedWallet));
            when(sharedWallet.getId()).thenReturn(1L);
            when(sharedWallet.getOwnerId()).thenReturn(callerId);
            when(sharedWallet.getMemberId()).thenReturn(2L);
            when(sharedWallet.getBalance()).thenReturn(BigDecimal.TEN);

            SharedWalletDto result = sharedWalletService.addBalanceToWallet(callerId, amount);

            verify(sharedWallet).deposit(amount);
            assertThat(result.balance()).isEqualTo(BigDecimal.TEN);
        }

        @Test
        void shouldThrowExceptionWhenWalletNotFoundWhileAddingBalance() {
            Long callerId = 1L;
            BigDecimal amount = BigDecimal.TEN;
            when(sharedWalletRepository.findByOwnerIdOrMemberId(callerId, callerId)).thenReturn(Optional.empty());

            assertThrows(InvalidInputException.class, () -> sharedWalletService.addBalanceToWallet(callerId, amount));
            verify(sharedWallet, never()).deposit(any());
        }
    }

    @Nested
    class RemoveBalanceFromWallet {

        @Test
        void shouldRemoveBalanceFromWalletWhenWalletExists() {
            Long callerId = 1L;
            BigDecimal amount = BigDecimal.TEN;
            when(sharedWalletRepository.findByOwnerIdOrMemberId(callerId, callerId)).thenReturn(Optional.of(sharedWallet));
            when(sharedWallet.getId()).thenReturn(1L);
            when(sharedWallet.getOwnerId()).thenReturn(callerId);
            when(sharedWallet.getMemberId()).thenReturn(2L);
            when(sharedWallet.getBalance()).thenReturn(BigDecimal.ZERO);

            SharedWalletDto result = sharedWalletService.removeBalanceFromWallet(callerId, amount);

            verify(sharedWallet).withdraw(amount);
            assertThat(result.balance()).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        void shouldThrowExceptionWhenWalletNotFoundWhileRemovingBalance() {
            Long callerId = 1L;
            BigDecimal amount = BigDecimal.TEN;
            when(sharedWalletRepository.findByOwnerIdOrMemberId(callerId, callerId)).thenReturn(Optional.empty());

            assertThrows(InvalidInputException.class, () -> sharedWalletService.removeBalanceFromWallet(callerId, amount));
            verify(sharedWallet, never()).withdraw(any());
        }
    }

    @Nested
    class GetWallet {

        @Test
        void shouldReturnWalletWhenWalletExists() {
            Long callerId = 1L;
            when(sharedWalletRepository.findByOwnerIdOrMemberId(callerId, callerId)).thenReturn(Optional.of(sharedWallet));
            when(sharedWallet.getId()).thenReturn(1L);
            when(sharedWallet.getOwnerId()).thenReturn(callerId);
            when(sharedWallet.getMemberId()).thenReturn(2L);
            when(sharedWallet.getBalance()).thenReturn(BigDecimal.TEN);

            SharedWalletDto result = sharedWalletService.getWallet(callerId);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.ownerId()).isEqualTo(callerId);
            assertThat(result.memberId()).isEqualTo(2L);
            assertThat(result.balance()).isEqualTo(BigDecimal.TEN);
        }

        @Test
        void shouldThrowExceptionWhenWalletNotFound() {
            Long callerId = 1L;
            when(sharedWalletRepository.findByOwnerIdOrMemberId(callerId, callerId)).thenReturn(Optional.empty());

            assertThrows(InvalidInputException.class, () -> sharedWalletService.getWallet(callerId));
        }
    }
}