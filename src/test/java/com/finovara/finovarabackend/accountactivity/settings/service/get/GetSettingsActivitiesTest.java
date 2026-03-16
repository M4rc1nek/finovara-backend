package com.finovara.finovarabackend.accountactivity.settings.service.get;

import com.finovara.finovarabackend.accountactivity.settings.dto.SettingsActivityDto;
import com.finovara.finovarabackend.accountactivity.settings.mapper.SettingsActivityMapper;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingsActivity;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivitySort;
import com.finovara.finovarabackend.accountactivity.settings.repository.SettingsActivityRepository;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetSettingsActivitiesTest {

    @Mock
    private SettingsActivityRepository settingsActivityRepository;
    @Mock
    private SettingsActivityMapper settingsActivityMapper;
    @InjectMocks
    private SettingsActivityService settingsActivityService;

    private final String EMAIL = "test@mail.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(settingsActivityService, "pageSize", 10);
    }

    @Test
    void shouldReturnActivitiesSortedByNewest() {

        SettingsActivity activity = new SettingsActivity();
        SettingsActivityDto dto = new SettingsActivityDto(
                null,
                null,
                LocalDateTime.now()
        );

        when(settingsActivityRepository.findByUserAssignedEmail(eq(EMAIL), any(Pageable.class))).thenReturn(List.of(activity));

        when(settingsActivityMapper.mapToSettingActivity(activity)).thenReturn(dto);

        List<SettingsActivityDto> result = settingsActivityService.getSettingsActivities(EMAIL, SettingActivitySort.NEWEST);

        assertEquals(1, result.size());
        assertEquals(dto, result.getFirst());

        verify(settingsActivityRepository).findByUserAssignedEmail(eq(EMAIL), any(Pageable.class));
        verify(settingsActivityMapper).mapToSettingActivity(activity);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoActivities() {

        when(settingsActivityRepository.findByUserAssignedEmail(eq(EMAIL), any(Pageable.class))).thenReturn(List.of());

        List<SettingsActivityDto> result = settingsActivityService.getSettingsActivities(EMAIL, SettingActivitySort.OLDEST);

        assertEquals(0, result.size());

        verify(settingsActivityRepository).findByUserAssignedEmail(eq(EMAIL), any(Pageable.class));
        verifyNoInteractions(settingsActivityMapper);
    }
}