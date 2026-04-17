package com.finovara.finovarabackend.accountactivity.secure.login.archive.service.map;


import com.finovara.finovarabackend.accountactivity.secure.login.activity.model.LoginActivity;
import com.finovara.finovarabackend.accountactivity.secure.login.activity.model.LoginActivityStatus;
import com.finovara.finovarabackend.accountactivity.secure.login.archive.model.LoginActivityArchive;
import com.finovara.finovarabackend.accountactivity.secure.login.archive.service.LoginActivityArchiveService;
import com.finovara.finovarabackend.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MapToArchiveLoginActivityTest {

    @InjectMocks
    private LoginActivityArchiveService service;

    @Test
    void shouldMapLoginActivityToArchive_correctly() {
        LocalDateTime before = LocalDateTime.now();

        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");

        LoginActivity loginActivity = LoginActivity.builder()
                .userAssigned(user)
                .status(LoginActivityStatus.SUCCESSFUL)
                .date(LocalDateTime.of(2026, 3, 10, 9, 30))
                .browser("Firefox")
                .ipAddress("192.168.1.100")
                .location("Berlin")
                .build();

        LoginActivityArchive archive = service.mapToArchive(loginActivity);

        LocalDateTime after = LocalDateTime.now();

        assertThat(archive.getUserAssigned()).isEqualTo(user);
        assertThat(archive.getType()).isEqualTo("Login");
        assertThat(archive.getStatus()).isEqualTo(LoginActivityStatus.SUCCESSFUL);
        assertThat(archive.getActivityLoginDate()).isEqualTo(LocalDateTime.of(2026, 3, 10, 9, 30));
        assertThat(archive.getBrowser()).isEqualTo("Firefox");
        assertThat(archive.getIpAddress()).isEqualTo("192.168.1.100");
        assertThat(archive.getLocation()).isEqualTo("Berlin");
        assertThat(archive.getMoveToArchiveDate().isBefore(before)).isFalse();
        assertThat(archive.getMoveToArchiveDate().isAfter(after)).isFalse();
    }
}