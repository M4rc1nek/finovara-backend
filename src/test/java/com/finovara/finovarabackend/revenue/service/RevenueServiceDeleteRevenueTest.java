package com.finovara.finovarabackend.revenue.service;

import com.finovara.finovarabackend.accountactivity.revenue.model.RevenueActivityType;
import com.finovara.finovarabackend.accountactivity.revenue.service.RevenueActivityService;
import com.finovara.finovarabackend.revenue.exception.notfound.RevenueNotFoundException;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.revenue.scoring.service.RevenueScoringService;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.AutoPaymentsMode;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.service.AutoPaymentsService;
import com.finovara.finovarabackend.util.service.revenue.RevenueManagerService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import com.finovara.finovarabackend.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RevenueServiceDeleteRevenueTest {
    @Mock
    private UserManagerService userManagerService;
    @Mock
    private RevenueManagerService revenueManagerService;
    @Mock
    private RevenueRepository revenueRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private WalletService walletService;
    @Mock
    private AutoPaymentsService autoPaymentsService;
    @Mock
    private RevenueActivityService revenueActivityService;
    @Mock
    private RevenueScoringService revenueScoringService;

    @InjectMocks
    private RevenueService revenueService;

    @Test
    void shouldDeleteRevenueSuccessfully() {
        String email = "test@mail.com";

        User user = new User();
        user.setId(1L);

        Revenue revenue = new Revenue();
        revenue.setId(1L);
        revenue.setUserAssigned(user);
        revenue.setAmount(new BigDecimal("100"));

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(revenueRepository.findByIdAndUserAssignedId(revenue.getId(), user.getId())).thenReturn(Optional.of(revenue));

        revenueService.deleteRevenue(revenue.getId(), email);

        verify(autoPaymentsService).handleRevenuePiggyBankAutomation(email, new BigDecimal("100"), AutoPaymentsMode.ROLLBACK);
        verify(walletService).removeBalanceFromWallet(email, new BigDecimal("100"));
        verify(revenueActivityService).createRevenueActivity(email, RevenueActivityType.DELETED_REVENUE, revenue);
        revenueRepository.delete(revenue);
        verify(revenueScoringService).recalculateScore(email);
    }

    @Test
    void shouldThrowWhenRevenueDoesNotExist() {
        String email = "test@gmail.com";

        User user = new User();
        user.setId(1L);

        Long revenueId = 1L;

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(revenueRepository.findByIdAndUserAssignedId(revenueId, user.getId())).thenReturn(Optional.empty());

        assertThrows(RevenueNotFoundException.class, () -> revenueService.deleteRevenue(revenueId, email));
    }

}
