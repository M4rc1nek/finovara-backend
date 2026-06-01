package com.finovara.activityservice.accountactivity.secure.login.activity.service;

import com.finovara.activityservice.activitylog.accountactivity.secure.login.activity.dto.LoginActivityDto;
import com.finovara.activityservice.activitylog.accountactivity.secure.login.activity.model.LoginActivity;
import com.finovara.activityservice.activitylog.accountactivity.secure.login.activity.repository.LoginActivityRepository;
import com.finovara.activityservice.activitylog.accountactivity.secure.login.activity.service.LoginActivityService;
import com.finovara.activityservice.activitylog.accountactivity.secure.login.archive.model.LoginActivityArchive;
import com.finovara.activityservice.activitylog.accountactivity.secure.login.archive.service.LoginActivityArchiveService;
import com.finovara.contracts.event.activity.secure.login.activity.LoginActivityEvent;
import com.finovara.contracts.model.activity.LoginActivityStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginActivityServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 5, 25, 15, 0);

    @Mock
    private LoginActivityRepository loginActivityRepository;

    @Mock
    private LoginActivityArchiveService archiveService;

    @InjectMocks
    private LoginActivityService loginActivityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loginActivityService, "pageSize", 10);
    }

    @Test
    void shouldSaveActivityFromEventAndNotArchiveWhenBelowThreshold() {
        LoginActivityEvent event = event();
        when(loginActivityRepository.countActivityLoginByUserId(USER_ID)).thenReturn(9L);

        loginActivityService.handleEvent(event);

        ArgumentCaptor<LoginActivity> captor = ArgumentCaptor.forClass(LoginActivity.class);
        verify(loginActivityRepository).save(captor.capture());
        verify(archiveService, never()).archive(any());

        LoginActivity activity = captor.getValue();
        assertThat(activity.getUserId()).isEqualTo(USER_ID);
        assertThat(activity.getType()).isEqualTo("Login");
        assertThat(activity.getStatus()).isEqualTo(event.status());
        assertThat(activity.getBrowser()).isEqualTo(event.browser());
        assertThat(activity.getIpAddress()).isEqualTo(event.ipAddress());
        assertThat(activity.getLocation()).isEqualTo(event.location());
        assertThat(activity.getCreatedAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    void shouldArchiveOldestActivitiesWhenThresholdExceeded() {
        LoginActivityEvent event = event();
        LoginActivity oldest = LoginActivity.builder().id(1L).userId(USER_ID).build();
        LoginActivityArchive archive = LoginActivityArchive.builder().userId(USER_ID).build();

        when(loginActivityRepository.countActivityLoginByUserId(USER_ID)).thenReturn(11L);
        when(loginActivityRepository.findOldestByUserId(eq(USER_ID), any(PageRequest.class))).thenReturn(List.of(oldest));
        when(archiveService.mapToArchive(oldest)).thenReturn(archive);

        loginActivityService.handleEvent(event);

        verify(loginActivityRepository).save(any(LoginActivity.class));
        verify(archiveService).archive(List.of(archive));
        verify(loginActivityRepository).deleteAll(List.of(oldest));
    }

    @Test
    void shouldReturnLoginActivityDtos() {
        LoginActivityDto dto = new LoginActivityDto("Login", LoginActivityStatus.SUCCESSFUL, OCCURRED_AT, "Firefox", "127.0.0.1", "Localhost");
        when(loginActivityRepository.findByUserIdOrderByDesc(USER_ID)).thenReturn(List.of(dto));

        List<LoginActivityDto> result = loginActivityService.getLoginActivity(USER_ID);

        assertThat(result).containsExactly(dto);
        verify(loginActivityRepository).findByUserIdOrderByDesc(USER_ID);
    }

    private LoginActivityEvent event() {
        return new LoginActivityEvent(
                USER_ID,
                LoginActivityStatus.SUCCESSFUL,
                "Firefox",
                "127.0.0.1",
                "Localhost",
                OCCURRED_AT
        );
    }
}
