package com.finovara.finovarabackend.revenue.service.add;

import com.finovara.finovarabackend.accountactivity.revenue.model.RevenueActivityType;
import com.finovara.finovarabackend.accountactivity.revenue.service.RevenueActivityService;
import com.finovara.finovarabackend.revenue.dto.RevenueDto;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.revenue.service.RevenueService;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.service.AutoPaymentsService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
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
    @InjectMocks
    private RevenueService revenueService;

    @Test
    void shouldAddRevenueSuccessfully() {
        RevenueDto dto = new RevenueDto(null, null, new BigDecimal("100"), RevenueCategory.SALARY, null, "test revenue");
        Long userId = 1L;
        User user = new User();
        user.setId(userId);

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);

        revenueService.addRevenue(dto, userId);

        verify(walletService).addBalanceToWallet(userId, dto.amount());
        verify(revenueActivityService).createRevenueActivity(eq(userId), eq(RevenueActivityType.ADDED_REVENUE), any(Revenue.class));
        verify(revenueRepository).save(any(Revenue.class));
        verify(autoPaymentsService).handleRevenuePiggyBankAutomation(userId, dto.amount(), PiggyBankAutomationMode.APPLY);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        RevenueDto dto = new RevenueDto(null, null, new BigDecimal("100"), RevenueCategory.SALARY, null, "test revenue");
        Long userId = 1L;

        when(userManagerService.getUserByIdOrThrow(userId)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> revenueService.addRevenue(dto, userId));
        verify(revenueRepository, never()).save(any());

    }
}