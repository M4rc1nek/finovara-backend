package com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.service;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.model.LoginActivity;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.dto.LoginActivityArchiveDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.mapper.LoginActivityArchiveMapper;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.model.LoginActivityArchive;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.repository.LoginActivityArchiveRepository;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class LoginActivityArchiveServiceTest {

    private static final Long USER_ID = 1L;
    private static final int PAGE_SIZE = 5;
    private static final SortType SORT = SortType.values()[0];

    @Mock
    private LoginActivityArchiveRepository loginActivityArchiveRepository;

    @Mock
    private LoginActivityArchiveMapper loginActivityArchiveMapper;

    @Mock
    private LoginActivityArchive loginActivityArchive;

    @Mock
    private LoginActivityArchiveDto loginActivityArchiveDto;

    @InjectMocks
    private LoginActivityArchiveService loginActivityArchiveService;

    @BeforeEach
    void setUp() {
        setField(loginActivityArchiveService, "pageSize", PAGE_SIZE);
    }

    @Nested
    class MapToArchive {

        private LocalDateTime createdAt;
        private LoginActivity activity;

        @BeforeEach
        void setUp() {
            createdAt = LocalDateTime.of(2026, 3, 1, 8, 15);
            activity = LoginActivity.builder()
                    .userId(USER_ID)
                    .type("Other")
                    .browser("Safari")
                    .ipAddress("172.16.0.5")
                    .location("Gdansk")
                    .createdAt(createdAt)
                    .build();
        }

        @Test
        void shouldMapAllFieldsWhenActivityIsValid() {
            LoginActivityArchive result = loginActivityArchiveService.mapToArchive(activity);

            assertEquals(USER_ID, result.getUserId());
            assertEquals(activity.getStatus(), result.getStatus());
            assertEquals("Safari", result.getBrowser());
            assertEquals("172.16.0.5", result.getIpAddress());
            assertEquals("Gdansk", result.getLocation());
            assertEquals(createdAt, result.getCreatedAt());
        }

        @Test
        void shouldSetMoveToArchiveDateToCurrentTimeWhenActivityIsValid() {
            LocalDateTime before = LocalDateTime.now();

            LoginActivityArchive result = loginActivityArchiveService.mapToArchive(activity);

            LocalDateTime after = LocalDateTime.now();
            assertNotNull(result.getMoveToArchiveDate());
            assertFalse(result.getMoveToArchiveDate().isBefore(before));
            assertFalse(result.getMoveToArchiveDate().isAfter(after));
        }

        @Test
        void shouldThrowExceptionWhenActivityIsNull() {
            assertThrows(NullPointerException.class, () -> loginActivityArchiveService.mapToArchive(null));
        }
    }

    @Nested
    class Archive {

        @Test
        void shouldSaveAllArchivesWhenListIsProvided() {
            List<LoginActivityArchive> archives = List.of(loginActivityArchive);

            loginActivityArchiveService.archive(archives);

            verify(loginActivityArchiveRepository).saveAll(archives);
        }

        @Test
        void shouldSaveEmptyListWhenListIsEmpty() {
            List<LoginActivityArchive> archives = List.of();

            loginActivityArchiveService.archive(archives);

            verify(loginActivityArchiveRepository).saveAll(archives);
        }

        @Test
        void shouldThrowExceptionWhenRepositorySaveAllFails() {
            List<LoginActivityArchive> archives = List.of(loginActivityArchive);
            doThrow(new IllegalStateException()).when(loginActivityArchiveRepository).saveAll(archives);

            assertThrows(IllegalStateException.class, () -> loginActivityArchiveService.archive(archives));
        }
    }

    @Nested
    class GetLoginActivityArchive {

        @Test
        void shouldReturnMappedDtosWhenArchivesExist() {
            when(loginActivityArchiveRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of(loginActivityArchive));
            when(loginActivityArchiveMapper.mapToDto(loginActivityArchive)).thenReturn(loginActivityArchiveDto);

            List<LoginActivityArchiveDto> result = loginActivityArchiveService.getLoginActivityArchive(USER_ID, SORT,1);

            assertEquals(List.of(loginActivityArchiveDto), result);
        }

        @Test
        void shouldUseConfiguredPageSizeWhenFetchingArchives() {
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            when(loginActivityArchiveRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of());

            loginActivityArchiveService.getLoginActivityArchive(USER_ID, SORT, 1);

            verify(loginActivityArchiveRepository).findByUserId(eq(USER_ID), pageableCaptor.capture());
            assertEquals(PAGE_SIZE, pageableCaptor.getValue().getPageSize());
        }

        @Test
        void shouldReturnEmptyListWhenNoArchivesExist() {
            when(loginActivityArchiveRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of());

            List<LoginActivityArchiveDto> result = loginActivityArchiveService.getLoginActivityArchive(USER_ID, SORT, 1);

            assertTrue(result.isEmpty());
            verifyNoInteractions(loginActivityArchiveMapper);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(loginActivityArchiveRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> loginActivityArchiveService.getLoginActivityArchive(USER_ID, SORT, 1));
        }

        @Test
        void shouldThrowExceptionWhenMapperFails() {
            when(loginActivityArchiveRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of(loginActivityArchive));
            when(loginActivityArchiveMapper.mapToDto(loginActivityArchive)).thenThrow(new IllegalArgumentException());

            assertThrows(IllegalArgumentException.class, () -> loginActivityArchiveService.getLoginActivityArchive(USER_ID, SORT, 1));
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteArchivesWhenUserIdIsProvided() {
            when(loginActivityArchiveRepository.findAllByUserId(USER_ID)).thenReturn(List.of(loginActivityArchive));

            loginActivityArchiveService.deleteByUserId(USER_ID);

            verify(loginActivityArchiveRepository).findAllByUserId(USER_ID);
            verify(loginActivityArchiveRepository).deleteAll(List.of(loginActivityArchive));
        }

        @Test
        void shouldDeleteArchivesWhenUserIdIsNull() {
            when(loginActivityArchiveRepository.findAllByUserId(null)).thenReturn(List.of());

            loginActivityArchiveService.deleteByUserId(null);

            verify(loginActivityArchiveRepository).findAllByUserId(null);
            verify(loginActivityArchiveRepository).deleteAll(List.of());
        }

        @Test
        void shouldThrowExceptionWhenRepositoryDeleteFails() {
            when(loginActivityArchiveRepository.findAllByUserId(USER_ID)).thenReturn(List.of(loginActivityArchive));
            doThrow(new IllegalStateException()).when(loginActivityArchiveRepository).deleteAll(List.of(loginActivityArchive));

            assertThrows(IllegalStateException.class, () -> loginActivityArchiveService.deleteByUserId(USER_ID));
        }
    }
}