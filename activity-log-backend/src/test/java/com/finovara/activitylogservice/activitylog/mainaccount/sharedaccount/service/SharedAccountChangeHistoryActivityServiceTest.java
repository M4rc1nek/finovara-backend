package com.finovara.activitylogservice.activitylog.mainaccount.sharedaccount.service;

import com.finovara.activitylogservice.activitylog.mainaccount.sharedaccount.dto.SharedAccountChangeHistoryActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.sharedaccount.mapper.SharedAccountActivityMapper;
import com.finovara.activitylogservice.activitylog.mainaccount.sharedaccount.model.SharedAccountChangeHistoryActivity;
import com.finovara.activitylogservice.activitylog.mainaccount.sharedaccount.repository.SharedAccountChangeHistoryActivityRepository;
import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.contracts.mainaccount.activity.event.sharedaccount.SharedAccountChangeHistoryActivityEvent;
import com.finovara.contracts.util.SortType;
import com.finovara.contracts.mainaccount.activity.model.SharedAccountChangeHistoryActivityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SharedAccountChangeHistoryActivityServiceTest {

    @Mock
    private SharedAccountChangeHistoryActivityRepository sharedAccountChangeHistoryActivityRepository;

    @Mock
    private SharedAccountActivityMapper sharedAccountActivityMapper;

    @Mock
    private AuthBackendClient authBackendClient;


    private SharedAccountChangeHistoryActivityService sharedAccountChangeHistoryActivityService;

    @BeforeEach
    void setUp() {
        sharedAccountChangeHistoryActivityService = new SharedAccountChangeHistoryActivityService(
                sharedAccountChangeHistoryActivityRepository,
                sharedAccountActivityMapper,
                authBackendClient
        );
        ReflectionTestUtils.setField(sharedAccountChangeHistoryActivityService, "pageSize", 10);
    }

    @Nested
    class HandleEventTests {

        @Test
        void shouldSaveActivityWhenEventIsValid() {
            SharedAccountChangeHistoryActivityEvent event = mock(SharedAccountChangeHistoryActivityEvent.class);
            when(event.userId()).thenReturn(1L);
            when(event.type()).thenReturn(SharedAccountChangeHistoryActivityType.values()[0]);
            when(event.refundedBalance()).thenReturn(BigDecimal.TEN);
            when(event.coFounderUsername()).thenReturn("cofounder");
            when(event.coFounderEmail()).thenReturn("cofounder@finovara.com");
            when(event.occurredAt()).thenReturn(LocalDateTime.of(2026, 2, 12, 3, 2));

            sharedAccountChangeHistoryActivityService.handleEvent(event);

            ArgumentCaptor<SharedAccountChangeHistoryActivity> captor = ArgumentCaptor.forClass(SharedAccountChangeHistoryActivity.class);
            verify(sharedAccountChangeHistoryActivityRepository, times(1)).save(captor.capture());

            SharedAccountChangeHistoryActivity savedActivity = captor.getValue();
            assertEquals(1L, savedActivity.getUserId());
            assertEquals(SharedAccountChangeHistoryActivityType.values()[0], savedActivity.getType());
            assertEquals(BigDecimal.TEN, savedActivity.getRefundedBalance());
            assertEquals("cofounder", savedActivity.getCoFounderUsername());
            assertEquals("cofounder@finovara.com", savedActivity.getCoFounderEmail());
            assertEquals(LocalDateTime.of(2026, 2, 12, 3, 2), savedActivity.getCreatedAt());
        }

        @Test
        void shouldCallRepositorySaveExactlyOnceWhenEventIsValid() {
            SharedAccountChangeHistoryActivityEvent event = mock(SharedAccountChangeHistoryActivityEvent.class);
            when(event.userId()).thenReturn(2L);

            sharedAccountChangeHistoryActivityService.handleEvent(event);

            verify(sharedAccountChangeHistoryActivityRepository, times(1)).save(any(SharedAccountChangeHistoryActivity.class));
        }

        @Test
        void shouldThrowExceptionWhenEventIsNull() {
            assertThrows(NullPointerException.class, () -> sharedAccountChangeHistoryActivityService.handleEvent(null));
        }

        @Test
        void shouldThrowExceptionWhenRepositorySaveFails() {
            SharedAccountChangeHistoryActivityEvent event = mock(SharedAccountChangeHistoryActivityEvent.class);
            when(event.userId()).thenReturn(3L);
            when(sharedAccountChangeHistoryActivityRepository.save(any(SharedAccountChangeHistoryActivity.class)))
                    .thenThrow(new RuntimeException("database error"));

            assertThrows(RuntimeException.class, () -> sharedAccountChangeHistoryActivityService.handleEvent(event));
        }

        @Test
        void shouldNotCallMapperWhenHandlingEvent() {
            SharedAccountChangeHistoryActivityEvent event = mock(SharedAccountChangeHistoryActivityEvent.class);
            when(event.userId()).thenReturn(4L);

            sharedAccountChangeHistoryActivityService.handleEvent(event);

            verifyNoInteractions(sharedAccountActivityMapper);
        }
    }

    @Nested
    class GetSharedAccountChangeHistoryActivityTests {

        @Test
        void shouldReturnMappedActivitiesWhenActivitiesExist() {
            Long userId = 1L;
            SortType sortType = SortType.values()[0];

            SharedAccountChangeHistoryActivity entity = SharedAccountChangeHistoryActivity.builder()
                    .userId(userId)
                    .build();
            SharedAccountChangeHistoryActivityDto dto = new SharedAccountChangeHistoryActivityDto(
                    SharedAccountChangeHistoryActivityType.values()[0],
                    BigDecimal.TEN,
                    "cofounder",
                    "cofounder@finovara.com",
                    LocalDateTime.now()
            );

            when(sharedAccountChangeHistoryActivityRepository.findByUserId(eq(userId), any(Pageable.class)))
                    .thenReturn(List.of(entity));
            when(sharedAccountActivityMapper.mapToSharedAccountActivity(entity)).thenReturn(dto);

            List<SharedAccountChangeHistoryActivityDto> result = sharedAccountChangeHistoryActivityService.getSharedAccountActivity(userId, sortType);

            assertEquals(1, result.size());
            assertEquals(dto, result.getFirst());
        }

        @Test
        void shouldReturnEmptyListWhenNoActivitiesExist() {
            Long userId = 1L;
            SortType sortType = SortType.values()[0];

            when(sharedAccountChangeHistoryActivityRepository.findByUserId(eq(userId), any(Pageable.class)))
                    .thenReturn(List.of());

            List<SharedAccountChangeHistoryActivityDto> result = sharedAccountChangeHistoryActivityService.getSharedAccountActivity(userId, sortType);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldCallRepositoryWithGivenUserIdWhenFetchingActivities() {
            Long userId = 7L;
            SortType sortType = SortType.values()[0];

            when(sharedAccountChangeHistoryActivityRepository.findByUserId(eq(userId), any(Pageable.class)))
                    .thenReturn(List.of());

            sharedAccountChangeHistoryActivityService.getSharedAccountActivity(userId, sortType);

            verify(sharedAccountChangeHistoryActivityRepository, times(1)).findByUserId(eq(userId), any(Pageable.class));
        }

        @Test
        void shouldMapEachReturnedEntityWhenFetchingActivities() {
            Long userId = 5L;
            SortType sortType = SortType.values()[0];

            SharedAccountChangeHistoryActivity firstEntity = SharedAccountChangeHistoryActivity.builder().userId(userId).build();
            SharedAccountChangeHistoryActivity secondEntity = SharedAccountChangeHistoryActivity.builder().userId(userId).build();

            SharedAccountChangeHistoryActivityDto firstDto = new SharedAccountChangeHistoryActivityDto(
                    SharedAccountChangeHistoryActivityType.values()[0], BigDecimal.ONE, "first", "first@finovara.com", LocalDateTime.now()
            );
            SharedAccountChangeHistoryActivityDto secondDto = new SharedAccountChangeHistoryActivityDto(
                    SharedAccountChangeHistoryActivityType.values()[0], BigDecimal.TWO, "second", "second@finovara.com", LocalDateTime.now()
            );

            when(sharedAccountChangeHistoryActivityRepository.findByUserId(eq(userId), any(Pageable.class)))
                    .thenReturn(List.of(firstEntity, secondEntity));
            when(sharedAccountActivityMapper.mapToSharedAccountActivity(firstEntity)).thenReturn(firstDto);
            when(sharedAccountActivityMapper.mapToSharedAccountActivity(secondEntity)).thenReturn(secondDto);

            sharedAccountChangeHistoryActivityService.getSharedAccountActivity(userId, sortType);

            verify(sharedAccountActivityMapper, times(1)).mapToSharedAccountActivity(firstEntity);
            verify(sharedAccountActivityMapper, times(1)).mapToSharedAccountActivity(secondEntity);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            Long userId = 1L;
            SortType sortType = SortType.values()[0];

            when(sharedAccountChangeHistoryActivityRepository.findByUserId(eq(userId), any(Pageable.class)))
                    .thenThrow(new RuntimeException("query failed"));

            assertThrows(RuntimeException.class, () -> sharedAccountChangeHistoryActivityService.getSharedAccountActivity(userId, sortType));
        }

        @Test
        void shouldThrowExceptionWhenMapperThrowsException() {
            Long userId = 1L;
            SortType sortType = SortType.values()[0];
            SharedAccountChangeHistoryActivity entity = SharedAccountChangeHistoryActivity.builder().userId(userId).build();

            when(sharedAccountChangeHistoryActivityRepository.findByUserId(eq(userId), any(Pageable.class)))
                    .thenReturn(List.of(entity));
            when(sharedAccountActivityMapper.mapToSharedAccountActivity(entity))
                    .thenThrow(new RuntimeException("mapping failed"));

            assertThrows(RuntimeException.class, () -> sharedAccountChangeHistoryActivityService.getSharedAccountActivity(userId, sortType));
        }

        @Test
        void shouldThrowExceptionWhenUserIdIsNull() {
            SortType sortType = SortType.values()[0];

            when(sharedAccountChangeHistoryActivityRepository.findByUserId(eq(null), any(Pageable.class)))
                    .thenThrow(new IllegalArgumentException("userId must not be null"));

            assertThrows(IllegalArgumentException.class, () -> sharedAccountChangeHistoryActivityService.getSharedAccountActivity(null, sortType));
        }
    }

    @Nested
    class GetRepositoryFindByUserIdTests {

        @Test
        void shouldReturnActivitiesWhenRepositoryReturnsResults() {
            Long userId = 1L;
            Pageable pageable = mock(Pageable.class);
            SharedAccountChangeHistoryActivity entity = SharedAccountChangeHistoryActivity.builder().userId(userId).build();

            when(sharedAccountChangeHistoryActivityRepository.findByUserId(userId, pageable)).thenReturn(List.of(entity));

            List<SharedAccountChangeHistoryActivity> result = sharedAccountChangeHistoryActivityService.getRepositoryFindByUserId(userId, pageable);

            assertEquals(1, result.size());
            assertEquals(entity, result.getFirst());
        }

        @Test
        void shouldReturnEmptyListWhenRepositoryReturnsNoResults() {
            Long userId = 1L;
            Pageable pageable = mock(Pageable.class);

            when(sharedAccountChangeHistoryActivityRepository.findByUserId(userId, pageable)).thenReturn(List.of());

            List<SharedAccountChangeHistoryActivity> result = sharedAccountChangeHistoryActivityService.getRepositoryFindByUserId(userId, pageable);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            Long userId = 1L;
            Pageable pageable = mock(Pageable.class);

            when(sharedAccountChangeHistoryActivityRepository.findByUserId(userId, pageable))
                    .thenThrow(new RuntimeException("connection lost"));

            assertThrows(RuntimeException.class, () -> sharedAccountChangeHistoryActivityService.getRepositoryFindByUserId(userId, pageable));
        }
    }

    @Nested
    class MapToDtoTests {

        @Test
        void shouldReturnDtoWhenEntityIsValid() {
            SharedAccountChangeHistoryActivity entity = SharedAccountChangeHistoryActivity.builder().userId(1L).build();
            SharedAccountChangeHistoryActivityDto dto = new SharedAccountChangeHistoryActivityDto(
                    SharedAccountChangeHistoryActivityType.values()[0],
                    BigDecimal.TEN,
                    "cofounder",
                    "cofounder@finovara.com",
                    LocalDateTime.now()
            );

            when(sharedAccountActivityMapper.mapToSharedAccountActivity(entity)).thenReturn(dto);

            SharedAccountChangeHistoryActivityDto result = sharedAccountChangeHistoryActivityService.mapToDto(entity);

            assertEquals(dto, result);
        }

        @Test
        void shouldReturnNullWhenMapperReturnsNull() {
            SharedAccountChangeHistoryActivity entity = SharedAccountChangeHistoryActivity.builder().userId(1L).build();

            when(sharedAccountActivityMapper.mapToSharedAccountActivity(entity)).thenReturn(null);

            SharedAccountChangeHistoryActivityDto result = sharedAccountChangeHistoryActivityService.mapToDto(entity);

            assertNull(result);
        }

        @Test
        void shouldThrowExceptionWhenMapperFails() {
            SharedAccountChangeHistoryActivity entity = SharedAccountChangeHistoryActivity.builder().userId(1L).build();

            when(sharedAccountActivityMapper.mapToSharedAccountActivity(entity))
                    .thenThrow(new RuntimeException("mapping error"));

            assertThrows(RuntimeException.class, () -> sharedAccountChangeHistoryActivityService.mapToDto(entity));
        }
    }

    @Nested
    class DeleteByUserIdTests {

        @Test
        void shouldDeleteActivitiesWhenUserIdIsValid() {
            Long userId = 1L;

            sharedAccountChangeHistoryActivityService.deleteByUserId(userId);

            verify(sharedAccountChangeHistoryActivityRepository, times(1)).deleteByUserId(userId);
        }

        @Test
        void shouldCallRepositoryExactlyOnceWhenDeletingActivities() {
            Long userId = 2L;

            sharedAccountChangeHistoryActivityService.deleteByUserId(userId);

            verify(sharedAccountChangeHistoryActivityRepository, times(1)).deleteByUserId(anyLong());
            verify(sharedAccountChangeHistoryActivityRepository, never()).deleteByUserId(3L);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryDeleteFails() {
            Long userId = 1L;
            doThrow(new RuntimeException("delete failed"))
                    .when(sharedAccountChangeHistoryActivityRepository).deleteByUserId(userId);

            assertThrows(RuntimeException.class, () -> sharedAccountChangeHistoryActivityService.deleteByUserId(userId));
        }

        @Test
        void shouldThrowExceptionWhenUserIdIsNull() {
            doThrow(new IllegalArgumentException("userId must not be null"))
                    .when(sharedAccountChangeHistoryActivityRepository).deleteByUserId(null);

            assertThrows(IllegalArgumentException.class, () -> sharedAccountChangeHistoryActivityService.deleteByUserId(null));
        }

        @Test
        void shouldNotCallMapperWhenDeletingActivities() {
            Long userId = 1L;

            sharedAccountChangeHistoryActivityService.deleteByUserId(userId);

            verifyNoInteractions(sharedAccountActivityMapper);
        }
    }
}