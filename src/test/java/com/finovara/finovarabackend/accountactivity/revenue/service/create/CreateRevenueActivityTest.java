package com.finovara.finovarabackend.accountactivity.revenue.service.create;

import com.finovara.finovarabackend.accountactivity.revenue.model.RevenueActivityType;
import com.finovara.finovarabackend.accountactivity.revenue.repository.RevenueActivityRepository;
import com.finovara.finovarabackend.accountactivity.revenue.service.RevenueActivityService;
import com.finovara.finovarabackend.config.TimeConfig;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateRevenueActivityTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private RevenueActivityRepository revenueActivityRepository;

    @Mock
    private TimeConfig timeConfig;

    @InjectMocks
    private RevenueActivityService revenueActivityService;

    private final String EMAIL = "test@mail.com";

    @Test
    void shouldCreateRevenueActivitySuccessfully() {

        User user = new User();
        user.setId(1L);

        Revenue revenue = new Revenue();
        revenue.setAmount(new BigDecimal("1000"));
        revenue.setCategory(RevenueCategory.SALARY);

        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), ZoneId.of("UTC"));

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
        when(timeConfig.clock()).thenReturn(fixedClock);

        revenueActivityService.createRevenueActivity(EMAIL, RevenueActivityType.ADDED_REVENUE, revenue);

        verify(revenueActivityRepository).save(argThat(activity ->
                activity.getUserAssigned().equals(user) &&
                        activity.getType() == RevenueActivityType.ADDED_REVENUE &&
                        activity.getAmount().compareTo(new BigDecimal("1000")) == 0 &&
                        activity.getCategory() == RevenueCategory.SALARY
        ));
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {

        Revenue revenue = new Revenue();
        revenue.setAmount(new BigDecimal("1000"));
        revenue.setCategory(RevenueCategory.SALARY);

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () ->
                revenueActivityService.createRevenueActivity(
                        EMAIL,
                        RevenueActivityType.ADDED_REVENUE,
                        revenue
                )
        );
        verify(revenueActivityRepository, never()).save(any());
    }
}