package com.finovara.finovarabackend.accountactivity.login.activities.service.get;

import com.finovara.finovarabackend.accountactivity.security.login.activities.dto.LoginActivityDto;
import com.finovara.finovarabackend.accountactivity.security.login.activities.model.LoginActivityStatus;
import com.finovara.finovarabackend.accountactivity.security.login.activities.repository.LoginActivityRepository;
import com.finovara.finovarabackend.accountactivity.security.login.activities.service.LoginActivityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetLoginActivityTest {
    @Mock
    private LoginActivityRepository loginActivityRepository;

    @InjectMocks
    private LoginActivityService loginActivityService;

    private static final String EMAIL = "test@example.com";

    @Test
    void shouldReturnLoginActivitiesForUser() {
        LoginActivityDto activity1 = new LoginActivityDto(
                "Login",
                LoginActivityStatus.successful,
                LocalDateTime.now(),
                "Chrome",
                "192.168.1.1",
                "Poland"
        );

        LoginActivityDto activity2 = new LoginActivityDto(
                "Login",
                LoginActivityStatus.successful,
                LocalDateTime.now(),
                "Firefox",
                "192.168.1.2",
                "Germany"
        );

        List<LoginActivityDto> expected = List.of(activity1, activity2);

        when(loginActivityRepository.findByUserAssignedEmailOrderByDesc(EMAIL))
                .thenReturn(expected);

        List<LoginActivityDto> result = loginActivityService.getLoginActivity(EMAIL);

        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expected);

        verify(loginActivityRepository).findByUserAssignedEmailOrderByDesc(EMAIL);
    }

    @Test
    void shouldReturnEmptyListWhenNoActivitiesExist() {
        when(loginActivityRepository.findByUserAssignedEmailOrderByDesc(EMAIL))
                .thenReturn(List.of());

        List<LoginActivityDto> result = loginActivityService.getLoginActivity(EMAIL);

        assertThat(result).isEmpty();

        verify(loginActivityRepository).findByUserAssignedEmailOrderByDesc(EMAIL);
    }
}