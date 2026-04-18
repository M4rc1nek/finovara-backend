package com.finovara.finovarabackend.revenue.service.edit;

import com.finovara.finovarabackend.accountactivity.revenue.model.RevenueActivityType;
import com.finovara.finovarabackend.accountactivity.revenue.service.RevenueActivityService;
import com.finovara.finovarabackend.exception.notfound.WalletNotFoundException;
import com.finovara.finovarabackend.revenue.dto.RevenueDto;
import com.finovara.finovarabackend.revenue.exception.notfound.RevenueNotFoundException;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.revenue.service.RevenueService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.service.AutoPaymentsService;
import com.finovara.finovarabackend.util.revenue.RevenueManagerService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import com.finovara.finovarabackend.wallet.model.Wallet;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EditRevenueTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private RevenueManagerService revenueManagerService;
    @Mock
    private RevenueRepository revenueRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private AutoPaymentsService autoPaymentsService;
    @Mock
    private RevenueActivityService revenueActivityService;

    @InjectMocks
    private RevenueService revenueService;

    @Test
    void shouldEditRevenueSuccessfully() {
        //given
        Long userId = 1L;

        Revenue existingRevenue = new Revenue();
        existingRevenue.setId(10L);
        Long revenueId = existingRevenue.getId();

        User user = new User();
        user.setId(userId);

        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal(1000));
        RevenueDto dto = new RevenueDto(null, null, new BigDecimal("100"), RevenueCategory.INVESTMENT, null, "edited revenue test");

        existingRevenue.setAmount(new BigDecimal("50"));
        existingRevenue.setCategory(RevenueCategory.SALARY);
        //when

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(revenueManagerService.getRevenueOrThrow(revenueId)).thenReturn(existingRevenue);
        when(walletRepository.findByUserAssignedId(userId)).thenReturn(Optional.of(wallet));

        existingRevenue.setUserAssigned(user);

        //then

        revenueService.editRevenue(dto, revenueId, userId);

        verify(autoPaymentsService).handleRevenuePiggyBankAutomation(userId, new BigDecimal("50"), PiggyBankAutomationMode.ROLLBACK);
        verify(autoPaymentsService).handleRevenuePiggyBankAutomation(userId, new BigDecimal("100"), PiggyBankAutomationMode.APPLY);

        verify(revenueActivityService).updateRevenueActivity(eq(userId), eq(RevenueActivityType.EDITED_REVENUE),
                eq(existingRevenue), eq(new BigDecimal("50")), eq(RevenueCategory.SALARY));

        verify(walletRepository).save(wallet);
        verify(revenueRepository).save(existingRevenue);

    }

    @Test
    void shouldThrowExceptionWhenRevenueBelongsToAnotherUser() {
        //given
        Long userId = 2L;

        User revenueOwner = new User();
        revenueOwner.setId(1L);

        User user = new User();
        user.setId(userId);

        Revenue existingRevenue = new Revenue();
        existingRevenue.setId(10L);
        existingRevenue.setUserAssigned(revenueOwner);

        Long revenueId = existingRevenue.getId();

        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal(1000));
        RevenueDto dto = new RevenueDto(null, null, new BigDecimal("100"), RevenueCategory.INVESTMENT, null, "edited revenue test");

        existingRevenue.setAmount(new BigDecimal("50"));
        existingRevenue.setCategory(RevenueCategory.SALARY);

        //when
        when(revenueManagerService.getRevenueOrThrow(revenueId)).thenReturn(existingRevenue);

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);

        //then
        assertThrows(RevenueNotFoundException.class, () -> revenueService.editRevenue(dto, revenueId, userId));
        verify(revenueRepository, never()).save(any());

    }

    @Test
    void shouldThrowExceptionWhenWalletDoesNotExist() {
        Long userId = 1L;

        User user = new User();
        user.setId(userId);

        Revenue revenue = new Revenue();
        revenue.setId(10L);
        revenue.setUserAssigned(user);

        Long revenueId = revenue.getId();

        RevenueDto dto = new RevenueDto(null, null, new BigDecimal("100"), RevenueCategory.INVESTMENT, null, "edited revenue test");

        when(revenueManagerService.getRevenueOrThrow(revenueId)).thenReturn(revenue);
        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(walletRepository.findByUserAssignedId(userId)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> revenueService.editRevenue(dto, revenueId, userId));
        verify(revenueRepository, never()).save(any());

    }
}
