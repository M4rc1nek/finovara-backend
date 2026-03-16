package com.finovara.finovarabackend.accountactivity.login.archive.service.get;

import com.finovara.finovarabackend.accountactivity.login.activities.model.LoginActivityStatus;
import com.finovara.finovarabackend.accountactivity.login.archive.dto.LoginActivityArchiveDto;
import com.finovara.finovarabackend.accountactivity.login.archive.repository.LoginActivityArchiveRepository;
import com.finovara.finovarabackend.accountactivity.login.archive.service.LoginActivityArchiveService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetLoginActivityArchiveTest {

    @Mock
    private LoginActivityArchiveRepository loginActivityArchiveRepository;

    @InjectMocks
    private LoginActivityArchiveService service;

    @Test
    void shouldReturnLoginActivityArchiveForEmail() {
        String email = "user@example.com";

        LoginActivityArchiveDto dto1 = new LoginActivityArchiveDto(
                "LOGIN",
                LoginActivityStatus.successful,
                LocalDateTime.now(),
                LocalDateTime.of(2026, 3, 10, 14, 30),
                "Chrome",
                "192.168.1.100",
                "Warsaw, Poland"
        );

        LoginActivityArchiveDto dto2 = new LoginActivityArchiveDto(
                "LOGIN",
                LoginActivityStatus.unsuccessful,
                LocalDateTime.now(),
                LocalDateTime.of(2026, 2, 16, 7, 21),
                "Firefox",
                "127.0.0.1",
                "Berlin, Germany"
        );

        List<LoginActivityArchiveDto> dtoList = List.of(dto1, dto2);

        when(loginActivityArchiveRepository.findAllByUserAssignedEmailOrderByIdDesc(email)).thenReturn(dtoList);

        List<LoginActivityArchiveDto> result = service.getLoginActivityArchive(email);

        assertThat(result).hasSize(2).containsExactly(dto1, dto2);

        verify(loginActivityArchiveRepository, times(1)).findAllByUserAssignedEmailOrderByIdDesc(email);
    }

    @Test
    void shouldReturnEmptyListWhenNoLoginActivityFound() {
        String email = "empty@example.com";

        when(loginActivityArchiveRepository.findAllByUserAssignedEmailOrderByIdDesc(email)).thenReturn(List.of());

        List<LoginActivityArchiveDto> result = service.getLoginActivityArchive(email);

        assertThat(result).isEmpty();

        verify(loginActivityArchiveRepository, times(1)).findAllByUserAssignedEmailOrderByIdDesc(email);
    }
}