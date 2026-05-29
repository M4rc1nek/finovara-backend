package com.finovara.activityservice.activity_log.accountactivity.secure.accountchange.activity.service;

import com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.activity.dto.AccountChangesActivityDto;
import com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.activity.model.AccountChangesActivity;
import com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.activity.repository.AccountChangesActivityRepository;
import com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.activity.service.AccountChangesActivityService;
import com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.archive.model.AccountChangeArchive;
import com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.archive.service.AccountChangeArchiveService;
import com.finovara.contracts.event.secure.accountchange.activity.AccountChangesActivityEvent;
import com.finovara.contracts.model.activity.AccountChangesActivityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountChangesActivityServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 5, 25, 16, 0);

    @Mock
    private AccountChangesActivityRepository accountChangesActivityRepository;

    @Mock
    private AccountChangeArchiveService archiveService;

    @InjectMocks
    private AccountChangesActivityService accountChangesActivityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(accountChangesActivityService, "pageSize", 10);
    }

    @Test
    void shouldSaveActivityFromEventAndNotArchiveWhenBelowThreshold() {
        AccountChangesActivityEvent event = event();
        when(accountChangesActivityRepository.countAccountChangesByUserId(USER_ID)).thenReturn(9L);

        accountChangesActivityService.handleEvent(event);

        ArgumentCaptor<AccountChangesActivity> captor = ArgumentCaptor.forClass(AccountChangesActivity.class);
        verify(accountChangesActivityRepository).save(captor.capture());
        verify(archiveService, never()).archive(any());

        AccountChangesActivity activity = captor.getValue();
        assertThat(activity.getUserId()).isEqualTo(USER_ID);
        assertThat(activity.getType()).isEqualTo(event.type());
        assertThat(activity.getBrowser()).isEqualTo(event.browser());
        assertThat(activity.getIpAddress()).isEqualTo(event.ipAddress());
        assertThat(activity.getLocation()).isEqualTo(event.location());
        assertThat(activity.getCreatedAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    void shouldArchiveOldestActivitiesWhenThresholdExceeded() {
        AccountChangesActivityEvent event = event();
        AccountChangesActivity oldest = AccountChangesActivity.builder().id(1L).userId(USER_ID).build();
        AccountChangeArchive archive = AccountChangeArchive.builder().userId(USER_ID).build();

        when(accountChangesActivityRepository.countAccountChangesByUserId(USER_ID)).thenReturn(11L);
        when(accountChangesActivityRepository.findFewByUserId(eq(USER_ID), any(PageRequest.class))).thenReturn(List.of(oldest));
        when(archiveService.mapToArchive(oldest)).thenReturn(archive);

        accountChangesActivityService.handleEvent(event);

        verify(accountChangesActivityRepository).save(any(AccountChangesActivity.class));
        verify(archiveService).archive(List.of(archive));
        verify(accountChangesActivityRepository).deleteAll(List.of(oldest));
    }

    @Test
    void shouldReturnAccountChangeDtos() {
        AccountChangesActivityDto dto = new AccountChangesActivityDto(
                AccountChangesActivityType.PASSWORD_CHANGED,
                OCCURRED_AT,
                "Chrome",
                "127.0.0.1",
                "Localhost"
        );
        when(accountChangesActivityRepository.findByUserIdOrderByIdDesc(USER_ID)).thenReturn(List.of(dto));

        List<AccountChangesActivityDto> result = accountChangesActivityService.getAccountChangesActivity(USER_ID);

        assertThat(result).containsExactly(dto);
        verify(accountChangesActivityRepository).findByUserIdOrderByIdDesc(USER_ID);
    }

    private AccountChangesActivityEvent event() {
        return new AccountChangesActivityEvent(
                USER_ID,
                AccountChangesActivityType.PASSWORD_CHANGED,
                "Chrome",
                "127.0.0.1",
                "Localhost",
                OCCURRED_AT
        );
    }
}
