package com.finovara.finovarabackend.accountactivity.limit.service.update;

import com.finovara.finovarabackend.accountactivity.limit.model.LimitActivityType;
import com.finovara.finovarabackend.accountactivity.limit.repository.LimitActivityRepository;
import com.finovara.finovarabackend.accountactivity.limit.service.LimitActivityService;
import com.finovara.finovarabackend.config.TimeConfig;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.limit.model.LimitType;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
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
class UpdateLimitActivityTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private LimitActivityRepository limitActivityRepository;

    @Mock
    private TimeConfig timeConfig;

    @InjectMocks
    private LimitActivityService limitActivityService;

    private final String EMAIL = "test@mail.com";

    Limit limit;

    @BeforeEach
    void setUp(){
        limit = new Limit();
        limit.setAmount(new BigDecimal("1000"));
        limit.setLimitType(LimitType.MONTHLY);
    }

    @Test
    void shouldUpdateLimitActivitySuccessfully() {

        User user = new User();
        user.setId(1L);

        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), ZoneId.of("UTC"));

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
        when(timeConfig.clock()).thenReturn(fixedClock);

        limitActivityService.updateLimitActivity(
                EMAIL,
                LimitActivityType.EDITED_LIMIT,
                limit,
                new BigDecimal("800")
        );

        verify(limitActivityRepository).save(argThat(activity ->
                activity.getUserAssigned().equals(user) &&
                        activity.getLimitActivityType() == LimitActivityType.EDITED_LIMIT &&
                        activity.getLimitType() == LimitType.MONTHLY &&
                        activity.getAmount().compareTo(new BigDecimal("1000")) == 0 &&
                        activity.getPreviousAmount().compareTo(new BigDecimal("800")) == 0
        ));
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () ->
                limitActivityService.updateLimitActivity(
                        EMAIL,
                        LimitActivityType.EDITED_LIMIT,
                        limit,
                        new BigDecimal("800")
                )
        );

        verify(limitActivityRepository, never()).save(any());
    }
}