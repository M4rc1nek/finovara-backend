package com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.service;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.model.AccountChangeActivity;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.dto.AccountChangeArchiveDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.mapper.AccountChangeArchiveMapper;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.model.AccountChangeArchive;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.repository.AccountChangeArchiveRepository;
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
class AccountChangeArchiveServiceTest {

    private static final Long USER_ID = 1L;
    private static final int PAGE_SIZE = 5;
    private static final SortType SORT = SortType.values()[0];

    @Mock
    private AccountChangeArchiveRepository accountChangeArchiveRepository;

    @Mock
    private AccountChangeArchiveMapper accountChangeArchiveMapper;

    @Mock
    private AccountChangeArchive accountChangeArchive;

    @Mock
    private AccountChangeArchiveDto accountChangeArchiveDto;

    @InjectMocks
    private AccountChangeArchiveService accountChangeArchiveService;

    @BeforeEach
    void setUp() {
        setField(accountChangeArchiveService, "pageSize", PAGE_SIZE);
    }

    @Nested
    class MapToArchive {

        private LocalDateTime createdAt;
        private AccountChangeActivity activity;

        @BeforeEach
        void setUp() {
            createdAt = LocalDateTime.of(2026, 1, 15, 10, 30);
            activity = AccountChangeActivity.builder()
                    .userId(USER_ID)
                    .browser("Firefox")
                    .ipAddress("10.0.0.1")
                    .location("Warsaw")
                    .createdAt(createdAt)
                    .build();
        }

        @Test
        void shouldMapAllFieldsWhenActivityIsValid() {
            AccountChangeArchive result = accountChangeArchiveService.mapToArchive(activity);

            assertEquals(USER_ID, result.getUserId());
            assertEquals(activity.getType(), result.getType());
            assertEquals("Firefox", result.getBrowser());
            assertEquals("10.0.0.1", result.getIpAddress());
            assertEquals("Warsaw", result.getLocation());
            assertEquals(createdAt, result.getCreatedAt());
        }

        @Test
        void shouldSetMoveToArchiveDateToCurrentTimeWhenActivityIsValid() {
            LocalDateTime before = LocalDateTime.now();

            AccountChangeArchive result = accountChangeArchiveService.mapToArchive(activity);

            LocalDateTime after = LocalDateTime.now();
            assertNotNull(result.getMoveToArchiveDate());
            assertFalse(result.getMoveToArchiveDate().isBefore(before));
            assertFalse(result.getMoveToArchiveDate().isAfter(after));
        }

        @Test
        void shouldKeepOriginalCreatedAtWhenActivityIsValid() {
            AccountChangeArchive result = accountChangeArchiveService.mapToArchive(activity);

            assertEquals(activity.getCreatedAt(), result.getCreatedAt());
            assertTrue(result.getMoveToArchiveDate().isAfter(result.getCreatedAt()));
        }

        @Test
        void shouldMapNullFieldsWhenActivityHasNullOptionalFields() {
            AccountChangeActivity emptyActivity = AccountChangeActivity.builder()
                    .userId(USER_ID)
                    .build();

            AccountChangeArchive result = accountChangeArchiveService.mapToArchive(emptyActivity);

            assertEquals(USER_ID, result.getUserId());
            assertEquals(null, result.getBrowser());
            assertEquals(null, result.getIpAddress());
            assertEquals(null, result.getLocation());
            assertEquals(null, result.getCreatedAt());
        }

        @Test
        void shouldThrowExceptionWhenActivityIsNull() {
            assertThrows(NullPointerException.class, () -> accountChangeArchiveService.mapToArchive(null));
        }
    }

    @Nested
    class Archive {

        @Test
        void shouldSaveAllArchivesWhenListIsProvided() {
            List<AccountChangeArchive> archives = List.of(accountChangeArchive);

            accountChangeArchiveService.archive(archives);

            verify(accountChangeArchiveRepository).saveAll(archives);
        }

        @Test
        void shouldSaveEmptyListWhenListIsEmpty() {
            List<AccountChangeArchive> archives = List.of();

            accountChangeArchiveService.archive(archives);

            verify(accountChangeArchiveRepository).saveAll(archives);
        }

        @Test
        void shouldThrowExceptionWhenRepositorySaveAllFails() {
            List<AccountChangeArchive> archives = List.of(accountChangeArchive);
            doThrow(new IllegalStateException()).when(accountChangeArchiveRepository).saveAll(archives);

            assertThrows(IllegalStateException.class, () -> accountChangeArchiveService.archive(archives));
        }
    }

    @Nested
    class GetAccountChangeArchive {

        @Test
        void shouldReturnMappedDtosWhenArchivesExist() {
            when(accountChangeArchiveRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of(accountChangeArchive));
            when(accountChangeArchiveMapper.mapToDto(accountChangeArchive)).thenReturn(accountChangeArchiveDto);

            List<AccountChangeArchiveDto> result = accountChangeArchiveService.getAccountChangeArchive(USER_ID, SORT);

            assertEquals(List.of(accountChangeArchiveDto), result);
        }

        @Test
        void shouldUseConfiguredPageSizeWhenFetchingArchives() {
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            when(accountChangeArchiveRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of());

            accountChangeArchiveService.getAccountChangeArchive(USER_ID, SORT);

            verify(accountChangeArchiveRepository).findByUserId(eq(USER_ID), pageableCaptor.capture());
            assertEquals(PAGE_SIZE, pageableCaptor.getValue().getPageSize());
        }

        @Test
        void shouldReturnEmptyListWhenNoArchivesExist() {
            when(accountChangeArchiveRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of());

            List<AccountChangeArchiveDto> result = accountChangeArchiveService.getAccountChangeArchive(USER_ID, SORT);

            assertTrue(result.isEmpty());
            verifyNoInteractions(accountChangeArchiveMapper);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(accountChangeArchiveRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> accountChangeArchiveService.getAccountChangeArchive(USER_ID, SORT));
        }

        @Test
        void shouldThrowExceptionWhenMapperFails() {
            when(accountChangeArchiveRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of(accountChangeArchive));
            when(accountChangeArchiveMapper.mapToDto(accountChangeArchive)).thenThrow(new IllegalArgumentException());

            assertThrows(IllegalArgumentException.class, () -> accountChangeArchiveService.getAccountChangeArchive(USER_ID, SORT));
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteArchivesWhenUserIdIsProvided() {
            accountChangeArchiveService.deleteByUserId(USER_ID);

            verify(accountChangeArchiveRepository).deleteByUserId(USER_ID);
        }

        @Test
        void shouldDeleteArchivesWhenUserIdIsNull() {
            accountChangeArchiveService.deleteByUserId(null);

            verify(accountChangeArchiveRepository).deleteByUserId(null);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryDeleteFails() {
            doThrow(new IllegalStateException()).when(accountChangeArchiveRepository).deleteByUserId(USER_ID);

            assertThrows(IllegalStateException.class, () -> accountChangeArchiveService.deleteByUserId(USER_ID));
        }
    }
}