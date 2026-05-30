package com.finovara.activityservice.accountactivity.secure.login.archive.service;

import com.finovara.activityservice.activitylog.accountactivity.secure.login.activity.model.LoginActivity;
import com.finovara.activityservice.activitylog.accountactivity.secure.login.archive.dto.LoginActivityArchiveDto;
import com.finovara.activityservice.activitylog.accountactivity.secure.login.archive.model.LoginActivityArchive;
import com.finovara.activityservice.activitylog.accountactivity.secure.login.archive.repository.LoginActivityArchiveRepository;
import com.finovara.activityservice.activitylog.accountactivity.secure.login.archive.service.LoginActivityArchiveService;
import com.finovara.contracts.model.activity.LoginActivityStatus;
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
class LoginActivityArchiveServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private LoginActivityArchiveRepository loginActivityArchiveRepository;

    @InjectMocks
    private LoginActivityArchiveService loginActivityArchiveService;

    @Test
    void shouldMapLoginActivityToArchive() {
        LocalDateTime before = LocalDateTime.now();
        LocalDateTime activityDate = LocalDateTime.of(2026, 3, 10, 9, 30);
        LoginActivity loginActivity = LoginActivity.builder()
                .userId(USER_ID)
                .status(LoginActivityStatus.SUCCESSFUL)
                .createdAt(activityDate)
                .browser("Firefox")
                .ipAddress("192.168.1.100")
                .location("Berlin")
                .build();

        LoginActivityArchive archive = loginActivityArchiveService.mapToArchive(loginActivity);

        assertThat(archive.getUserId()).isEqualTo(USER_ID);
        assertThat(archive.getType()).isEqualTo("Login");
        assertThat(archive.getStatus()).isEqualTo(LoginActivityStatus.SUCCESSFUL);
        assertThat(archive.getActivityLoginDate()).isEqualTo(activityDate);
        assertThat(archive.getBrowser()).isEqualTo("Firefox");
        assertThat(archive.getIpAddress()).isEqualTo("192.168.1.100");
        assertThat(archive.getLocation()).isEqualTo("Berlin");
        assertThat(archive.getMoveToArchiveDate()).isBetween(before, LocalDateTime.now());
    }

    @Test
    void shouldReturnLoginActivityArchiveForUserId() {
        LoginActivityArchiveDto dto = new LoginActivityArchiveDto(
                "Login",
                LoginActivityStatus.SUCCESSFUL,
                LocalDateTime.now(),
                LocalDateTime.of(2026, 3, 10, 14, 30),
                "Chrome",
                "192.168.1.100",
                "Warsaw, Poland"
        );
        when(loginActivityArchiveRepository.findAllByUserIdOrderByIdDesc(USER_ID)).thenReturn(List.of(dto));

        List<LoginActivityArchiveDto> result = loginActivityArchiveService.getLoginActivityArchive(USER_ID);

        assertThat(result).containsExactly(dto);
        verify(loginActivityArchiveRepository).findAllByUserIdOrderByIdDesc(USER_ID);
    }

    @Test
    void shouldArchiveActivities() {
        List<LoginActivityArchive> archives = List.of(LoginActivityArchive.builder().build());

        loginActivityArchiveService.archive(archives);

        verify(loginActivityArchiveRepository).saveAll(archives);
    }
}
