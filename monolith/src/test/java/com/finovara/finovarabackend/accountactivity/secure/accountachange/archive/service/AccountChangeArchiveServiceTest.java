package com.finovara.finovarabackend.accountactivity.secure.accountachange.archive.service;

import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.model.AccountChangesActivity;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.activity.model.AccountChangesActivityType;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.archive.dto.AccountChangeArchiveDto;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.archive.model.AccountChangeArchive;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.archive.repository.AccountChangeArchiveRepository;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.archive.service.AccountChangeArchiveService;
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
class AccountChangeArchiveServiceTest {

    @Mock
    private AccountChangeArchiveRepository accountChangeArchiveRepository;

    @InjectMocks
    private AccountChangeArchiveService accountChangeArchiveService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
    }
    @Nested
    class MapToArchive {

        @Test
        void shouldMapAccountChangesActivityToArchiveCorrectly() {
            LocalDateTime before = LocalDateTime.now();

            AccountChangesActivity activity = AccountChangesActivity.builder().userAssigned(user).
                    type(AccountChangesActivityType.PASSWORD_CHANGED).createdAt(LocalDateTime.of(2026, 3, 10, 8, 0)).
                    browser("Chrome").ipAddress("127.0.0.1").location("TestCity").build();

            AccountChangeArchive archive = accountChangeArchiveService.mapToArchive(activity);

            LocalDateTime after = LocalDateTime.now();

            assertThat(archive.getUserAssigned()).isEqualTo(user);
            assertThat(archive.getType()).isEqualTo(AccountChangesActivityType.PASSWORD_CHANGED);
            assertThat(archive.getActivityAccountChangesDate()).isEqualTo(LocalDateTime.of(2026, 3, 10, 8, 0));

            assertThat(archive.getBrowser()).isEqualTo("Chrome");
            assertThat(archive.getIpAddress()).isEqualTo("127.0.0.1");
            assertThat(archive.getLocation()).isEqualTo("TestCity");

            assertThat(archive.getMoveToArchiveDate()).isAfterOrEqualTo(before);
            assertThat(archive.getMoveToArchiveDate()).isBeforeOrEqualTo(after);
        }
    }

    @Nested
    class GetArchive {

        @Test
        void shouldReturnArchiveListForEmail() {
            String email = "user@example.com";

            AccountChangeArchiveDto dto1 = new AccountChangeArchiveDto(AccountChangesActivityType.PASSWORD_CHANGED, LocalDateTime.now(), LocalDateTime.of(2026, 3, 10, 14, 30), "Chrome", "192.168.1.100", "Warsaw, Poland");

            AccountChangeArchiveDto dto2 = new AccountChangeArchiveDto(AccountChangesActivityType.PASSWORD_CHANGED, LocalDateTime.now(), LocalDateTime.of(2026, 2, 16, 7, 21), "Firefox", "127.0.0.1", "Berlin, Germany");

            when(accountChangeArchiveRepository.findAllByUserAssignedEmailOrderByIdDesc(email)).thenReturn(List.of(dto1, dto2));

            List<AccountChangeArchiveDto> result = accountChangeArchiveService.getAccountChangeArchive(email);

            assertThat(result).hasSize(2).containsExactly(dto1, dto2);

            verify(accountChangeArchiveRepository).findAllByUserAssignedEmailOrderByIdDesc(email);
        }

        @Test
        void shouldReturnEmptyListWhenNoArchiveFound() {
            String email = "empty@example.com";

            when(accountChangeArchiveRepository.findAllByUserAssignedEmailOrderByIdDesc(email)).thenReturn(List.of());

            List<AccountChangeArchiveDto> result = accountChangeArchiveService.getAccountChangeArchive(email);

            assertThat(result).isEmpty();

            verify(accountChangeArchiveRepository).findAllByUserAssignedEmailOrderByIdDesc(email);
        }
    }

    @Nested
    class Archive {

        @Test
        void shouldArchiveMultipleActivities() {
            AccountChangeArchive a1 = AccountChangeArchive.builder().build();
            AccountChangeArchive a2 = AccountChangeArchive.builder().build();

            List<AccountChangeArchive> list = List.of(a1, a2);

            accountChangeArchiveService.archive(list);

            verify(accountChangeArchiveRepository).saveAll(list);
        }

        @Test
        void shouldHandleEmptyList() {
            List<AccountChangeArchive> emptyList = List.of();

            accountChangeArchiveService.archive(emptyList);

            verify(accountChangeArchiveRepository).saveAll(emptyList);
        }
    }
}