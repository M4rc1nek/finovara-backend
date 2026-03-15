package com.finovara.finovarabackend.accountactivity.accountchange.archive.service.map;

import com.finovara.finovarabackend.accountactivity.accountchange.activities.model.AccountChangesActivity;
import com.finovara.finovarabackend.accountactivity.accountchange.activities.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.accountchange.archive.model.AccountChangeArchive;
import com.finovara.finovarabackend.accountactivity.accountchange.archive.service.AccountChangeArchiveService;
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
class MapToArchiveAccountChangesTest {

    @Mock
    private TimeConfig timeConfig;

    @InjectMocks
    private AccountChangeArchiveService service;

    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), ZoneId.of("UTC"));
        when(timeConfig.clock()).thenReturn(fixedClock);
    }

    @Test
    void shouldMapAccountChangesActivityToArchive_correctly() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");

        AccountChangesActivity activity = AccountChangesActivity.builder()
                .userAssigned(user)
                .type(AccountChangesActivityType.PASSWORD_CHANGED)
                .date(LocalDateTime.of(2026, 3, 10, 8, 0))
                .browser("Chrome")
                .ipAddress("127.0.0.1")
                .location("TestCity")
                .build();

        AccountChangeArchive archive = service.mapToArchive(activity);

        assertThat(archive.getUserAssigned()).isEqualTo(user);
        assertThat(archive.getType()).isEqualTo(AccountChangesActivityType.PASSWORD_CHANGED);
        assertThat(archive.getActivityAccountChangesDate()).isEqualTo(LocalDateTime.of(2026, 3, 10, 8, 0));
        assertThat(archive.getBrowser()).isEqualTo("Chrome");
        assertThat(archive.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(archive.getLocation()).isEqualTo("TestCity");
        assertThat(archive.getMoveToArchiveDate()).isEqualTo(LocalDateTime.now(fixedClock));
    }
}