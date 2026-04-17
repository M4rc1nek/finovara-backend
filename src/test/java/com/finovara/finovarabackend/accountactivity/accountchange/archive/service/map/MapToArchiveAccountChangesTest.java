package com.finovara.finovarabackend.accountactivity.accountchange.archive.service.map;

import com.finovara.finovarabackend.accountactivity.security.accountchange.activities.model.AccountChangesActivity;
import com.finovara.finovarabackend.accountactivity.security.accountchange.activities.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.security.accountchange.archive.model.AccountChangeArchive;
import com.finovara.finovarabackend.accountactivity.security.accountchange.archive.service.AccountChangeArchiveService;
import com.finovara.finovarabackend.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MapToArchiveAccountChangesTest {

    @InjectMocks
    private AccountChangeArchiveService service;

    @Test
    void shouldMapAccountChangesActivityToArchive_correctly() {
        LocalDateTime before = LocalDateTime.now();

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

        LocalDateTime after = LocalDateTime.now();

        assertThat(archive.getUserAssigned()).isEqualTo(user);
        assertThat(archive.getType()).isEqualTo(AccountChangesActivityType.PASSWORD_CHANGED);
        assertThat(archive.getActivityAccountChangesDate()).isEqualTo(LocalDateTime.of(2026, 3, 10, 8, 0));
        assertThat(archive.getBrowser()).isEqualTo("Chrome");
        assertThat(archive.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(archive.getLocation()).isEqualTo("TestCity");
        assertThat(archive.getMoveToArchiveDate().isBefore(before)).isFalse();
        assertThat(archive.getMoveToArchiveDate().isAfter(after)).isFalse();
    }
}