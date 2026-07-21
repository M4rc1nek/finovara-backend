package com.finovara.financeservice.sharedaccount.settings.expense.spendcontrol.service;

import com.finovara.contracts.exception.unprocessablecontent.InvalidOperationException;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettings;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettingsRepository;
import com.finovara.financeservice.sharedaccount.settings.expense.spendcontrol.dto.SpendControlDto;
import com.finovara.financeservice.sharedaccount.wallet.model.SharedWallet;
import com.finovara.financeservice.sharedaccount.wallet.repository.SharedWalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpendControlServiceTest {

    @Mock
    private SharedAccountSettingsRepository sharedAccountSettingsRepository;

    @Mock
    private SharedWalletRepository sharedWalletRepository;

    @InjectMocks
    private SpendControlService spendControlService;

    private SharedAccountSettings sharedAccountSettings;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        sharedAccountSettings = new SharedAccountSettings();
        when(sharedAccountSettingsRepository.findByUserId(USER_ID)).thenReturn(sharedAccountSettings);
    }

    private SharedWallet walletWithBalance(BigDecimal balance) {
        SharedWallet wallet = SharedWallet.create(1L, 2L);
        wallet.deposit(balance);
        return wallet;
    }

    @Nested
    class SaveSpendControlService {

        @Test
        void shouldEnableSpendControlWithGivenPercentage() {
            SpendControlDto dto = new SpendControlDto(true, new BigDecimal("30"));

            spendControlService.saveSpendControlService(USER_ID, dto);

            assertTrue(sharedAccountSettings.isSpendControlEnabled());
            assertEquals(new BigDecimal("30"), sharedAccountSettings.getSpendControlPercentage());
        }

        @Test
        void shouldDisableSpendControl() {
            SpendControlDto dto = new SpendControlDto(false, new BigDecimal("30"));

            spendControlService.saveSpendControlService(USER_ID, dto);

            assertFalse(sharedAccountSettings.isSpendControlEnabled());
        }
    }

    @Nested
    class GetSpendControl {

        @Test
        void shouldReturnCurrentSpendControlSettings() {
            sharedAccountSettings.setSpendControlEnabled(true);
            sharedAccountSettings.setSpendControlPercentage(new BigDecimal("40"));

            SpendControlDto result = spendControlService.getSmartScan(USER_ID);

            assertTrue(result.spendControlEnabled());
            assertEquals(new BigDecimal("40"), result.spendControlPercentage());
        }

        @Test
        void shouldReturnDisabledSpendControlSettings() {
            sharedAccountSettings.setSpendControlEnabled(false);
            sharedAccountSettings.setSpendControlPercentage(BigDecimal.ZERO);

            SpendControlDto result = spendControlService.getSmartScan(USER_ID);

            assertFalse(result.spendControlEnabled());
        }
    }

    @Nested
    class HandleSpendControl {

        @Test
        void shouldDoNothingWhenSpendControlDisabled() {
            sharedAccountSettings.setSpendControlEnabled(false);

            spendControlService.handleSpendControl(USER_ID, BigDecimal.valueOf(100));

            verifyNoInteractions(sharedWalletRepository);
        }

        @Test
        void shouldNotThrowWhenExpenseWithinAllowedLimit() {
            sharedAccountSettings.setSpendControlEnabled(true);
            sharedAccountSettings.setSpendControlPercentage(new BigDecimal("50"));

            when(sharedWalletRepository.findByUserId(USER_ID)).thenReturn(walletWithBalance(new BigDecimal("1000")));

            assertDoesNotThrow(() -> spendControlService.handleSpendControl(USER_ID, new BigDecimal("400")));
        }

        @Test
        void shouldNotThrowWhenExpenseEqualsAllowedLimit() {
            sharedAccountSettings.setSpendControlEnabled(true);
            sharedAccountSettings.setSpendControlPercentage(new BigDecimal("50"));

            when(sharedWalletRepository.findByUserId(USER_ID)).thenReturn(walletWithBalance(new BigDecimal("1000")));

            assertDoesNotThrow(() -> spendControlService.handleSpendControl(USER_ID, new BigDecimal("500")));
        }

        @Test
        void shouldThrowExceptionWhenExpenseExceedsAllowedLimit() {
            sharedAccountSettings.setSpendControlEnabled(true);
            sharedAccountSettings.setSpendControlPercentage(new BigDecimal("50"));

            when(sharedWalletRepository.findByUserId(USER_ID)).thenReturn(walletWithBalance(new BigDecimal("1000")));

            BigDecimal expenseAmount = new BigDecimal("600");

            assertThrows(InvalidOperationException.class,
                    () -> spendControlService.handleSpendControl(USER_ID, expenseAmount));
        }

        @Test
        void shouldNotThrowExceptionWhenPercentageIsFullBalance() {
            sharedAccountSettings.setSpendControlEnabled(true);
            sharedAccountSettings.setSpendControlPercentage(new BigDecimal("100"));

            when(sharedWalletRepository.findByUserId(USER_ID)).thenReturn(walletWithBalance(new BigDecimal("1000")));

            assertDoesNotThrow(() -> spendControlService.handleSpendControl(USER_ID, new BigDecimal("1000")));
        }
    }
}