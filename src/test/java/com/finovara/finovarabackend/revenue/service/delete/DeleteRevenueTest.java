package com.finovara.finovarabackend.revenue.service.delete;

import com.finovara.finovarabackend.accountactivity.revenue.model.RevenueActivityType;
import com.finovara.finovarabackend.accountactivity.revenue.service.RevenueActivityService;
import com.finovara.finovarabackend.revenue.exception.notfound.RevenueNotFoundException;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.revenue.service.RevenueService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.service.AutoPaymentsService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import com.finovara.finovarabackend.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteRevenueTest {
    @Mock
    private UserManagerService userManagerService;
    @Mock
    private RevenueRepository revenueRepository;
    @Mock
    private WalletService walletService;
    @Mock
    private AutoPaymentsService autoPaymentsService;
    @Mock
    private RevenueActivityService revenueActivityService;

    @InjectMocks
    private RevenueService revenueService;

    @Test
    void shouldDeleteRevenueSuccessfully() {
        Long userId = 1L;

        User user = new User();
        user.setId(userId);

        Revenue revenue = new Revenue();
        revenue.setId(1L);
        revenue.setUserAssigned(user);
        revenue.setAmount(new BigDecimal("100"));

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(revenueRepository.findByIdAndUserAssignedId(revenue.getId(), user.getId())).thenReturn(Optional.of(revenue));

        revenueService.deleteRevenue(revenue.getId(), userId);

        InOrder inOrder = inOrder(autoPaymentsService, walletService, revenueActivityService, revenueRepository);
        inOrder.verify(autoPaymentsService).handleRevenuePiggyBankAutomation(userId, new BigDecimal("100"), PiggyBankAutomationMode.ROLLBACK);
        inOrder.verify(walletService).removeBalanceFromWallet(userId, new BigDecimal("100"));
        inOrder.verify(revenueActivityService).createRevenueActivity(userId, RevenueActivityType.DELETED_REVENUE, revenue);
        inOrder.verify(revenueRepository).delete(revenue);

        verify(userManagerService).getUserByIdOrThrow(userId);
        verify(revenueRepository).findByIdAndUserAssignedId(revenue.getId(), user.getId());
        verifyNoMoreInteractions(autoPaymentsService, walletService, revenueActivityService, revenueRepository);
    }

    @Test
    void shouldThrowWhenRevenueDoesNotExist() {
        Long userId = 1L;

        User user = new User();
        user.setId(userId);

        Long revenueId = 1L;

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(revenueRepository.findByIdAndUserAssignedId(revenueId, user.getId())).thenReturn(Optional.empty());

        assertThrows(RevenueNotFoundException.class, () -> revenueService.deleteRevenue(revenueId, userId));

        verify(userManagerService).getUserByIdOrThrow(userId);
        verify(revenueRepository).findByIdAndUserAssignedId(revenueId, user.getId());
        verify(revenueRepository, never()).delete(any());
        verifyNoInteractions(autoPaymentsService, walletService, revenueActivityService);

    }

}
