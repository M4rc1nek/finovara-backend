package com.finovara.finovarabackend.accountactivity.limit.service.create;

import com.finovara.finovarabackend.accountactivity.limit.model.LimitActivityType;
import com.finovara.finovarabackend.accountactivity.limit.repository.LimitActivityRepository;
import com.finovara.finovarabackend.accountactivity.limit.service.LimitActivityService;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
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
class CreateLimitActivityTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private LimitActivityRepository limitActivityRepository;

    @InjectMocks
    private LimitActivityService limitActivityService;

    private final String EMAIL = "test@mail.com";

    @Test
    void shouldCreateLimitActivitySuccessfully() {

        User user = new User();
        user.setId(1L);

        Limit limit = new Limit();
        limit.setAmount(new BigDecimal("500"));
        limit.setPeriodType(PeriodType.MONTHLY);
        LocalDateTime now = LocalDateTime.now();



        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);

        limitActivityService.createLimitActivity(EMAIL, LimitActivityType.ADDED_LIMIT, limit);

        verify(limitActivityRepository).save(argThat(activity ->
                activity.getUserAssigned().equals(user) &&
                        activity.getLimitActivityType() == LimitActivityType.ADDED_LIMIT &&
                        activity.getPeriodType() == PeriodType.MONTHLY &&
                        activity.getAmount().compareTo(new BigDecimal("500")) == 0 &&
                        !activity.getDate().isBefore(now)
        ));
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {

        Limit limit = new Limit();
        limit.setAmount(new BigDecimal("500"));
        limit.setPeriodType(PeriodType.MONTHLY);

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () ->
                limitActivityService.createLimitActivity(
                        EMAIL,
                        LimitActivityType.ADDED_LIMIT,
                        limit
                )
        );

        verify(limitActivityRepository, never()).save(any());
    }
}