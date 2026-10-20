package com.finovara.financeservice.sharedaccount.wallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.financeservice.sharedaccount.wallet.dto.SharedWalletDto;
import com.finovara.financeservice.sharedaccount.wallet.model.SharedWallet;
import com.finovara.financeservice.sharedaccount.wallet.repository.SharedWalletRepository;
import com.finovara.financeservice.util.sharedaccount.wallet.SharedWalletManager;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SharedWalletServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long OTHER_MEMBER_ID = 3L;
    private static final Long CALLER_ID = 1L;
    private static final Long WALLET_ID = 10L;
    private static final BigDecimal BALANCE = BigDecimal.valueOf(150);
    private static final BigDecimal AMOUNT = BigDecimal.TEN;

    @Mock
    private SharedWalletRepository sharedWalletRepository;

    @Mock
    private SharedWalletManager sharedWalletManager;

    @Mock
    private SharedWallet sharedWallet;

    @Captor
    private ArgumentCaptor<SharedWallet> walletCaptor;

    @InjectMocks
    private SharedWalletService sharedWalletService;

    private SharedWalletDto expectedDto;

    @BeforeEach
    void setUp() {
        expectedDto = new SharedWalletDto(WALLET_ID, OWNER_ID, MEMBER_ID, BALANCE);
    }

    private void stubWalletData() {
        when(sharedWallet.getId()).thenReturn(WALLET_ID);
        when(sharedWallet.getOwnerId()).thenReturn(OWNER_ID);
        when(sharedWallet.getMemberId()).thenReturn(MEMBER_ID);
        when(sharedWallet.getBalance()).thenReturn(BALANCE);
    }

    @Nested
    class CreateSharedWallet {

        @Nested
        class ExistingWallet {

            @BeforeEach
            void setUp() {
                when(sharedWalletRepository.findByOwnerIdOrMemberId(OWNER_ID, OWNER_ID)).thenReturn(Optional.of(sharedWallet));
                stubWalletData();
            }

            @Test
            void shouldReturnExistingWalletDtoWhenSharedWalletAlreadyExists() {
                SharedWalletDto result = sharedWalletService.createSharedWallet(OWNER_ID, MEMBER_ID);

                assertEquals(expectedDto, result);
            }

            @Test
            void shouldNotSaveWalletWhenSharedWalletAlreadyExists() {
                sharedWalletService.createSharedWallet(OWNER_ID, MEMBER_ID);

                verify(sharedWalletRepository, never()).save(any(SharedWallet.class));
            }

            @Test
            void shouldLookUpWalletByOwnerIdWhenCheckingExistence() {
                sharedWalletService.createSharedWallet(OWNER_ID, MEMBER_ID);

                verify(sharedWalletRepository).findByOwnerIdOrMemberId(OWNER_ID, OWNER_ID);
            }
        }

        @Nested
        class NewWallet {

            @BeforeEach
            void setUp() {
                when(sharedWalletRepository.findByOwnerIdOrMemberId(OWNER_ID, OWNER_ID)).thenReturn(Optional.empty());
                when(sharedWalletRepository.save(any(SharedWallet.class))).thenReturn(sharedWallet);
                stubWalletData();
            }

            @Test
            void shouldReturnDtoOfSavedWalletWhenSharedWalletDoesNotExist() {
                SharedWalletDto result = sharedWalletService.createSharedWallet(OWNER_ID, MEMBER_ID);

                assertEquals(expectedDto, result);
            }

            @Test
            void shouldSaveWalletWithOwnerAndMemberWhenSharedWalletDoesNotExist() {
                sharedWalletService.createSharedWallet(OWNER_ID, MEMBER_ID);

                verify(sharedWalletRepository).save(walletCaptor.capture());
                SharedWallet captured = walletCaptor.getValue();
                assertEquals(OWNER_ID, captured.getOwnerId());
                assertEquals(MEMBER_ID, captured.getMemberId());
            }

            @Test
            void shouldLookUpExistingWalletBeforeSavingWhenSharedWalletDoesNotExist() {
                sharedWalletService.createSharedWallet(OWNER_ID, MEMBER_ID);

                InOrder inOrder = inOrder(sharedWalletRepository);
                inOrder.verify(sharedWalletRepository).findByOwnerIdOrMemberId(OWNER_ID, OWNER_ID);
                inOrder.verify(sharedWalletRepository).save(any(SharedWallet.class));
            }
        }

        @Nested
        class Failure {

            @Test
            void shouldThrowExceptionWhenRepositoryLookupFails() {
                when(sharedWalletRepository.findByOwnerIdOrMemberId(OWNER_ID, OWNER_ID))
                        .thenThrow(new IllegalStateException("db failed"));

                assertThrows(IllegalStateException.class, () -> sharedWalletService.createSharedWallet(OWNER_ID, MEMBER_ID));

                verify(sharedWalletRepository, never()).save(any(SharedWallet.class));
            }

            @Test
            void shouldThrowExceptionWhenRepositorySaveFails() {
                when(sharedWalletRepository.findByOwnerIdOrMemberId(OWNER_ID, OWNER_ID)).thenReturn(Optional.empty());
                when(sharedWalletRepository.save(any(SharedWallet.class))).thenThrow(new IllegalStateException("db failed"));

                assertThrows(IllegalStateException.class, () -> sharedWalletService.createSharedWallet(OWNER_ID, MEMBER_ID));
            }

            @Test
            void shouldThrowExceptionWhenRepositoryReturnsNullAfterSave() {
                when(sharedWalletRepository.findByOwnerIdOrMemberId(OWNER_ID, OWNER_ID)).thenReturn(Optional.empty());

                assertThrows(NullPointerException.class, () -> sharedWalletService.createSharedWallet(OWNER_ID, MEMBER_ID));
            }
        }

        @Test
        void shouldReturnExistingWalletDataWhenRequestedMemberDiffersFromExistingMember() {
            when(sharedWalletRepository.findByOwnerIdOrMemberId(OWNER_ID, OWNER_ID)).thenReturn(Optional.of(sharedWallet));
            stubWalletData();

            SharedWalletDto result = sharedWalletService.createSharedWallet(OWNER_ID, OTHER_MEMBER_ID);

            assertEquals(MEMBER_ID, result.memberId());
            verify(sharedWalletRepository, never()).save(any(SharedWallet.class));
        }
    }

    @Nested
    class AddBalanceToWallet {

        @Nested
        class Success {

            @BeforeEach
            void setUp() {
                when(sharedWalletManager.getWalletForUpdateOrThrow(CALLER_ID)).thenReturn(sharedWallet);
                stubWalletData();
            }

            @Test
            void shouldReturnWalletDtoWhenBalanceIsAdded() {
                SharedWalletDto result = sharedWalletService.addBalanceToWallet(CALLER_ID, AMOUNT);

                assertEquals(expectedDto, result);
            }

            @Test
            void shouldDepositAmountWhenBalanceIsAdded() {
                sharedWalletService.addBalanceToWallet(CALLER_ID, AMOUNT);

                verify(sharedWallet).deposit(AMOUNT);
                verify(sharedWallet, never()).withdraw(any(BigDecimal.class));
            }

            @Test
            void shouldLockWalletForUpdateWhenBalanceIsAdded() {
                sharedWalletService.addBalanceToWallet(CALLER_ID, AMOUNT);

                verify(sharedWalletManager).getWalletForUpdateOrThrow(CALLER_ID);
                verify(sharedWalletManager, never()).getWalletOrThrow(any());
            }

            @Test
            void shouldDepositBeforeReadingBalanceWhenBalanceIsAdded() {
                sharedWalletService.addBalanceToWallet(CALLER_ID, AMOUNT);

                InOrder inOrder = inOrder(sharedWallet);
                inOrder.verify(sharedWallet).deposit(AMOUNT);
                inOrder.verify(sharedWallet).getBalance();
            }

            @Test
            void shouldNotUseRepositoryWhenBalanceIsAdded() {
                sharedWalletService.addBalanceToWallet(CALLER_ID, AMOUNT);

                verifyNoInteractions(sharedWalletRepository);
            }
        }

        @Nested
        class Failure {

            @Test
            void shouldThrowExceptionWhenWalletDoesNotExist() {
                when(sharedWalletManager.getWalletForUpdateOrThrow(CALLER_ID))
                        .thenThrow(new InvalidInputException("Wallet not found"));

                assertThrows(InvalidInputException.class, () -> sharedWalletService.addBalanceToWallet(CALLER_ID, AMOUNT));

                verifyNoInteractions(sharedWallet, sharedWalletRepository);
            }

            @Test
            void shouldThrowExceptionWhenManagerReturnsNull() {
                assertThrows(NullPointerException.class, () -> sharedWalletService.addBalanceToWallet(CALLER_ID, AMOUNT));

                verifyNoInteractions(sharedWalletRepository);
            }

            @Test
            void shouldThrowExceptionWhenDepositFails() {
                when(sharedWalletManager.getWalletForUpdateOrThrow(CALLER_ID)).thenReturn(sharedWallet);
                doThrow(new InvalidInputException("Invalid amount")).when(sharedWallet).deposit(AMOUNT);

                assertThrows(InvalidInputException.class, () -> sharedWalletService.addBalanceToWallet(CALLER_ID, AMOUNT));

                verify(sharedWallet, never()).getBalance();
            }
        }
    }

    @Nested
    class RemoveBalanceFromWallet {

        @Nested
        class Success {

            @BeforeEach
            void setUp() {
                when(sharedWalletManager.getWalletForUpdateOrThrow(CALLER_ID)).thenReturn(sharedWallet);
                stubWalletData();
            }

            @Test
            void shouldReturnWalletDtoWhenBalanceIsRemoved() {
                SharedWalletDto result = sharedWalletService.removeBalanceFromWallet(CALLER_ID, AMOUNT);

                assertEquals(expectedDto, result);
            }

            @Test
            void shouldWithdrawAmountWhenBalanceIsRemoved() {
                sharedWalletService.removeBalanceFromWallet(CALLER_ID, AMOUNT);

                verify(sharedWallet).withdraw(AMOUNT);
                verify(sharedWallet, never()).deposit(any(BigDecimal.class));
            }

            @Test
            void shouldLockWalletForUpdateWhenBalanceIsRemoved() {
                sharedWalletService.removeBalanceFromWallet(CALLER_ID, AMOUNT);

                verify(sharedWalletManager).getWalletForUpdateOrThrow(CALLER_ID);
                verify(sharedWalletManager, never()).getWalletOrThrow(any());
            }

            @Test
            void shouldWithdrawBeforeReadingBalanceWhenBalanceIsRemoved() {
                sharedWalletService.removeBalanceFromWallet(CALLER_ID, AMOUNT);

                InOrder inOrder = inOrder(sharedWallet);
                inOrder.verify(sharedWallet).withdraw(AMOUNT);
                inOrder.verify(sharedWallet).getBalance();
            }

            @Test
            void shouldNotUseRepositoryWhenBalanceIsRemoved() {
                sharedWalletService.removeBalanceFromWallet(CALLER_ID, AMOUNT);

                verifyNoInteractions(sharedWalletRepository);
            }
        }

        @Nested
        class Failure {

            @Test
            void shouldThrowExceptionWhenWalletDoesNotExist() {
                when(sharedWalletManager.getWalletForUpdateOrThrow(CALLER_ID))
                        .thenThrow(new InvalidInputException("Wallet not found"));

                assertThrows(InvalidInputException.class, () -> sharedWalletService.removeBalanceFromWallet(CALLER_ID, AMOUNT));

                verifyNoInteractions(sharedWallet, sharedWalletRepository);
            }

            @Test
            void shouldThrowExceptionWhenManagerReturnsNull() {
                assertThrows(NullPointerException.class, () -> sharedWalletService.removeBalanceFromWallet(CALLER_ID, AMOUNT));

                verifyNoInteractions(sharedWalletRepository);
            }

            @Test
            void shouldThrowExceptionWhenFundsAreInsufficient() {
                when(sharedWalletManager.getWalletForUpdateOrThrow(CALLER_ID)).thenReturn(sharedWallet);
                doThrow(new InvalidInputException("Insufficient funds")).when(sharedWallet).withdraw(AMOUNT);

                assertThrows(InvalidInputException.class, () -> sharedWalletService.removeBalanceFromWallet(CALLER_ID, AMOUNT));

                verify(sharedWallet, never()).getBalance();
            }
        }
    }

    @Nested
    class GetWallet {

        @Nested
        class Success {

            @BeforeEach
            void setUp() {
                when(sharedWalletManager.getWalletOrThrow(CALLER_ID)).thenReturn(sharedWallet);
                stubWalletData();
            }

            @Test
            void shouldReturnWalletDtoWhenWalletExists() {
                SharedWalletDto result = sharedWalletService.getWallet(CALLER_ID);

                assertEquals(expectedDto, result);
            }

            @Test
            void shouldNotLockWalletForUpdateWhenWalletIsRead() {
                sharedWalletService.getWallet(CALLER_ID);

                verify(sharedWalletManager).getWalletOrThrow(CALLER_ID);
                verify(sharedWalletManager, never()).getWalletForUpdateOrThrow(any());
            }

            @Test
            void shouldNotModifyBalanceWhenWalletIsRead() {
                sharedWalletService.getWallet(CALLER_ID);

                verify(sharedWallet, never()).deposit(any(BigDecimal.class));
                verify(sharedWallet, never()).withdraw(any(BigDecimal.class));
                verifyNoInteractions(sharedWalletRepository);
            }
        }

        @Nested
        class Failure {

            @Test
            void shouldThrowExceptionWhenWalletDoesNotExist() {
                when(sharedWalletManager.getWalletOrThrow(CALLER_ID)).thenThrow(new InvalidInputException("Wallet not found"));

                assertThrows(InvalidInputException.class, () -> sharedWalletService.getWallet(CALLER_ID));

                verifyNoInteractions(sharedWalletRepository);
            }

            @Test
            void shouldThrowExceptionWhenManagerReturnsNull() {
                assertThrows(NullPointerException.class, () -> sharedWalletService.getWallet(CALLER_ID));
            }
        }

        @Test
        void shouldReturnWalletDtoWhenCallerIsMember() {
            when(sharedWalletManager.getWalletOrThrow(MEMBER_ID)).thenReturn(sharedWallet);
            stubWalletData();

            SharedWalletDto result = sharedWalletService.getWallet(MEMBER_ID);

            assertEquals(expectedDto, result);
        }
    }
}