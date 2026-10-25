package com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.service;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.dto.AccountChangeActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.mapper.AccountChangeActivityMapper;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.model.AccountChangeActivity;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.repository.AccountChangeActivityRepository;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.model.AccountChangeArchive;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.service.AccountChangeArchiveService;
import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.contracts.mainaccount.activity.event.secure.accountchange.activity.AccountChangeActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.AccountChangeActivityType;
import com.finovara.contracts.user.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.util.SortType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class AccountChangeActivityServiceTest {

    private static final Long USER_ID = 1L;
    private static final int PAGE_SIZE = 5;
    private static final int PAGE = 0;
    private static final SortType SORT = SortType.values()[0];

    @Mock
    private AccountChangeActivityRepository accountChangeActivityRepository;

    @Mock
    private AccountChangeArchiveService accountChangeArchiveService;

    @Mock
    private AccountChangeActivityMapper accountChangeActivityMapper;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private AccountChangeActivity accountChangeActivity;

    @Mock
    private AccountChangeActivityDto accountChangeActivityDto;

    @Mock
    private AccountChangeArchive accountChangeArchive;

    @Mock
    private ConfirmPasswordDto confirmPasswordDto;

    @InjectMocks
    private AccountChangeActivityService accountChangeActivityService;

    @BeforeEach
    void setUp() {
        setField(accountChangeActivityService, "pageSize", PAGE_SIZE);
    }

    @Nested
    class HandleEvent {

        private LocalDateTime occurredAt;
        private AccountChangeActivityEvent event;

        @BeforeEach
        void setUp() {
            occurredAt = LocalDateTime.of(2026, 2, 20, 12, 0);
            event = mock(AccountChangeActivityEvent.class);
            when(event.userId()).thenReturn(USER_ID);
            when(event.type()).thenReturn(AccountChangeActivityType.PASSWORD_CHANGED);
            when(event.browser()).thenReturn("Chrome");
            when(event.ipAddress()).thenReturn("192.168.0.1");
            when(event.location()).thenReturn("Krakow");
            when(event.occurredAt()).thenReturn(occurredAt);
        }

        @Test
        void shouldSaveActivityWithEventDataWhenEventIsValid() {
            ArgumentCaptor<AccountChangeActivity> captor = ArgumentCaptor.forClass(AccountChangeActivity.class);

            accountChangeActivityService.handleEvent(event);

            verify(accountChangeActivityRepository).save(captor.capture());
            AccountChangeActivity saved = captor.getValue();
            assertEquals(USER_ID, saved.getUserId());
            assertEquals(event.type(), saved.getType());
            assertEquals("Chrome", saved.getBrowser());
            assertEquals("192.168.0.1", saved.getIpAddress());
            assertEquals("Krakow", saved.getLocation());
            assertEquals(occurredAt, saved.getCreatedAt());
        }

        @Test
        void shouldNotArchiveActivitiesWhenActivityCountDoesNotExceedPageSize() {
            List<AccountChangeActivity> oldestActivities = new ArrayList<>();
            for (int i = 0; i < PAGE_SIZE; i++) {
                oldestActivities.add(accountChangeActivity);
            }
            when(accountChangeActivityRepository.findOldestForArchive(eq(USER_ID), any(Pageable.class))).thenReturn(oldestActivities);

            accountChangeActivityService.handleEvent(event);

            verify(accountChangeArchiveService, never()).archive(any());
            verify(accountChangeActivityRepository, never()).deleteAll(any());
        }

        @Test
        void shouldArchiveAndDeleteActivitiesWhenActivityCountExceedsPageSize() {
            List<AccountChangeActivity> oldestActivities = new ArrayList<>();
            for (int i = 0; i < PAGE_SIZE + 1; i++) {
                oldestActivities.add(accountChangeActivity);
            }
            when(accountChangeActivityRepository.findOldestForArchive(eq(USER_ID), any(Pageable.class))).thenReturn(oldestActivities);
            when(accountChangeArchiveService.mapToArchive(accountChangeActivity)).thenReturn(accountChangeArchive);

            accountChangeActivityService.handleEvent(event);

            ArgumentCaptor<List<AccountChangeArchive>> archiveCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<List<AccountChangeActivity>> deleteCaptor = ArgumentCaptor.forClass(List.class);
            verify(accountChangeArchiveService).archive(archiveCaptor.capture());
            verify(accountChangeActivityRepository).deleteAll(deleteCaptor.capture());
            assertEquals(PAGE_SIZE, archiveCaptor.getValue().size());
            assertEquals(PAGE_SIZE, deleteCaptor.getValue().size());
        }

        @Test
        void shouldThrowExceptionWhenRepositorySaveFails() {
            doThrow(new IllegalStateException()).when(accountChangeActivityRepository).save(any(AccountChangeActivity.class));

            assertThrows(IllegalStateException.class, () -> accountChangeActivityService.handleEvent(event));
        }

        @Test
        void shouldNotArchiveActivitiesWhenRepositorySaveFails() {
            doThrow(new IllegalStateException()).when(accountChangeActivityRepository).save(any(AccountChangeActivity.class));

            assertThrows(IllegalStateException.class, () -> accountChangeActivityService.handleEvent(event));

            verifyNoInteractions(accountChangeArchiveService);
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteActivitiesWhenUserIdIsProvided() {
            when(accountChangeActivityRepository.findAllByUserId(USER_ID)).thenReturn(List.of(accountChangeActivity));

            accountChangeActivityService.deleteByUserId(USER_ID);

            verify(accountChangeActivityRepository).findAllByUserId(USER_ID);
            verify(accountChangeActivityRepository).deleteAll(List.of(accountChangeActivity));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryDeleteFails() {
            when(accountChangeActivityRepository.findAllByUserId(USER_ID)).thenReturn(List.of(accountChangeActivity));
            doThrow(new IllegalStateException()).when(accountChangeActivityRepository).deleteAll(List.of(accountChangeActivity));

            assertThrows(IllegalStateException.class, () -> accountChangeActivityService.deleteByUserId(USER_ID));
        }
    }

    @Nested
    class GetAccountChangeActivity {

        @Test
        void shouldReturnMappedDtosWhenActivitiesExist() {
            when(accountChangeActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of(accountChangeActivity));
            when(accountChangeActivityMapper.mapToDto(accountChangeActivity)).thenReturn(accountChangeActivityDto);

            List<AccountChangeActivityDto> result = accountChangeActivityService.getAccountChangeActivity(USER_ID, SORT, PAGE);

            assertEquals(List.of(accountChangeActivityDto), result);
        }

        @Test
        void shouldUseConfiguredPageSizeWhenFetchingActivities() {
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            when(accountChangeActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of());

            accountChangeActivityService.getAccountChangeActivity(USER_ID, SORT, PAGE);

            verify(accountChangeActivityRepository).findByUserId(eq(USER_ID), pageableCaptor.capture());
            assertEquals(PAGE_SIZE, pageableCaptor.getValue().getPageSize());
        }

        @Test
        void shouldReturnEmptyListWhenNoActivitiesExist() {
            when(accountChangeActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of());

            List<AccountChangeActivityDto> result = accountChangeActivityService.getAccountChangeActivity(USER_ID, SORT, PAGE);

            assertTrue(result.isEmpty());
            verifyNoInteractions(accountChangeActivityMapper);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(accountChangeActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> accountChangeActivityService.getAccountChangeActivity(USER_ID, SORT, PAGE));
        }

        @Test
        void shouldThrowExceptionWhenMapperFails() {
            when(accountChangeActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of(accountChangeActivity));
            when(accountChangeActivityMapper.mapToDto(accountChangeActivity)).thenThrow(new IllegalArgumentException());

            assertThrows(IllegalArgumentException.class, () -> accountChangeActivityService.getAccountChangeActivity(USER_ID, SORT, PAGE));
        }
    }

    @Nested
    class ConfirmPassword {

        @Test
        void shouldDelegatePasswordVerificationWhenDataIsProvided() {
            accountChangeActivityService.confirmPassword(USER_ID, confirmPasswordDto);

            verify(authBackendClient).verifyPassword(USER_ID, confirmPasswordDto);
        }

        @Test
        void shouldDelegatePasswordVerificationWhenDtoIsNull() {
            accountChangeActivityService.confirmPassword(USER_ID, null);

            verify(authBackendClient).verifyPassword(USER_ID, null);
        }

        @Test
        void shouldThrowExceptionWhenPasswordVerificationFails() {
            doThrow(new IllegalStateException()).when(authBackendClient).verifyPassword(USER_ID, confirmPasswordDto);

            assertThrows(IllegalStateException.class, () -> accountChangeActivityService.confirmPassword(USER_ID, confirmPasswordDto));
        }
    }

    @Nested
    class DeleteByUserIdFromRepository {

        @Test
        void shouldDeleteActivitiesWhenUserIdIsProvided() {
            List<AccountChangeActivity> activities = List.of(accountChangeActivity);
            when(accountChangeActivityRepository.findAllByUserId(USER_ID)).thenReturn(activities);

            accountChangeActivityService.deleteByUserId(USER_ID);

            verify(accountChangeActivityRepository).findAllByUserId(USER_ID);
            verify(accountChangeActivityRepository).deleteAll(activities);
        }

        @Test
        void shouldDeleteActivitiesWhenUserIdIsNull() {
            when(accountChangeActivityRepository.findAllByUserId(null)).thenReturn(List.of());

            accountChangeActivityService.deleteByUserId(null);

            verify(accountChangeActivityRepository).findAllByUserId(null);
            verify(accountChangeActivityRepository).deleteAll(List.of());
        }

        @Test
        void shouldThrowExceptionWhenRepositoryDeleteFails() {
            List<AccountChangeActivity> activities = List.of(accountChangeActivity);
            when(accountChangeActivityRepository.findAllByUserId(USER_ID)).thenReturn(activities);
            doThrow(new IllegalStateException()).when(accountChangeActivityRepository).deleteAll(activities);

            assertThrows(IllegalStateException.class, () -> accountChangeActivityService.deleteByUserId(USER_ID));
        }
    }
}
