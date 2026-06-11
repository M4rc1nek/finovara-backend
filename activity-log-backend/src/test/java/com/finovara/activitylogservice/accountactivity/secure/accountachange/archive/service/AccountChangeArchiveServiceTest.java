package com.finovara.activitylogservice.accountactivity.secure.accountachange.archive.service;

import com.finovara.activitylogservice.activitylog.accountactivity.secure.accountchange.activity.model.AccountChangesActivity;
import com.finovara.activitylogservice.activitylog.accountactivity.secure.accountchange.archive.dto.AccountChangeArchiveDto;
import com.finovara.activitylogservice.activitylog.accountactivity.secure.accountchange.archive.model.AccountChangeArchive;
import com.finovara.activitylogservice.activitylog.accountactivity.secure.accountchange.archive.repository.AccountChangeArchiveRepository;
import com.finovara.activitylogservice.activitylog.accountactivity.secure.accountchange.archive.service.AccountChangeArchiveService;
import com.finovara.contracts.model.activity.AccountChangesActivityType;
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

    private static final Long USER_ID = 1L;

    @Mock
    private AccountChangeArchiveRepository accountChangeArchiveRepository;

    @InjectMocks
    private AccountChangeArchiveService accountChangeArchiveService;

    @Nested
    class DeleteByUserId {

        @Test
        void shouldCallRepositoryDeleteByUserIdWhenUserIdIsValid() {
            accountChangeArchiveService.deleteByUserId(USER_ID);

            verify(accountChangeArchiveRepository).deleteByUserId(USER_ID);
        }
    }

    @Nested
    class MapToArchive {

        @Test
        void shouldMapAccountChangesActivityToArchive() {
            LocalDateTime before = LocalDateTime.now();
            LocalDateTime activityDate = LocalDateTime.of(2026, 3, 10, 8, 0);
            AccountChangesActivity activity = AccountChangesActivity.builder()
                    .userId(USER_ID)
                    .type(AccountChangesActivityType.PASSWORD_CHANGED)
                    .createdAt(activityDate)
                    .browser("Chrome")
                    .ipAddress("127.0.0.1")
                    .location("Localhost")
                    .build();

            AccountChangeArchive archive = accountChangeArchiveService.mapToArchive(activity);

            assertThat(archive.getUserId()).isEqualTo(USER_ID);
            assertThat(archive.getType()).isEqualTo(AccountChangesActivityType.PASSWORD_CHANGED);
            assertThat(archive.getActivityAccountChangesDate()).isEqualTo(activityDate);
            assertThat(archive.getBrowser()).isEqualTo("Chrome");
            assertThat(archive.getIpAddress()).isEqualTo("127.0.0.1");
            assertThat(archive.getLocation()).isEqualTo("Localhost");
            assertThat(archive.getMoveToArchiveDate()).isBetween(before, LocalDateTime.now());
        }
    }

    @Nested
    class GetAccountChangeArchive {

        @Test
        void shouldReturnArchiveForUserId() {
            AccountChangeArchiveDto dto = new AccountChangeArchiveDto(
                    AccountChangesActivityType.PASSWORD_CHANGED,
                    LocalDateTime.now(),
                    LocalDateTime.of(2026, 3, 10, 14, 30),
                    "Chrome",
                    "192.168.1.100",
                    "Warsaw, Poland"
            );
            when(accountChangeArchiveRepository.findAllByUserIdOrderByIdDesc(USER_ID)).thenReturn(List.of(dto));

            List<AccountChangeArchiveDto> result = accountChangeArchiveService.getAccountChangeArchive(USER_ID);

            assertThat(result).containsExactly(dto);
            verify(accountChangeArchiveRepository).findAllByUserIdOrderByIdDesc(USER_ID);
        }
    }

    @Nested
    class Archive {

        @Test
        void shouldSaveAllArchivesViaRepository() {
            List<AccountChangeArchive> archives = List.of(AccountChangeArchive.builder().build());

            accountChangeArchiveService.archive(archives);

            verify(accountChangeArchiveRepository).saveAll(archives);
        }
    }
}
