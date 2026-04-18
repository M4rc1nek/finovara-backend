package com.finovara.finovarabackend.accountactivity.limit.service.get;

import com.finovara.finovarabackend.util.model.SortType;
import com.finovara.finovarabackend.accountactivity.limit.dto.LimitActivityDto;
import com.finovara.finovarabackend.accountactivity.limit.mapper.LimitActivityMapper;
import com.finovara.finovarabackend.accountactivity.limit.model.LimitActivity;
import com.finovara.finovarabackend.accountactivity.limit.repository.LimitActivityRepository;
import com.finovara.finovarabackend.accountactivity.limit.service.LimitActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetLimitActivityTest {

    @Mock
    private LimitActivityRepository limitActivityRepository;

    @Mock
    private LimitActivityMapper limitActivityMapper;

    @InjectMocks
    private LimitActivityService limitActivityService;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(limitActivityService, "pageSize", 10);
    }

    @Test
    void shouldReturnActivitiesSortedByNewest() {

        LimitActivity activity = new LimitActivity();
        LimitActivityDto dto = new LimitActivityDto(
                null,
                null,
                new BigDecimal("500"),
                null,
                LocalDateTime.now()
        );

        when(limitActivityRepository.findByUserAssignedId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(List.of(activity));

        when(limitActivityMapper.mapToLimitActivity(activity)).thenReturn(dto);

        List<LimitActivityDto> result =
                limitActivityService.getLimitActivity(USER_ID, SortType.NEWEST);

        assertEquals(1, result.size());
        assertEquals(dto, result.getFirst());

        verify(limitActivityRepository).findByUserAssignedId(eq(USER_ID), any(Pageable.class));
        verify(limitActivityMapper).mapToLimitActivity(activity);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoActivities() {

        when(limitActivityRepository.findByUserAssignedId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(List.of());

        List<LimitActivityDto> result =
                limitActivityService.getLimitActivity(USER_ID, SortType.OLDEST);

        assertEquals(0, result.size());

        verify(limitActivityRepository).findByUserAssignedId(eq(USER_ID), any(Pageable.class));
        verifyNoInteractions(limitActivityMapper);
    }
}