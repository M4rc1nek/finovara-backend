package com.finovara.notificationservice.notification.service.limit;

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
public class LimitWarningTest {
    @Mock
    private LimitRepository limitRepository;

    @Mock
    private LimitCalculateService limitCalculateService;

    @InjectMocks
    private LimitWarningService service;

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
    void shouldReturnNotificationWhenThresholdReached() {
        LimitStatsDto stats = mock(LimitStatsDto.class);
        when(stats.percentage()).thenReturn(BigDecimal.valueOf(80));
        when(stats.periodType()).thenReturn(null);
        when(stats.limitId()).thenReturn(10L);

        when(limitCalculateService.calculateLimitStats(userId, 10L, LocalDate.now())).thenReturn(stats);

        List<NotificationResponse> result = service.getNotifications(userId);

        assertEquals(1, result.size());
    }

    @Test
    void shouldReturnNotificationWhenExactly75() {
        LimitStatsDto stats = mock(LimitStatsDto.class);
        when(stats.percentage()).thenReturn(BigDecimal.valueOf(75));
        when(stats.limitId()).thenReturn(10L);

        when(limitCalculateService.calculateLimitStats(userId, 10L, LocalDate.now())).thenReturn(stats);

        List<NotificationResponse> result = service.getNotifications(userId);

        assertEquals(1, result.size());
    }

    @Test
    void shouldNotReturnNotificationWhenAbove100() {
        LimitStatsDto stats = mock(LimitStatsDto.class);
        when(stats.percentage()).thenReturn(BigDecimal.valueOf(120));

        when(limitCalculateService.calculateLimitStats(userId, 10L, LocalDate.now())).thenReturn(stats);

        List<NotificationResponse> result = service.getNotifications(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnOnlyWarningsInRange() {
        Limit limit1 = new Limit();
        limit1.setId(10L);

        Limit limit2 = new Limit();
        limit2.setId(20L);

        when(limitRepository.findAllByUserAssignedId(userId)).thenReturn(List.of(limit1, limit2));

        LimitStatsDto stats1 = mock(LimitStatsDto.class);
        when(stats1.percentage()).thenReturn(BigDecimal.valueOf(80));
        when(stats1.limitId()).thenReturn(10L);

        LimitStatsDto stats2 = mock(LimitStatsDto.class);
        when(stats2.percentage()).thenReturn(BigDecimal.valueOf(120));

        when(limitCalculateService.calculateLimitStats(userId, 10L, LocalDate.now())).thenReturn(stats1);

        when(limitCalculateService.calculateLimitStats(userId, 20L, LocalDate.now())).thenReturn(stats2);

        List<NotificationResponse> result = service.getNotifications(userId);

        assertEquals(1, result.size());
    }

}
