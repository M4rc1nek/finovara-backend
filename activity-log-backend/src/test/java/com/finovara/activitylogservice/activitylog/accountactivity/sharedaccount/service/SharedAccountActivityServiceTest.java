package com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.service;

import com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.dto.SharedAccountActivityDto;
import com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.mapper.SharedAccountActivityMapper;
import com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.model.SharedAccountActivity;
import com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.repository.SharedAccountActivityRepository;
import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.contracts.event.activity.sharedaccount.SharedAccountActivityEvent;
import com.finovara.contracts.model.SortType;
import com.finovara.contracts.model.activity.SharedAccountActivityType;
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
class SharedAccountActivityServiceTest {

    @Mock
    private SharedAccountActivityRepository sharedAccountActivityRepository;

    @Mock
    private SharedAccountActivityMapper sharedAccountActivityMapper;

    @Mock
    private AuthBackendClient authBackendClient;


    private SharedAccountActivityService sharedAccountActivityService;

    @BeforeEach
    void setUp() {
        sharedAccountActivityService = new SharedAccountActivityService(
                sharedAccountActivityRepository,
                sharedAccountActivityMapper,
                authBackendClient
        );
        ReflectionTestUtils.setField(sharedAccountActivityService, "pageSize", 10);
    }

    @Nested
    class HandleEventTests {

        @Test
        void shouldSaveActivityWhenEventIsValid() {
            SharedAccountActivityEvent event = mock(SharedAccountActivityEvent.class);
            when(event.userId()).thenReturn(1L);
            when(event.type()).thenReturn(SharedAccountActivityType.values()[0]);
            when(event.refundedBalance()).thenReturn(BigDecimal.TEN);
            when(event.coFounderUsername()).thenReturn("cofounder");
            when(event.coFounderEmail()).thenReturn("cofounder@finovara.com");
            when(event.occurredAt()).thenReturn(LocalDateTime.of(2026, 2, 12, 3, 2));

            sharedAccountActivityService.handleEvent(event);

            ArgumentCaptor<SharedAccountActivity> captor = ArgumentCaptor.forClass(SharedAccountActivity.class);
            verify(sharedAccountActivityRepository, times(1)).save(captor.capture());

            SharedAccountActivity savedActivity = captor.getValue();
            assertEquals(1L, savedActivity.getUserId());
            assertEquals(SharedAccountActivityType.values()[0], savedActivity.getType());
            assertEquals(BigDecimal.TEN, savedActivity.getRefundedBalance());
            assertEquals("cofounder", savedActivity.getCoFounderUsername());
            assertEquals("cofounder@finovara.com", savedActivity.getCoFounderEmail());
            assertEquals(LocalDateTime.of(2026, 2, 12, 3, 2), savedActivity.getCreatedAt());
        }

        @Test
        void shouldCallRepositorySaveExactlyOnceWhenEventIsValid() {
            SharedAccountActivityEvent event = mock(SharedAccountActivityEvent.class);
            when(event.userId()).thenReturn(2L);

            sharedAccountActivityService.handleEvent(event);

            verify(sharedAccountActivityRepository, times(1)).save(any(SharedAccountActivity.class));
        }

        @Test
        void shouldThrowExceptionWhenEventIsNull() {
            assertThrows(NullPointerException.class, () -> sharedAccountActivityService.handleEvent(null));
        }

        @Test
        void shouldThrowExceptionWhenRepositorySaveFails() {
            SharedAccountActivityEvent event = mock(SharedAccountActivityEvent.class);
            when(event.userId()).thenReturn(3L);
            when(sharedAccountActivityRepository.save(any(SharedAccountActivity.class)))
                    .thenThrow(new RuntimeException("database error"));

            assertThrows(RuntimeException.class, () -> sharedAccountActivityService.handleEvent(event));
        }

        @Test
        void shouldNotCallMapperWhenHandlingEvent() {
            SharedAccountActivityEvent event = mock(SharedAccountActivityEvent.class);
            when(event.userId()).thenReturn(4L);

            sharedAccountActivityService.handleEvent(event);

            verifyNoInteractions(sharedAccountActivityMapper);
        }
    }

    @Nested
    class GetSharedAccountActivityTests {

        @Test
        void shouldReturnMappedActivitiesWhenActivitiesExist() {
            Long userId = 1L;
            SortType sortType = SortType.values()[0];

            SharedAccountActivity entity = SharedAccountActivity.builder()
                    .userId(userId)
                    .build();
            SharedAccountActivityDto dto = new SharedAccountActivityDto(
                    SharedAccountActivityType.values()[0],
                    BigDecimal.TEN,
                    "cofounder",
                    "cofounder@finovara.com",
                    LocalDateTime.now()
            );

            when(sharedAccountActivityRepository.findByUserId(eq(userId), any(Pageable.class)))
                    .thenReturn(List.of(entity));
            when(sharedAccountActivityMapper.mapToSharedAccountActivity(entity)).thenReturn(dto);

            List<SharedAccountActivityDto> result = sharedAccountActivityService.getSharedAccountActivity(userId, sortType);

            assertEquals(1, result.size());
            assertEquals(dto, result.get(0));
        }

        @Test
        void shouldReturnEmptyListWhenNoActivitiesExist() {
            Long userId = 1L;
            SortType sortType = SortType.values()[0];

            when(sharedAccountActivityRepository.findByUserId(eq(userId), any(Pageable.class)))
                    .thenReturn(List.of());

            List<SharedAccountActivityDto> result = sharedAccountActivityService.getSharedAccountActivity(userId, sortType);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldCallRepositoryWithGivenUserIdWhenFetchingActivities() {
            Long userId = 7L;
            SortType sortType = SortType.values()[0];

            when(sharedAccountActivityRepository.findByUserId(eq(userId), any(Pageable.class)))
                    .thenReturn(List.of());

            sharedAccountActivityService.getSharedAccountActivity(userId, sortType);

            verify(sharedAccountActivityRepository, times(1)).findByUserId(eq(userId), any(Pageable.class));
        }

        @Test
        void shouldMapEachReturnedEntityWhenFetchingActivities() {
            Long userId = 5L;
            SortType sortType = SortType.values()[0];

            SharedAccountActivity firstEntity = SharedAccountActivity.builder().userId(userId).build();
            SharedAccountActivity secondEntity = SharedAccountActivity.builder().userId(userId).build();

            SharedAccountActivityDto firstDto = new SharedAccountActivityDto(
                    SharedAccountActivityType.values()[0], BigDecimal.ONE, "first", "first@finovara.com", LocalDateTime.now()
            );
            SharedAccountActivityDto secondDto = new SharedAccountActivityDto(
                    SharedAccountActivityType.values()[0], BigDecimal.TWO, "second", "second@finovara.com", LocalDateTime.now()
            );

            when(sharedAccountActivityRepository.findByUserId(eq(userId), any(Pageable.class)))
                    .thenReturn(List.of(firstEntity, secondEntity));
            when(sharedAccountActivityMapper.mapToSharedAccountActivity(firstEntity)).thenReturn(firstDto);
            when(sharedAccountActivityMapper.mapToSharedAccountActivity(secondEntity)).thenReturn(secondDto);

            sharedAccountActivityService.getSharedAccountActivity(userId, sortType);

            verify(sharedAccountActivityMapper, times(1)).mapToSharedAccountActivity(firstEntity);
            verify(sharedAccountActivityMapper, times(1)).mapToSharedAccountActivity(secondEntity);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            Long userId = 1L;
            SortType sortType = SortType.values()[0];

            when(sharedAccountActivityRepository.findByUserId(eq(userId), any(Pageable.class)))
                    .thenThrow(new RuntimeException("query failed"));

            assertThrows(RuntimeException.class, () -> sharedAccountActivityService.getSharedAccountActivity(userId, sortType));
        }

        @Test
        void shouldThrowExceptionWhenMapperThrowsException() {
            Long userId = 1L;
            SortType sortType = SortType.values()[0];
            SharedAccountActivity entity = SharedAccountActivity.builder().userId(userId).build();

            when(sharedAccountActivityRepository.findByUserId(eq(userId), any(Pageable.class)))
                    .thenReturn(List.of(entity));
            when(sharedAccountActivityMapper.mapToSharedAccountActivity(entity))
                    .thenThrow(new RuntimeException("mapping failed"));

            assertThrows(RuntimeException.class, () -> sharedAccountActivityService.getSharedAccountActivity(userId, sortType));
        }

        @Test
        void shouldThrowExceptionWhenUserIdIsNull() {
            SortType sortType = SortType.values()[0];

            when(sharedAccountActivityRepository.findByUserId(eq(null), any(Pageable.class)))
                    .thenThrow(new IllegalArgumentException("userId must not be null"));

            assertThrows(IllegalArgumentException.class, () -> sharedAccountActivityService.getSharedAccountActivity(null, sortType));
        }
    }

    @Nested
    class GetRepositoryFindByUserIdTests {

        @Test
        void shouldReturnActivitiesWhenRepositoryReturnsResults() {
            Long userId = 1L;
            Pageable pageable = mock(Pageable.class);
            SharedAccountActivity entity = SharedAccountActivity.builder().userId(userId).build();

            when(sharedAccountActivityRepository.findByUserId(userId, pageable)).thenReturn(List.of(entity));

            List<SharedAccountActivity> result = sharedAccountActivityService.getRepositoryFindByUserId(userId, pageable);

            assertEquals(1, result.size());
            assertEquals(entity, result.get(0));
        }

        @Test
        void shouldReturnEmptyListWhenRepositoryReturnsNoResults() {
            Long userId = 1L;
            Pageable pageable = mock(Pageable.class);

            when(sharedAccountActivityRepository.findByUserId(userId, pageable)).thenReturn(List.of());

            List<SharedAccountActivity> result = sharedAccountActivityService.getRepositoryFindByUserId(userId, pageable);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            Long userId = 1L;
            Pageable pageable = mock(Pageable.class);

            when(sharedAccountActivityRepository.findByUserId(userId, pageable))
                    .thenThrow(new RuntimeException("connection lost"));

            assertThrows(RuntimeException.class, () -> sharedAccountActivityService.getRepositoryFindByUserId(userId, pageable));
        }
    }

    @Nested
    class MapToDtoTests {

        @Test
        void shouldReturnDtoWhenEntityIsValid() {
            SharedAccountActivity entity = SharedAccountActivity.builder().userId(1L).build();
            SharedAccountActivityDto dto = new SharedAccountActivityDto(
                    SharedAccountActivityType.values()[0],
                    BigDecimal.TEN,
                    "cofounder",
                    "cofounder@finovara.com",
                    LocalDateTime.now()
            );

            when(sharedAccountActivityMapper.mapToSharedAccountActivity(entity)).thenReturn(dto);

            SharedAccountActivityDto result = sharedAccountActivityService.mapToDto(entity);

            assertEquals(dto, result);
        }

        @Test
        void shouldReturnNullWhenMapperReturnsNull() {
            SharedAccountActivity entity = SharedAccountActivity.builder().userId(1L).build();

            when(sharedAccountActivityMapper.mapToSharedAccountActivity(entity)).thenReturn(null);

            SharedAccountActivityDto result = sharedAccountActivityService.mapToDto(entity);

            assertNull(result);
        }

        @Test
        void shouldThrowExceptionWhenMapperFails() {
            SharedAccountActivity entity = SharedAccountActivity.builder().userId(1L).build();

            when(sharedAccountActivityMapper.mapToSharedAccountActivity(entity))
                    .thenThrow(new RuntimeException("mapping error"));

            assertThrows(RuntimeException.class, () -> sharedAccountActivityService.mapToDto(entity));
        }
    }

    @Nested
    class DeleteByUserIdTests {

        @Test
        void shouldDeleteActivitiesWhenUserIdIsValid() {
            Long userId = 1L;

            sharedAccountActivityService.deleteByUserId(userId);

            verify(sharedAccountActivityRepository, times(1)).deleteByUserId(userId);
        }

        @Test
        void shouldCallRepositoryExactlyOnceWhenDeletingActivities() {
            Long userId = 2L;

            sharedAccountActivityService.deleteByUserId(userId);

            verify(sharedAccountActivityRepository, times(1)).deleteByUserId(anyLong());
            verify(sharedAccountActivityRepository, never()).deleteByUserId(3L);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryDeleteFails() {
            Long userId = 1L;
            doThrow(new RuntimeException("delete failed"))
                    .when(sharedAccountActivityRepository).deleteByUserId(userId);

            assertThrows(RuntimeException.class, () -> sharedAccountActivityService.deleteByUserId(userId));
        }

        @Test
        void shouldThrowExceptionWhenUserIdIsNull() {
            doThrow(new IllegalArgumentException("userId must not be null"))
                    .when(sharedAccountActivityRepository).deleteByUserId(null);

            assertThrows(IllegalArgumentException.class, () -> sharedAccountActivityService.deleteByUserId(null));
        }

        @Test
        void shouldNotCallMapperWhenDeletingActivities() {
            Long userId = 1L;

            sharedAccountActivityService.deleteByUserId(userId);

            verifyNoInteractions(sharedAccountActivityMapper);
        }
    }
}