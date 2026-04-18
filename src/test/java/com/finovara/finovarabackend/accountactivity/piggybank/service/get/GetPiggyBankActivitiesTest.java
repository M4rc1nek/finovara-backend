package com.finovara.finovarabackend.accountactivity.piggybank.service.get;

import com.finovara.finovarabackend.util.model.SortType;
import com.finovara.finovarabackend.accountactivity.piggybank.dto.PiggyBankActivityDto;
import com.finovara.finovarabackend.accountactivity.piggybank.mapper.PiggyBankActivityMapper;
import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivity;
import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.repository.PiggyBankActivityRepository;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.piggybank.model.PiggyBankGoalType;
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
class GetPiggyBankActivitiesTest {

    @Mock
    private PiggyBankActivityRepository piggyBankActivityRepository;

    @Mock
    private PiggyBankActivityMapper piggyBankActivityMapper;

    @InjectMocks
    private PiggyBankActivityService piggyBankActivityService;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(piggyBankActivityService, "pageSize", 10);
    }

    @Test
    void shouldReturnActivitiesSortedByNewest() {

        PiggyBankActivity activity = new PiggyBankActivity();
        PiggyBankActivityDto dto = new PiggyBankActivityDto(
                "My piggybank",
                null,
                PiggyBankActivityType.ADDED_PIGGY_BANK,
                PiggyBankGoalType.GIFTS,
                null,
                new BigDecimal("100"),
                null,
                null,
                null,
                LocalDateTime.now()
        );

        when(piggyBankActivityRepository.findByUserAssignedId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of(activity));

        when(piggyBankActivityMapper.mapToPiggyBankActivity(activity)).thenReturn(dto);

        List<PiggyBankActivityDto> result = piggyBankActivityService.getPiggyBankActivities(USER_ID, SortType.NEWEST);

        assertEquals(1, result.size());
        assertEquals(dto, result.getFirst());

        verify(piggyBankActivityRepository).findByUserAssignedId(eq(USER_ID), any(Pageable.class));
        verify(piggyBankActivityMapper).mapToPiggyBankActivity(activity);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoActivities() {

        when(piggyBankActivityRepository.findByUserAssignedId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of());

        List<PiggyBankActivityDto> result = piggyBankActivityService.getPiggyBankActivities(USER_ID, SortType.OLDEST);

        assertEquals(0, result.size());

        verify(piggyBankActivityRepository).findByUserAssignedId(eq(USER_ID), any(Pageable.class));
        verifyNoInteractions(piggyBankActivityMapper);
    }
}