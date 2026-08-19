package com.finovara.activitylogservice.accountactivity.settings.service;

import com.finovara.activitylogservice.activitylog.accountactivity.settings.dto.SettingsActivityDto;
import com.finovara.activitylogservice.activitylog.accountactivity.settings.mapper.SettingsActivityMapper;
import com.finovara.activitylogservice.activitylog.accountactivity.settings.model.SettingsActivity;
import com.finovara.activitylogservice.activitylog.accountactivity.settings.repository.SettingsActivityRepository;
import com.finovara.activitylogservice.activitylog.accountactivity.settings.service.SettingsActivityService;
import com.finovara.contracts.activity.event.settings.SettingsActivityEvent;
import com.finovara.contracts.model.SortType;
import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.model.activity.SettingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

    @Nested
    class DeleteByUserId {

        @Test
        void shouldCallRepositoryDeleteByUserIdWhenUserIdIsValid() {
            settingsActivityService.deleteByUserId(USER_ID);

            verify(settingsActivityRepository).deleteByUserId(USER_ID);
        }
    }

    @Nested
    class HandleEvent {

        @Test
        void shouldBuildEntityAndSaveViaRepository() {
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
    }

    @Nested
    class GetSettingsActivities {

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
}
