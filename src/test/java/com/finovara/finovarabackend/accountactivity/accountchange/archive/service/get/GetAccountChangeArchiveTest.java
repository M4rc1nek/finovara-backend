package com.finovara.finovarabackend.accountactivity.accountchange.archive.service.get;

import com.finovara.finovarabackend.accountactivity.accountchange.activities.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.accountchange.archive.dto.AccountChangeArchiveDto;
import com.finovara.finovarabackend.accountactivity.accountchange.archive.repository.AccountChangeArchiveRepository;
import com.finovara.finovarabackend.accountactivity.accountchange.archive.service.AccountChangeArchiveService;
import com.finovara.finovarabackend.config.TimeConfig;
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
class GetAccountChangeArchiveTest {

    @Mock
    private AccountChangeArchiveRepository accountChangeArchiveRepository;

    @Mock
    private TimeConfig timeConfig;

    @InjectMocks
    private AccountChangeArchiveService service;

    @Test
    void shouldReturnArchiveListForEmail() {
        String email = "user@example.com";

        AccountChangeArchiveDto dto1 = new AccountChangeArchiveDto(
                AccountChangesActivityType.PASSWORD_CHANGED,
                LocalDateTime.now(),
                LocalDateTime.of(2026, 3, 10, 14, 30),
                "Chrome",
                "192.168.1.100",
                "Warsaw, Poland"
        );

        AccountChangeArchiveDto dto2 = new AccountChangeArchiveDto(
                AccountChangesActivityType.PASSWORD_CHANGED,
                LocalDateTime.now(),
                LocalDateTime.of(2026, 2, 16, 7, 21),
                "Firefox",
                "127.0.0.1",
                "Berlin, Germany"
        );

        List<AccountChangeArchiveDto> dtoList = List.of(dto1, dto2);

        when(accountChangeArchiveRepository.findAllByUserAssignedEmailOrderByIdDesc(email)).thenReturn(dtoList);

        List<AccountChangeArchiveDto> result = service.getAccountChangeArchive(email);

        assertThat(result).hasSize(2).containsExactly(dto1, dto2);
        verify(accountChangeArchiveRepository, times(1)).findAllByUserAssignedEmailOrderByIdDesc(email);
    }

    @Test
    void shouldReturnEmptyListWhenNoArchiveFound() {
        String email = "empty@example.com";
        when(accountChangeArchiveRepository.findAllByUserAssignedEmailOrderByIdDesc(email)).thenReturn(List.of());

        List<AccountChangeArchiveDto> result = service.getAccountChangeArchive(email);

        assertThat(result).isEmpty();
        verify(accountChangeArchiveRepository, times(1)).findAllByUserAssignedEmailOrderByIdDesc(email);
    }
}