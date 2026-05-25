package com.finovara.activityservice.activity_log.accountactivity.settings.service;

import com.finovara.activityservice.activity_log.accountactivity.settings.dto.SettingsActivityDto;
import com.finovara.activityservice.activity_log.accountactivity.settings.mapper.SettingsActivityMapper;
import com.finovara.activityservice.activity_log.accountactivity.settings.model.SettingsActivity;
import com.finovara.activityservice.activity_log.accountactivity.settings.repository.SettingsActivityRepository;
import com.finovara.activityservice.contracts.event.settings.SettingsActivityEvent;
import com.finovara.activityservice.contracts.model.SortType;
import com.finovara.activityservice.contracts.model.activity.SettingActivityStatus;
import com.finovara.activityservice.contracts.model.activity.SettingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsActivityServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 5, 25, 14, 0);

    @Mock
    private SettingsActivityRepository settingsActivityRepository;

    @Mock
    private SettingsActivityMapper settingsActivityMapper;

    @InjectMocks
    private SettingsActivityService settingsActivityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(settingsActivityService, "pageSize", 10);
    }

    @Test
    void shouldSaveActivityFromEvent() {
        SettingsActivityEvent event = new SettingsActivityEvent(
                USER_ID,
                SettingType.NOTIFICATION_PASSWORD_CHANGED,
                SettingActivityStatus.ENABLED,
                OCCURRED_AT
        );

        settingsActivityService.handleEvent(event);

        ArgumentCaptor<SettingsActivity> captor = ArgumentCaptor.forClass(SettingsActivity.class);
        verify(settingsActivityRepository).save(captor.capture());

        SettingsActivity activity = captor.getValue();
        assertThat(activity.getUserId()).isEqualTo(USER_ID);
        assertThat(activity.getSettingType()).isEqualTo(event.settingType());
        assertThat(activity.getStatus()).isEqualTo(event.status());
        assertThat(activity.getCreatedAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    void shouldReturnMappedActivities() {
        SettingsActivity activity = SettingsActivity.builder().userId(USER_ID).build();
        SettingsActivityDto dto = new SettingsActivityDto(
                SettingActivityStatus.DISABLED,
                SettingType.PIGGY_BANK_ROUND_UP,
                OCCURRED_AT
        );

        when(settingsActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of(activity));
        when(settingsActivityMapper.mapToSettingActivity(activity)).thenReturn(dto);

        List<SettingsActivityDto> result = settingsActivityService.getSettingsActivities(USER_ID, SortType.NEWEST);

        assertThat(result).containsExactly(dto);
        verify(settingsActivityRepository).findByUserId(eq(USER_ID), any(Pageable.class));
        verify(settingsActivityMapper).mapToSettingActivity(activity);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoActivities() {
        when(settingsActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of());

        List<SettingsActivityDto> result = settingsActivityService.getSettingsActivities(USER_ID, SortType.OLDEST);

        assertThat(result).isEmpty();
        verifyNoInteractions(settingsActivityMapper);
    }
}
