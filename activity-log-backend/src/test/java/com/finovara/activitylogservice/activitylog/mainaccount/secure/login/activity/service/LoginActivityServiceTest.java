package com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.service;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.dto.LoginActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.dto.LoginActivityFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.mapper.LoginActivityMapper;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.model.LoginActivity;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.repository.LoginActivityRepository;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.model.LoginActivityArchive;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.service.LoginActivityArchiveService;
import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.contracts.mainaccount.activity.event.secure.login.activity.LoginActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.LoginActivityStatus;
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
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginActivityServiceTest {

    private static final Long USER_ID = 1L;
    private static final int PAGE_SIZE = 5;
    private static final SortType SORT = SortType.NEWEST;
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 4, 10, 18, 45);

    @Mock
    private LoginActivityRepository loginActivityRepository;

    @Mock
    private LoginActivityArchiveService loginActivityArchiveService;

    @Mock
    private LoginActivityMapper loginActivityMapper;

    @Mock
    private AuthBackendClient authBackendClient;

    @InjectMocks
    private LoginActivityService loginActivityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loginActivityService, "pageSize", PAGE_SIZE);
    }

    @Nested
    class HandleEvent {

        private LoginActivityEvent event;
        private LoginActivity oldestActivity;
        private LoginActivityArchive archivedActivity;

        @BeforeEach
        void setUp() {
            event = new LoginActivityEvent(USER_ID, LoginActivityStatus.SUCCESSFUL, "Edge", "203.0.113.7", "Wroclaw", OCCURRED_AT);
            oldestActivity = LoginActivity.builder()
                    .userId(USER_ID)
                    .type("Login")
                    .status(LoginActivityStatus.SUCCESSFUL)
                    .browser("Chrome")
                    .ipAddress("198.51.100.42")
                    .location("Berlin")
                    .createdAt(OCCURRED_AT.minusDays(1))
                    .build();
            archivedActivity = LoginActivityArchive.builder()
                    .userId(USER_ID)
                    .type("Login")
                    .status(LoginActivityStatus.SUCCESSFUL)
                    .browser("Chrome")
                    .ipAddress("198.51.100.42")
                    .location("Berlin")
                    .createdAt(OCCURRED_AT.minusDays(1))
                    .build();
        }

        @Test
        void shouldSaveActivityWhenEventIsValid() {
            ArgumentCaptor<LoginActivity> captor = ArgumentCaptor.forClass(LoginActivity.class);

            loginActivityService.handleEvent(event);

            verify(loginActivityRepository).save(captor.capture());
            LoginActivity saved = captor.getValue();
            assertEquals(USER_ID, saved.getUserId());
            assertEquals("Login", saved.getType());
            assertEquals(LoginActivityStatus.SUCCESSFUL, saved.getStatus());
            assertEquals("Edge", saved.getBrowser());
            assertEquals("203.0.113.7", saved.getIpAddress());
            assertEquals("Wroclaw", saved.getLocation());
            assertEquals(OCCURRED_AT, saved.getCreatedAt());
        }

        @Test
        void shouldNotArchiveActivitiesWhenActivitiesDoNotExceedPageSize() {
            when(loginActivityRepository.findOldestForArchive(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of(oldestActivity));

            loginActivityService.handleEvent(event);

            verify(loginActivityArchiveService, never()).archive(anyList());
            verify(loginActivityRepository, never()).deleteAll(anyList());
        }

        @Test
        void shouldArchiveOldestActivitiesWhenThresholdIsExceeded() {
            List<LoginActivity> oldestActivities = List.of(oldestActivity, oldestActivity, oldestActivity, oldestActivity, oldestActivity, oldestActivity);
            when(loginActivityRepository.findOldestForArchive(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(oldestActivities);
            when(loginActivityArchiveService.mapToArchive(oldestActivity)).thenReturn(archivedActivity);

            loginActivityService.handleEvent(event);

            ArgumentCaptor<List<LoginActivityArchive>> archiveCaptor = ArgumentCaptor.forClass(List.class);
            verify(loginActivityArchiveService).archive(archiveCaptor.capture());
            assertEquals(PAGE_SIZE, archiveCaptor.getValue().size());

            ArgumentCaptor<List<LoginActivity>> deleteCaptor = ArgumentCaptor.forClass(List.class);
            verify(loginActivityRepository).deleteAll(deleteCaptor.capture());
            assertEquals(PAGE_SIZE, deleteCaptor.getValue().size());
        }

        @Test
        void shouldThrowExceptionWhenRepositorySaveFails() {
            doThrow(new IllegalStateException("save failed")).when(loginActivityRepository).save(any(LoginActivity.class));

            assertThrows(IllegalStateException.class, () -> loginActivityService.handleEvent(event));
        }

        @Test
        void shouldNotArchiveActivitiesWhenRepositorySaveFails() {
            doThrow(new IllegalStateException("save failed")).when(loginActivityRepository).save(any(LoginActivity.class));

            assertThrows(IllegalStateException.class, () -> loginActivityService.handleEvent(event));
            verifyNoInteractions(loginActivityArchiveService);
        }
    }

    @Nested
    class FilterLoginActivity {

        @Test
        void shouldFilterActivitiesUsingCreatedDateRangeAndRepository() {
            LoginActivityFilterDto filter = new LoginActivityFilterDto(
                    "Login",
                    LoginActivityStatus.SUCCESSFUL,
                    "Edge",
                    "203.0.113.7",
                    "Wroclaw",
                    LocalDate.of(2026, 4, 10)
            );
            LoginActivity activity = LoginActivity.builder().userId(USER_ID).build();
            LoginActivityDto dto = new LoginActivityDto("Login", LoginActivityStatus.SUCCESSFUL, OCCURRED_AT, "Edge", "203.0.113.7", "Wroclaw");

            when(loginActivityRepository.findFilteredActivities(eq(USER_ID), eq(filter), any(), any(), any(Pageable.class)))
                    .thenReturn(List.of(activity));
            when(loginActivityMapper.mapToDto(activity)).thenReturn(dto);

            List<LoginActivityDto> result = loginActivityService.filterLoginActivity(USER_ID, filter, 1);

            assertEquals(List.of(dto), result);
            verify(loginActivityRepository).findFilteredActivities(eq(USER_ID), eq(filter), any(), any(), any(Pageable.class));
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteActivitiesWhenUserIdIsProvided() {
            when(loginActivityRepository.findAllByUserId(USER_ID)).thenReturn(List.of(LoginActivity.builder().userId(USER_ID).build()));

            loginActivityService.deleteByUserId(USER_ID);

            verify(loginActivityRepository).findAllByUserId(USER_ID);
            verify(loginActivityRepository).deleteAll(anyList());
        }

        @Test
        void shouldThrowExceptionWhenRepositoryDeleteFails() {
            List<LoginActivity> activities = List.of(LoginActivity.builder().userId(USER_ID).build());
            when(loginActivityRepository.findAllByUserId(USER_ID)).thenReturn(activities);
            doThrow(new IllegalStateException("delete failed")).when(loginActivityRepository).deleteAll(activities);

            assertThrows(IllegalStateException.class, () -> loginActivityService.deleteByUserId(USER_ID));
        }
    }

    @Nested
    class GetLoginActivity {

        @Test
        void shouldReturnMappedDtosWhenActivitiesExist() {
            LoginActivity activity = LoginActivity.builder().userId(USER_ID).type("Login").status(LoginActivityStatus.SUCCESSFUL).build();
            when(loginActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of(activity));
            when(loginActivityMapper.mapToDto(activity)).thenReturn(new LoginActivityDto("Login", LoginActivityStatus.SUCCESSFUL, OCCURRED_AT, "Edge", "203.0.113.7", "Wroclaw"));

            List<LoginActivityDto> result = loginActivityService.getLoginActivity(USER_ID, SORT, 1);

            assertEquals(List.of(new LoginActivityDto("Login", LoginActivityStatus.SUCCESSFUL, OCCURRED_AT, "Edge", "203.0.113.7", "Wroclaw")), result);
        }

        @Test
        void shouldUseConfiguredPageSizeWhenFetchingActivities() {
            when(loginActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of());

            loginActivityService.getLoginActivity(USER_ID, SORT, 1);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(loginActivityRepository).findByUserId(eq(USER_ID), captor.capture());
            assertEquals(PAGE_SIZE, captor.getValue().getPageSize());
        }

        @Test
        void shouldReturnEmptyListWhenNoActivitiesExist() {
            when(loginActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of());

            List<LoginActivityDto> result = loginActivityService.getLoginActivity(USER_ID, SORT, 1);

            assertTrue(result.isEmpty());
            verifyNoInteractions(loginActivityMapper);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFindFails() {
            when(loginActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenThrow(new IllegalStateException("query failed"));

            assertThrows(IllegalStateException.class, () -> loginActivityService.getLoginActivity(USER_ID, SORT, 1));
        }

        @Test
        void shouldThrowExceptionWhenMapperFails() {
            LoginActivity activity = LoginActivity.builder().userId(USER_ID).type("Login").status(LoginActivityStatus.SUCCESSFUL).build();
            when(loginActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of(activity));
            when(loginActivityMapper.mapToDto(activity)).thenThrow(new IllegalArgumentException("mapping failed"));

            assertThrows(IllegalArgumentException.class, () -> loginActivityService.getLoginActivity(USER_ID, SORT, 1));
        }
    }

    @Nested
    class ConfirmPassword {

        @Test
        void shouldDelegatePasswordVerificationWhenDataIsProvided() {
            ConfirmPasswordDto dto = new ConfirmPasswordDto("Secret123");

            loginActivityService.confirmPassword(USER_ID, dto);

            verify(authBackendClient).verifyPassword(USER_ID, dto);
        }

        @Test
        void shouldDelegatePasswordVerificationWhenDtoIsNull() {
            loginActivityService.confirmPassword(USER_ID, null);

            verify(authBackendClient).verifyPassword(USER_ID, null);
        }

        @Test
        void shouldThrowExceptionWhenPasswordVerificationFails() {
            ConfirmPasswordDto dto = new ConfirmPasswordDto("Secret123");
            doThrow(new IllegalStateException("verification failed")).when(authBackendClient)
                    .verifyPassword(USER_ID, dto);

            assertThrows(IllegalStateException.class, () -> loginActivityService.confirmPassword(USER_ID, dto));
        }
    }

    @Nested
    class DeleteByUserIdFromRepository {

        @Test
        void shouldDeleteActivitiesWhenUserIdIsProvided() {
            List<LoginActivity> activities = List.of(new LoginActivity());
            when(loginActivityRepository.findAllByUserId(USER_ID)).thenReturn(activities);

            loginActivityService.deleteByUserId(USER_ID);

            verify(loginActivityRepository).findAllByUserId(USER_ID);
            verify(loginActivityRepository).deleteAll(activities);
        }

        @Test
        void shouldDeleteActivitiesWhenUserIdIsNull() {
            when(loginActivityRepository.findAllByUserId(null)).thenReturn(List.of());

            loginActivityService.deleteByUserId(null);

            verify(loginActivityRepository).findAllByUserId(null);
            verify(loginActivityRepository).deleteAll(List.of());
        }

        @Test
        void shouldThrowExceptionWhenRepositoryDeleteFails() {
            List<LoginActivity> activities = List.of(new LoginActivity());
            when(loginActivityRepository.findAllByUserId(USER_ID)).thenReturn(activities);
            doThrow(new IllegalStateException("delete failed")).when(loginActivityRepository).deleteAll(activities);

            assertThrows(IllegalStateException.class, () -> loginActivityService.deleteByUserId(USER_ID));
        }
    }
}
