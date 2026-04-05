package com.finovara.finovarabackend.revenue.service.edit;

import com.finovara.finovarabackend.accountactivity.revenue.model.RevenueActivityType;
import com.finovara.finovarabackend.accountactivity.revenue.service.RevenueActivityService;
import com.finovara.finovarabackend.exception.notfound.WalletNotFoundException;
import com.finovara.finovarabackend.revenue.dto.RevenueDTO;
import com.finovara.finovarabackend.revenue.exception.notfound.RevenueNotFoundException;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.revenue.service.RevenueService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.AutoPaymentsMode;
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
        String email = "test@test.com";

        Revenue existingRevenue = new Revenue();
        existingRevenue.setId(10L);
        Long revenueId = existingRevenue.getId();

        User user = new User();
        user.setId(1L);

        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal(1000));
        RevenueDTO dto = new RevenueDTO(null, null, new BigDecimal("100"), RevenueCategory.INVESTMENT, null, "edited revenue test");

        existingRevenue.setAmount(new BigDecimal("50"));
        existingRevenue.setCategory(RevenueCategory.SALARY);
        //when

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(revenueManagerService.getRevenueOrThrow(revenueId)).thenReturn(existingRevenue);
        when(walletRepository.findByUserAssignedEmail(email)).thenReturn(Optional.of(wallet));

        existingRevenue.setUserAssigned(user);

        //then

        revenueService.editRevenue(dto, revenueId, email);

        verify(autoPaymentsService).handleRevenuePiggyBankAutomation(email, new BigDecimal("50"), AutoPaymentsMode.ROLLBACK);
        verify(autoPaymentsService).handleRevenuePiggyBankAutomation(email, new BigDecimal("100"), AutoPaymentsMode.APPLY);

        verify(revenueActivityService).updateRevenueActivity(eq(email), eq(RevenueActivityType.EDITED_REVENUE),
                eq(existingRevenue), eq(new BigDecimal("50")), eq(RevenueCategory.SALARY));

        verify(walletRepository).save(wallet);
        verify(revenueRepository).save(existingRevenue);

    }

    @Test
    void shouldThrowExceptionWhenRevenueBelongsToAnotherUser() {
        //given
        String email = "test@test.com";

        User revenueOwner = new User();
        revenueOwner.setId(1L);

        User user = new User();
        user.setId(2L);

        Revenue existingRevenue = new Revenue();
        existingRevenue.setId(10L);
        existingRevenue.setUserAssigned(revenueOwner);

        Long revenueId = existingRevenue.getId();

        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal(1000));
        RevenueDTO dto = new RevenueDTO(null, null, new BigDecimal("100"), RevenueCategory.INVESTMENT, null, "edited revenue test");

        existingRevenue.setAmount(new BigDecimal("50"));
        existingRevenue.setCategory(RevenueCategory.SALARY);

        //when
        when(revenueManagerService.getRevenueOrThrow(revenueId)).thenReturn(existingRevenue);

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);

        //then
        assertThrows(RevenueNotFoundException.class, () -> revenueService.editRevenue(dto, revenueId, email));
        verify(revenueRepository, never()).save(any());

    }

    @Test
    void shouldThrowExceptionWhenWalletDoesNotExist() {
        String email = "test@mail.com";

        User user = new User();
        user.setId(1L);

        Revenue revenue = new Revenue();
        revenue.setId(10L);
        revenue.setUserAssigned(user);

        Long revenueId = revenue.getId();

        RevenueDTO dto = new RevenueDTO(null, null, new BigDecimal("100"), RevenueCategory.INVESTMENT, null, "edited revenue test");

        when(revenueManagerService.getRevenueOrThrow(revenueId)).thenReturn(revenue);
        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(walletRepository.findByUserAssignedEmail(email)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> revenueService.editRevenue(dto, revenueId, email));
        verify(revenueRepository, never()).save(any());

    }
}
