package com.finovara.finovarabackend.accountactivity.login.archive.service.map;

import com.finovara.finovarabackend.accountactivity.login.activities.model.LoginActivity;
import com.finovara.finovarabackend.accountactivity.login.activities.model.LoginActivityStatus;
import com.finovara.finovarabackend.accountactivity.login.archive.model.LoginActivityArchive;
import com.finovara.finovarabackend.accountactivity.login.archive.service.LoginActivityArchiveService;
import com.finovara.finovarabackend.config.TimeConfig;
import com.finovara.finovarabackend.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MapToArchiveLoginActivityTest {

    @Mock
    private TimeConfig timeConfig;

    @InjectMocks
    private LoginActivityArchiveService service;

    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), ZoneId.of("UTC"));
        when(timeConfig.clock()).thenReturn(fixedClock);
    }

    @Test
    void shouldMapLoginActivityToArchive_correctly() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");

        LoginActivity loginActivity = LoginActivity.builder()
                .userAssigned(user)
                .status(LoginActivityStatus.successful)
                .date(LocalDateTime.of(2026, 3, 10, 9, 30))
                .browser("Firefox")
                .ipAddress("192.168.1.100")
                .location("Berlin")
                .build();

        LoginActivityArchive archive = service.mapToArchive(loginActivity);

        assertThat(archive.getUserAssigned()).isEqualTo(user);
        assertThat(archive.getType()).isEqualTo("Login");
        assertThat(archive.getStatus()).isEqualTo(LoginActivityStatus.successful);
        assertThat(archive.getActivityLoginDate()).isEqualTo(LocalDateTime.of(2026, 3, 10, 9, 30));
        assertThat(archive.getBrowser()).isEqualTo("Firefox");
        assertThat(archive.getIpAddress()).isEqualTo("192.168.1.100");
        assertThat(archive.getLocation()).isEqualTo("Berlin");
        assertThat(archive.getMoveToArchiveDate()).isEqualTo(LocalDateTime.now(fixedClock));
    }
}