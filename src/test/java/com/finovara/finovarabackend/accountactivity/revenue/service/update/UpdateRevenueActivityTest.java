package com.finovara.finovarabackend.accountactivity.revenue.service.update;

import com.finovara.finovarabackend.accountactivity.revenue.model.RevenueActivityType;
import com.finovara.finovarabackend.accountactivity.revenue.repository.RevenueActivityRepository;
import com.finovara.finovarabackend.accountactivity.revenue.service.RevenueActivityService;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateRevenueActivityTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private RevenueActivityRepository revenueActivityRepository;

    @InjectMocks
    private RevenueActivityService revenueActivityService;

    private final Long USER_ID = 1L;

    @Test
    void shouldUpdateRevenueActivitySuccessfully() {

        User user = new User();
        user.setId(USER_ID);

        Revenue revenue = new Revenue();
        revenue.setAmount(new BigDecimal("2000"));
        revenue.setCategory(RevenueCategory.BONUS);

        BigDecimal previousAmount = new BigDecimal("1500");
        RevenueCategory previousCategory = RevenueCategory.SALARY;
        LocalDateTime now = LocalDateTime.now();

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

        revenueActivityService.updateRevenueActivity(USER_ID, RevenueActivityType.EDITED_REVENUE, revenue, previousAmount, previousCategory);

        verify(revenueActivityRepository).save(argThat(activity ->
                activity.getUserAssigned().equals(user) &&
                        activity.getType() == RevenueActivityType.EDITED_REVENUE &&
                        activity.getAmount().compareTo(new BigDecimal("2000")) == 0 &&
                        activity.getCategory() == RevenueCategory.BONUS &&
                        activity.getPreviousAmount().compareTo(previousAmount) == 0 &&
                        activity.getPreviousCategory() == previousCategory &&
                        !activity.getCreatedAt().isBefore(now)
        ));
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {

        Revenue revenue = new Revenue();
        revenue.setAmount(new BigDecimal("2000"));
        revenue.setCategory(RevenueCategory.BONUS);

        BigDecimal previousAmount = new BigDecimal("1500");
        RevenueCategory previousCategory = RevenueCategory.SALARY;

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () ->
                revenueActivityService.updateRevenueActivity(
                        USER_ID,
                        RevenueActivityType.EDITED_REVENUE,
                        revenue,
                        previousAmount,
                        previousCategory
                )
        );

        verify(revenueActivityRepository, never()).save(any());
    }
}