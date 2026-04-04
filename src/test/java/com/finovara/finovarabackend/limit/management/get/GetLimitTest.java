package com.finovara.finovarabackend.limit.management.get;

import com.finovara.finovarabackend.limit.dto.LimitStatsDto;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.limit.service.LimitCalculateService;
import com.finovara.finovarabackend.limit.service.LimitManagementService;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetLimitTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private LimitCalculateService limitCalculateService;

    @Mock
    private com.finovara.finovarabackend.limit.repository.LimitRepository limitRepository;

    @InjectMocks
    private LimitManagementService limitService;

    @Test
    void shouldGetLimitStatsSuccessfully() {
        String email = "user@example.com";
        User user = new User();
        user.setId(1L);

        Limit limit1 = new Limit();
        limit1.setId(10L);
        Limit limit2 = new Limit();
        limit2.setId(20L);

        LimitStatsDto dto1 = new LimitStatsDto(10L, null, new BigDecimal("100"), new BigDecimal("30"),
                new BigDecimal("70"), new BigDecimal("30"), null, LocalDate.now());
        LimitStatsDto dto2 = new LimitStatsDto(20L, null, new BigDecimal("200"), new BigDecimal("50"),
                new BigDecimal("150"), new BigDecimal("25"), null, LocalDate.now());

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(limitRepository.findAllByUserAssignedId(user.getId())).thenReturn(List.of(limit1, limit2));
        when(limitCalculateService.calculateLimitStats(user.getId(), 10L, LocalDate.now())).thenReturn(dto1);
        when(limitCalculateService.calculateLimitStats(user.getId(), 20L, LocalDate.now())).thenReturn(dto2);

        List<LimitStatsDto> result = limitService.getLimitStats(email);

        assertThat(result, contains(dto1, dto2));
        verify(userManagerService).getUserByEmailOrThrow(email);
        verify(limitRepository).findAllByUserAssignedId(user.getId());
        verify(limitCalculateService).calculateLimitStats(user.getId(), 10L, LocalDate.now());
        verify(limitCalculateService).calculateLimitStats(user.getId(), 20L, LocalDate.now());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        String email = "test@example.com";

        when(userManagerService.getUserByEmailOrThrow(email)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> limitService.getLimitStats(email));

        verify(userManagerService).getUserByEmailOrThrow(email);
        verifyNoInteractions(limitRepository, limitCalculateService);
    }

    @Test
    void shouldReturnEmptyListWhenNoLimits() {
        String email = "user@example.com";
        User user = new User();
        user.setId(1L);

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(limitRepository.findAllByUserAssignedId(user.getId())).thenReturn(List.of());

        List<LimitStatsDto> result = limitService.getLimitStats(email);

        assertThat(result, is(empty()));
        verify(userManagerService).getUserByEmailOrThrow(email);
        verify(limitRepository).findAllByUserAssignedId(user.getId());
        verifyNoInteractions(limitCalculateService);
    }
}