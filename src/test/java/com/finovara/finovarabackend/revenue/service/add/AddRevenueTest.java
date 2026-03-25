package com.finovara.finovarabackend.revenue.service.add;

import com.finovara.finovarabackend.accountactivity.revenue.model.RevenueActivityType;
import com.finovara.finovarabackend.accountactivity.revenue.service.RevenueActivityService;
import com.finovara.finovarabackend.revenue.dto.RevenueDTO;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.revenue.service.RevenueService;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.revenue.scoring.service.RevenueScoringService;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.AutoPaymentsMode;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.service.AutoPaymentsService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import com.finovara.finovarabackend.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddRevenueTest {

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
    @Mock
    private RevenueScoringService revenueScoringService;
    @InjectMocks
    private RevenueService revenueService;

    @Test
    void shouldAddRevenueSuccessfully() {
        RevenueDTO dto = new RevenueDTO(null, null, new BigDecimal("100"), RevenueCategory.SALARY, null, "test income");
        String email = "test@test.com";
        User user = new User();

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);

        revenueService.addRevenue(dto, email);

        verify(walletService).addBalanceToWallet(email, dto.amount());
        verify(revenueActivityService).createRevenueActivity(eq(email), eq(RevenueActivityType.ADDED_REVENUE), any(Revenue.class));
        verify(revenueRepository).save(any(Revenue.class));
        verify(revenueScoringService).recalculateScore(email);
        verify(autoPaymentsService).handleRevenuePiggyBankAutomation(email, dto.amount(), AutoPaymentsMode.APPLY);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        RevenueDTO dto = new RevenueDTO(null, null, new BigDecimal("100"), RevenueCategory.SALARY, null, "test income");
        String email = "test@test.com";

        when(userManagerService.getUserByEmailOrThrow(email)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> revenueService.addRevenue(dto, email));
        verify(revenueRepository, never()).save(any());

    }
}