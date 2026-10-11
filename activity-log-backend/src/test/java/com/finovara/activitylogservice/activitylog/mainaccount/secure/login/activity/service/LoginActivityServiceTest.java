package com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.service;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.dto.LoginActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.mapper.LoginActivityMapper;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.model.LoginActivity;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.repository.LoginActivityRepository;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.model.LoginActivityArchive;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.service.LoginActivityArchiveService;
import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.contracts.mainaccount.activity.event.secure.login.activity.LoginActivityEvent;
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

import java.time.LocalDateTime;
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

@ExtendWith(MockitoExtension.class)
class LoginActivityServiceTest {

    private static final Long USER_ID = 1L;
    private static final int PAGE_SIZE = 5;
    private static final SortType SORT = SortType.values()[0];

    @Mock
    private LoginActivityRepository loginActivityRepository;

    @Mock
    private LoginActivityArchiveService loginActivityArchiveService;

    @Mock
    private LoginActivityMapper loginActivityMapper;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private LoginActivity loginActivity;

    @Mock
    private LoginActivityDto loginActivityDto;

    @Mock
    private LoginActivityArchive loginActivityArchive;

    @Mock
    private ConfirmPasswordDto confirmPasswordDto;

    @InjectMocks
    private LoginActivityService loginActivityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loginActivityService, "pageSize", PAGE_SIZE);
    }

    @Nested
    class HandleEvent {

        private LocalDateTime occurredAt;
        private LoginActivityEvent event;

        @BeforeEach
        void setUp() {
            occurredAt = LocalDateTime.of(2026, 4, 10, 18, 45);
            event = mock(LoginActivityEvent.class);
            when(event.userId()).thenReturn(USER_ID);
            when(event.browser()).thenReturn("Edge");
            when(event.ipAddress()).thenReturn("203.0.113.7");
            when(event.location()).thenReturn("Wroclaw");
            when(event.occurredAt()).thenReturn(occurredAt);
        }

        @Test
        void shouldSaveActivityWithEventDataWhenEventIsValid() {
            ArgumentCaptor<LoginActivity> captor = ArgumentCaptor.forClass(LoginActivity.class);

            loginActivityService.handleEvent(event);

            verify(loginActivityRepository).save(captor.capture());
            LoginActivity saved = captor.getValue();
            assertEquals(USER_ID, saved.getUserId());
            assertEquals("Login", saved.getType());
            assertEquals(event.status(), saved.getStatus());
            assertEquals("Edge", saved.getBrowser());
            assertEquals("203.0.113.7", saved.getIpAddress());
            assertEquals("Wroclaw", saved.getLocation());
            assertEquals(occurredAt, saved.getCreatedAt());
        }

        @Test
        void shouldNotArchiveActivitiesWhenActivityCountDoesNotExceedPageSize() {
            when(loginActivityRepository.countActivityLoginByUserId(USER_ID)).thenReturn((long) PAGE_SIZE);

            loginActivityService.handleEvent(event);

            verify(loginActivityArchiveService, never()).archive(any());
            verify(loginActivityRepository, never()).deleteAll(any());
        }

        @Test
        void shouldArchiveAndDeleteActivitiesWhenActivityCountExceedsPageSize() {
            when(loginActivityRepository.countActivityLoginByUserId(USER_ID)).thenReturn(PAGE_SIZE + 1L);
            when(loginActivityRepository.findOldestByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of(loginActivity));
            when(loginActivityArchiveService.mapToArchive(loginActivity)).thenReturn(loginActivityArchive);

            loginActivityService.handleEvent(event);

            verify(loginActivityArchiveService).archive(List.of(loginActivityArchive));
            verify(loginActivityRepository).deleteAll(List.of(loginActivity));
        }

        @Test
        void shouldThrowExceptionWhenRepositorySaveFails() {
            doThrow(new IllegalStateException()).when(loginActivityRepository).save(any(LoginActivity.class));

            assertThrows(IllegalStateException.class, () -> loginActivityService.handleEvent(event));
        }

        @Test
        void shouldNotArchiveActivitiesWhenRepositorySaveFails() {
            doThrow(new IllegalStateException()).when(loginActivityRepository).save(any(LoginActivity.class));

            assertThrows(IllegalStateException.class, () -> loginActivityService.handleEvent(event));

            verifyNoInteractions(loginActivityArchiveService);
        }
    }

    @Nested
    class GetLoginActivity {

        @Test
        void shouldReturnMappedDtosWhenActivitiesExist() {
            when(loginActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of(loginActivity));
            when(loginActivityMapper.mapToDto(loginActivity)).thenReturn(loginActivityDto);

            List<LoginActivityDto> result = loginActivityService.getLoginActivity(USER_ID, SORT);

            assertEquals(List.of(loginActivityDto), result);
        }

        @Test
        void shouldUseConfiguredPageSizeWhenFetchingActivities() {
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            when(loginActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of());

            loginActivityService.getLoginActivity(USER_ID, SORT);

            verify(loginActivityRepository).findByUserId(eq(USER_ID), pageableCaptor.capture());
            assertEquals(PAGE_SIZE, pageableCaptor.getValue().getPageSize());
        }

        @Test
        void shouldReturnEmptyListWhenNoActivitiesExist() {
            when(loginActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of());

            List<LoginActivityDto> result = loginActivityService.getLoginActivity(USER_ID, SORT);

            assertTrue(result.isEmpty());
            verifyNoInteractions(loginActivityMapper);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(loginActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> loginActivityService.getLoginActivity(USER_ID, SORT));
        }

        @Test
        void shouldThrowExceptionWhenMapperFails() {
            when(loginActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of(loginActivity));
            when(loginActivityMapper.mapToDto(loginActivity)).thenThrow(new IllegalArgumentException());

            assertThrows(IllegalArgumentException.class, () -> loginActivityService.getLoginActivity(USER_ID, SORT));
        }
    }

    @Nested
    class ConfirmPassword {

        @Test
        void shouldDelegatePasswordVerificationWhenDataIsProvided() {
            loginActivityService.confirmPassword(USER_ID, confirmPasswordDto);

            verify(authBackendClient).verifyPassword(USER_ID, confirmPasswordDto);
        }

        @Test
        void shouldDelegatePasswordVerificationWhenDtoIsNull() {
            loginActivityService.confirmPassword(USER_ID, null);

            verify(authBackendClient).verifyPassword(USER_ID, null);
        }

        @Test
        void shouldThrowExceptionWhenPasswordVerificationFails() {
            doThrow(new IllegalStateException()).when(authBackendClient).verifyPassword(USER_ID, confirmPasswordDto);

            assertThrows(IllegalStateException.class, () -> loginActivityService.confirmPassword(USER_ID, confirmPasswordDto));
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteActivitiesWhenUserIdIsProvided() {
            loginActivityService.deleteByUserId(USER_ID);

            verify(loginActivityRepository).deleteByUserId(USER_ID);
        }

        @Test
        void shouldDeleteActivitiesWhenUserIdIsNull() {
            loginActivityService.deleteByUserId(null);

            verify(loginActivityRepository).deleteByUserId(null);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryDeleteFails() {
            doThrow(new IllegalStateException()).when(loginActivityRepository).deleteByUserId(USER_ID);

            assertThrows(IllegalStateException.class, () -> loginActivityService.deleteByUserId(USER_ID));
        }
    }
}