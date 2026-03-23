package com.finovara.finovarabackend.usersetting.piggybank.autopayments.service.handle;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.AutoPaymentsMode;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.service.AutoPaymentsService;
import com.finovara.finovarabackend.usersetting.piggybank.completion.service.GoalCompletionService;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import com.finovara.finovarabackend.util.service.wallet.WalletManagerService;
import com.finovara.finovarabackend.wallet.model.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandleAutoPaymentsTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private WalletManagerService walletManagerService;

    @Mock
    private PiggyBankActivityService piggyBankActivityService;
    @Mock
    private GoalCompletionService goalCompletionService;

    @InjectMocks
    private AutoPaymentsService autoPaymentsService;

    private User user;
    private Wallet wallet;
    private PiggyBank piggyBank;

    private final String EMAIL = "test@test.com";

    @BeforeEach
    void setup() {
        user = new User();
        wallet = new Wallet();
        wallet.setBalance(BigDecimal.valueOf(500));
        user.setPiggyBanks(List.of());

        piggyBank = new PiggyBank();
        PiggyBankSettings settings = new PiggyBankSettings();
        piggyBank.setSettings(settings);
        piggyBank.setAmount(BigDecimal.valueOf(100));

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
        when(walletManagerService.getWalletByUserEmailOrThrow(EMAIL)).thenReturn(wallet);
    }

    @Test
    void shouldDoNothingWhenNoPiggyBanks() {
        user.setPiggyBanks(List.of());

        autoPaymentsService.handleRevenuePiggyBankAutomation(EMAIL, BigDecimal.valueOf(100), AutoPaymentsMode.APPLY);

        assertThat(BigDecimal.valueOf(100)).isEqualByComparingTo(piggyBank.getAmount());
    }

    @Test
    void shouldDoNothingWhenAutomationInactive() {
        PiggyBank inactivePiggyBank = new PiggyBank();
        PiggyBankSettings settings = new PiggyBankSettings();
        settings.setAutomationActive(false);
        inactivePiggyBank.setSettings(settings);
        inactivePiggyBank.setAmount(BigDecimal.valueOf(100));
        user.setPiggyBanks(List.of(inactivePiggyBank));

        autoPaymentsService.handleRevenuePiggyBankAutomation(EMAIL, BigDecimal.valueOf(100), AutoPaymentsMode.APPLY);

        assertThat(BigDecimal.valueOf(100)).isEqualByComparingTo(piggyBank.getAmount());
    }

    @Test
    void shouldApplyAutomationPayment() {
        piggyBank.getSettings().setAutomationActive(true);
        piggyBank.getSettings().setAutomationPercentage(BigDecimal.valueOf(50));
        user.setPiggyBanks(List.of(piggyBank));

        autoPaymentsService.handleRevenuePiggyBankAutomation(EMAIL, BigDecimal.valueOf(200), AutoPaymentsMode.APPLY);

        assertThat(piggyBank.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(400));
        verify(piggyBankActivityService).createPaymentPiggyBankActivity(eq(EMAIL), eq(piggyBank),
                eq(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING), argThat(amount -> amount.compareTo(BigDecimal.valueOf(100)) == 0));
    }

    @Test
    void shouldRollbackAutomationPayment() {
        piggyBank.getSettings().setAutomationActive(true);
        piggyBank.getSettings().setAutomationPercentage(BigDecimal.valueOf(50));
        user.setPiggyBanks(List.of(piggyBank));

        autoPaymentsService.handleRevenuePiggyBankAutomation(EMAIL, BigDecimal.valueOf(200), AutoPaymentsMode.ROLLBACK);

        assertThat(piggyBank.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(600));
        verify(piggyBankActivityService).createPaymentPiggyBankActivity(eq(EMAIL), eq(piggyBank),
                eq(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING), argThat(amount -> amount.compareTo(BigDecimal.valueOf(100)) == 0));
    }
}