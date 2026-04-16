package com.finovara.finovarabackend.notification.service.limit;

import com.finovara.finovarabackend.limit.dto.LimitStatsDto;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.limit.repository.LimitRepository;
import com.finovara.finovarabackend.limit.service.LimitCalculateService;
import com.finovara.finovarabackend.notification.dto.NotificationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LimitExceededTest {
    @Mock
    private LimitRepository limitRepository;

    @Mock
    private LimitCalculateService limitCalculateService;

    @InjectMocks
    private LimitExceededService limitExceededService;

    private Long userId;
    private Limit limit;

    @BeforeEach
    void setUp() {
        userId = 1L;
        limit = new Limit();
        limit.setId(10L);
        when(limitRepository.findAllByUserAssignedId(userId)).thenReturn(List.of(limit));
    }

    @Test
    void shouldReturnNotificationWhenThresholdExceeded() {
        LimitStatsDto stats = mock(LimitStatsDto.class);
        when(stats.percentage()).thenReturn(BigDecimal.valueOf(150));
        when(stats.createdAt()).thenReturn(LocalDate.now());
        when(stats.periodType()).thenReturn(null);
        when(stats.limitId()).thenReturn(10L);

        when(limitCalculateService.calculateLimitStats(userId, 10L, LocalDate.now())).thenReturn(stats);

        List<NotificationResponse> result = limitExceededService.getNotifications(userId);


        assertEquals(1, result.size());
    }

    @Test
    void shouldNotReturnNotificationWhenBelowThreshold() {
        LimitStatsDto stats = mock(LimitStatsDto.class);
        when(stats.percentage()).thenReturn(BigDecimal.valueOf(50));

        when(limitCalculateService.calculateLimitStats(userId, 10L, LocalDate.now())).thenReturn(stats);

        List<NotificationResponse> result = limitExceededService.getNotifications(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnNotificationWhenThresholdExactly100() {
        LimitStatsDto stats = mock(LimitStatsDto.class);
        when(stats.percentage()).thenReturn(BigDecimal.valueOf(100));
        when(stats.createdAt()).thenReturn(LocalDate.now());
        when(stats.periodType()).thenReturn(null);
        when(stats.limitId()).thenReturn(10L);

        when(limitCalculateService.calculateLimitStats(userId, 10L, LocalDate.now())).thenReturn(stats);

        List<NotificationResponse> result = limitExceededService.getNotifications(userId);

        assertEquals(1, result.size());
    }
}
