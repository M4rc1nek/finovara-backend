package com.finovara.finovarabackend.accountactivity.secure.login.archive.service;

import com.finovara.finovarabackend.accountactivity.secure.login.activity.model.LoginActivity;
import com.finovara.finovarabackend.accountactivity.secure.login.activity.model.LoginActivityStatus;
import com.finovara.finovarabackend.accountactivity.secure.login.archive.dto.LoginActivityArchiveDto;
import com.finovara.finovarabackend.accountactivity.secure.login.archive.model.LoginActivityArchive;
import com.finovara.finovarabackend.accountactivity.secure.login.archive.repository.LoginActivityArchiveRepository;
import com.finovara.finovarabackend.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

    @Mock
    private LoginActivityArchiveRepository loginActivityArchiveRepository;

    @InjectMocks
    private LoginActivityArchiveService loginActivityArchiveService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
    }

    @Nested
    class MapToArchiveLoginActivity {

        @Test
        void shouldMapLoginActivityToArchive_correctly() {
            LocalDateTime before = LocalDateTime.now();

            LoginActivity loginActivity = LoginActivity.builder().userAssigned(user).status(LoginActivityStatus.SUCCESSFUL).
                    createdAt(LocalDateTime.of(2026, 3, 10, 9, 30)).browser("Firefox").ipAddress("192.168.1.100").
                    location("Berlin").build();

            LoginActivityArchive archive = loginActivityArchiveService.mapToArchive(loginActivity);

            LocalDateTime after = LocalDateTime.now();

            assertThat(archive.getUserAssigned()).isEqualTo(user);
            assertThat(archive.getType()).isEqualTo("Login");
            assertThat(archive.getStatus()).isEqualTo(LoginActivityStatus.SUCCESSFUL);
            assertThat(archive.getActivityLoginDate()).isEqualTo(LocalDateTime.of(2026, 3, 10, 9, 30));

            assertThat(archive.getBrowser()).isEqualTo("Firefox");
            assertThat(archive.getIpAddress()).isEqualTo("192.168.1.100");
            assertThat(archive.getLocation()).isEqualTo("Berlin");

            assertThat(archive.getMoveToArchiveDate()).isAfterOrEqualTo(before);
            assertThat(archive.getMoveToArchiveDate()).isBeforeOrEqualTo(after);
        }
    }

    @Nested
    class GetLoginActivityArchive {

        @Test
        void shouldReturnLoginActivityArchiveForEmail() {
            String email = "user@example.com";

            LoginActivityArchiveDto dto1 = new LoginActivityArchiveDto("LOGIN", LoginActivityStatus.SUCCESSFUL,
                    LocalDateTime.now(), LocalDateTime.of(2026, 3, 10, 14, 30), "Chrome", "192.168.1.100", "Warsaw, Poland");

            LoginActivityArchiveDto dto2 = new LoginActivityArchiveDto("LOGIN", LoginActivityStatus.UNSUCCESSFUL,
                    LocalDateTime.now(), LocalDateTime.of(2026, 2, 16, 7, 21), "Firefox", "127.0.0.1", "Berlin, Germany");

            when(loginActivityArchiveRepository.findAllByUserAssignedEmailOrderByIdDesc(email)).thenReturn(List.of(dto1, dto2));

            List<LoginActivityArchiveDto> result = loginActivityArchiveService.getLoginActivityArchive(email);

            assertThat(result).hasSize(2).containsExactly(dto1, dto2);

            verify(loginActivityArchiveRepository).findAllByUserAssignedEmailOrderByIdDesc(email);
        }

        @Test
        void shouldReturnEmptyListWhenNoLoginActivityFound() {
            String email = "empty@example.com";

            when(loginActivityArchiveRepository.findAllByUserAssignedEmailOrderByIdDesc(email)).thenReturn(List.of());

            List<LoginActivityArchiveDto> result = loginActivityArchiveService.getLoginActivityArchive(email);

            assertThat(result).isEmpty();

            verify(loginActivityArchiveRepository).findAllByUserAssignedEmailOrderByIdDesc(email);
        }
    }

    @Nested
    class ArchiveLoginActivities {

        @Test
        void shouldArchiveMultipleActivities() {
            LoginActivityArchive a1 = LoginActivityArchive.builder().build();
            LoginActivityArchive a2 = LoginActivityArchive.builder().build();

            List<LoginActivityArchive> list = List.of(a1, a2);

            loginActivityArchiveService.archive(list);

            verify(loginActivityArchiveRepository).saveAll(list);
        }

        @Test
        void shouldHandleEmptyList() {
            List<LoginActivityArchive> emptyList = List.of();

            loginActivityArchiveService.archive(emptyList);

            verify(loginActivityArchiveRepository).saveAll(emptyList);
        }
    }
}