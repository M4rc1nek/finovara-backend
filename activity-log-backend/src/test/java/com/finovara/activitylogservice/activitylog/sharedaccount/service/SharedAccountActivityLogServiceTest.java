package com.finovara.activitylogservice.activitylog.sharedaccount.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.finovara.activitylogservice.activitylog.sharedaccount.dto.SharedAccountActivityLogDto;
import com.finovara.activitylogservice.activitylog.sharedaccount.mapper.SharedAccountActivityLogMapper;
import com.finovara.activitylogservice.activitylog.sharedaccount.model.SharedAccountActivityLog;
import com.finovara.activitylogservice.activitylog.sharedaccount.repository.SharedAccountActivityLogRepository;
import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.contracts.user.authorization.dto.UserDataDto;
import com.finovara.contracts.util.SortType;
import feign.FeignException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class SharedAccountActivityLogServiceTest {

    private static final int PAGE_SIZE = 25;
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long OWNER_ID = 10L;
    private static final Long MEMBER_ID = 20L;
    private static final Long TARGET_ID = 30L;

    @Mock
    private SharedAccountActivityLogRepository sharedAccountActivityLogRepository;

    @Mock
    private SharedAccountActivityLogMapper sharedAccountActivityLogMapper;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private SharedAccountActivityLogEvent event;

    @Mock
    private SharedAccountActivityLog firstActivity;

    @Mock
    private SharedAccountActivityLog secondActivity;

    @Mock
    private SharedAccountActivityLogDto firstDto;

    @Mock
    private SharedAccountActivityLogDto secondDto;

    @Mock
    private UserDataDto userData;

    @Mock
    private UserDataDto otherUserData;

    @Captor
    private ArgumentCaptor<SharedAccountActivityLog> activityCaptor;

    @InjectMocks
    private SharedAccountActivityLogService service;

    private SortType sort;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        setField(service, "pageSize", PAGE_SIZE);
        sort = SortType.values()[0];
        pageable = sort.getPageable(PAGE_SIZE);
    }

    @Nested
    class CreateSharedAccountActivityLog {

        @Test
        void shouldSaveActivityWithEventDataWhenEventIsValid() {
            when(event.ownerId()).thenReturn(OWNER_ID);
            when(event.memberId()).thenReturn(MEMBER_ID);
            when(event.userId()).thenReturn(USER_ID);
            when(event.targetId()).thenReturn(TARGET_ID);

            service.createSharedAccountActivityLog(event);

            verify(sharedAccountActivityLogRepository).save(activityCaptor.capture());
            SharedAccountActivityLog saved = activityCaptor.getValue();
            assertEquals(OWNER_ID, saved.getOwnerId());
            assertEquals(MEMBER_ID, saved.getMemberId());
            assertEquals(USER_ID, saved.getUserId());
            assertEquals(TARGET_ID, saved.getTargetId());
            assertEquals(event.type(), saved.getActivityType());
            assertEquals(event.createdAt(), saved.getCreatedAt());
        }

        @Test
        void shouldSaveActivityWithNullTargetWhenEventHasNoTarget() {
            when(event.ownerId()).thenReturn(OWNER_ID);
            when(event.memberId()).thenReturn(MEMBER_ID);
            when(event.userId()).thenReturn(USER_ID);
            when(event.targetId()).thenReturn(null);

            service.createSharedAccountActivityLog(event);

            verify(sharedAccountActivityLogRepository).save(activityCaptor.capture());
            assertNull(activityCaptor.getValue().getTargetId());
        }

        @Test
        void shouldSaveActivityExactlyOnceWhenEventIsValid() {
            service.createSharedAccountActivityLog(event);

            verify(sharedAccountActivityLogRepository, times(1)).save(any(SharedAccountActivityLog.class));
        }

        @Test
        void shouldThrowExceptionWhenEventIsNull() {
            assertThrows(NullPointerException.class, () -> service.createSharedAccountActivityLog(null));

            verifyNoInteractions(sharedAccountActivityLogRepository);
        }

        @Test
        void shouldThrowExceptionWhenRepositorySaveFails() {
            doThrow(new IllegalStateException()).when(sharedAccountActivityLogRepository)
                    .save(any(SharedAccountActivityLog.class));

            assertThrows(IllegalStateException.class, () -> service.createSharedAccountActivityLog(event));
        }
    }

    @Nested
    class GetSharedAccountLogActivity {

        @Test
        void shouldReturnMappedDtosWhenActivitiesExist() {
            when(sharedAccountActivityLogRepository.findByOwnerIdOrMemberId(USER_ID, pageable))
                    .thenReturn(List.of(firstActivity, secondActivity));
            when(firstActivity.getUserId()).thenReturn(USER_ID);
            when(secondActivity.getUserId()).thenReturn(OTHER_USER_ID);
            when(authBackendClient.getUserSession(USER_ID)).thenReturn(userData);
            when(authBackendClient.getUserSession(OTHER_USER_ID)).thenReturn(otherUserData);
            when(sharedAccountActivityLogMapper.mapToDto(firstActivity, userData)).thenReturn(firstDto);
            when(sharedAccountActivityLogMapper.mapToDto(secondActivity, otherUserData)).thenReturn(secondDto);

            List<SharedAccountActivityLogDto> result = service.getSharedAccountLogActivity(USER_ID, sort);

            assertEquals(2, result.size());
            assertSame(firstDto, result.get(0));
            assertSame(secondDto, result.get(1));
        }

        @Test
        void shouldReturnEmptyListWhenNoActivitiesFound() {
            when(sharedAccountActivityLogRepository.findByOwnerIdOrMemberId(USER_ID, pageable))
                    .thenReturn(List.of());

            List<SharedAccountActivityLogDto> result = service.getSharedAccountLogActivity(USER_ID, sort);

            assertTrue(result.isEmpty());
            verifyNoInteractions(authBackendClient, sharedAccountActivityLogMapper);
        }

        @Test
        void shouldRequestPageableWithConfiguredPageSizeWhenFetchingActivities() {
            when(sharedAccountActivityLogRepository.findByOwnerIdOrMemberId(USER_ID, pageable))
                    .thenReturn(List.of());

            service.getSharedAccountLogActivity(USER_ID, sort);

            verify(sharedAccountActivityLogRepository).findByOwnerIdOrMemberId(USER_ID, pageable);
            assertEquals(PAGE_SIZE, pageable.getPageSize());
        }

        @Test
        void shouldFetchUserDataOnlyOnceWhenActivitiesShareSameUser() {
            when(sharedAccountActivityLogRepository.findByOwnerIdOrMemberId(USER_ID, pageable))
                    .thenReturn(List.of(firstActivity, secondActivity));
            when(firstActivity.getUserId()).thenReturn(USER_ID);
            when(secondActivity.getUserId()).thenReturn(USER_ID);
            when(authBackendClient.getUserSession(USER_ID)).thenReturn(userData);
            when(sharedAccountActivityLogMapper.mapToDto(firstActivity, userData)).thenReturn(firstDto);
            when(sharedAccountActivityLogMapper.mapToDto(secondActivity, userData)).thenReturn(secondDto);

            List<SharedAccountActivityLogDto> result = service.getSharedAccountLogActivity(USER_ID, sort);

            assertEquals(2, result.size());
            verify(authBackendClient, times(1)).getUserSession(USER_ID);
        }

        @Test
        void shouldPreserveRepositoryOrderWhenMappingActivities() {
            when(sharedAccountActivityLogRepository.findByOwnerIdOrMemberId(USER_ID, pageable))
                    .thenReturn(List.of(secondActivity, firstActivity));
            when(firstActivity.getUserId()).thenReturn(USER_ID);
            when(secondActivity.getUserId()).thenReturn(OTHER_USER_ID);
            when(authBackendClient.getUserSession(USER_ID)).thenReturn(userData);
            when(authBackendClient.getUserSession(OTHER_USER_ID)).thenReturn(otherUserData);
            when(sharedAccountActivityLogMapper.mapToDto(firstActivity, userData)).thenReturn(firstDto);
            when(sharedAccountActivityLogMapper.mapToDto(secondActivity, otherUserData)).thenReturn(secondDto);

            List<SharedAccountActivityLogDto> result = service.getSharedAccountLogActivity(USER_ID, sort);

            assertSame(secondDto, result.get(0));
            assertSame(firstDto, result.get(1));
        }

        @Test
        void shouldMapEachActivityExactlyOnceWhenActivitiesExist() {
            when(sharedAccountActivityLogRepository.findByOwnerIdOrMemberId(USER_ID, pageable))
                    .thenReturn(List.of(firstActivity, secondActivity));
            when(firstActivity.getUserId()).thenReturn(USER_ID);
            when(secondActivity.getUserId()).thenReturn(OTHER_USER_ID);
            when(authBackendClient.getUserSession(USER_ID)).thenReturn(userData);
            when(authBackendClient.getUserSession(OTHER_USER_ID)).thenReturn(otherUserData);

            service.getSharedAccountLogActivity(USER_ID, sort);

            verify(sharedAccountActivityLogMapper, times(1)).mapToDto(firstActivity, userData);
            verify(sharedAccountActivityLogMapper, times(1)).mapToDto(secondActivity, otherUserData);
        }

        @Test
        void shouldMapWithNullUserDataWhenUserIsNotFound() {
            FeignException.NotFound notFound = mock(FeignException.NotFound.class);
            when(sharedAccountActivityLogRepository.findByOwnerIdOrMemberId(USER_ID, pageable))
                    .thenReturn(List.of(firstActivity));
            when(firstActivity.getUserId()).thenReturn(USER_ID);
            when(authBackendClient.getUserSession(USER_ID)).thenThrow(notFound);
            when(sharedAccountActivityLogMapper.mapToDto(firstActivity, null)).thenReturn(firstDto);

            List<SharedAccountActivityLogDto> result = service.getSharedAccountLogActivity(USER_ID, sort);

            assertEquals(1, result.size());
            assertSame(firstDto, result.get(0));
        }

        @Test
        void shouldMapWithNullUserDataWhenAuthBackendFailsWithServerError() {
            FeignException.InternalServerError serverError = mock(FeignException.InternalServerError.class);
            when(sharedAccountActivityLogRepository.findByOwnerIdOrMemberId(USER_ID, pageable))
                    .thenReturn(List.of(firstActivity));
            when(firstActivity.getUserId()).thenReturn(USER_ID);
            when(authBackendClient.getUserSession(USER_ID)).thenThrow(serverError);
            when(sharedAccountActivityLogMapper.mapToDto(firstActivity, null)).thenReturn(firstDto);

            List<SharedAccountActivityLogDto> result = service.getSharedAccountLogActivity(USER_ID, sort);

            assertSame(firstDto, result.get(0));
        }

        @Test
        void shouldResolveRemainingUsersWhenOneUserLookupFails() {
            FeignException.NotFound notFound = mock(FeignException.NotFound.class);
            when(sharedAccountActivityLogRepository.findByOwnerIdOrMemberId(USER_ID, pageable))
                    .thenReturn(List.of(firstActivity, secondActivity));
            when(firstActivity.getUserId()).thenReturn(USER_ID);
            when(secondActivity.getUserId()).thenReturn(OTHER_USER_ID);
            when(authBackendClient.getUserSession(USER_ID)).thenThrow(notFound);
            when(authBackendClient.getUserSession(OTHER_USER_ID)).thenReturn(otherUserData);
            when(sharedAccountActivityLogMapper.mapToDto(firstActivity, null)).thenReturn(firstDto);
            when(sharedAccountActivityLogMapper.mapToDto(secondActivity, otherUserData)).thenReturn(secondDto);

            List<SharedAccountActivityLogDto> result = service.getSharedAccountLogActivity(USER_ID, sort);

            assertEquals(2, result.size());
            assertSame(firstDto, result.get(0));
            assertSame(secondDto, result.get(1));
        }

        @Test
        void shouldPropagateExceptionWhenAuthBackendFailsWithNonFeignException() {
            when(sharedAccountActivityLogRepository.findByOwnerIdOrMemberId(USER_ID, pageable))
                    .thenReturn(List.of(firstActivity));
            when(firstActivity.getUserId()).thenReturn(USER_ID);
            when(authBackendClient.getUserSession(USER_ID)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class,
                    () -> service.getSharedAccountLogActivity(USER_ID, sort));

            verifyNoInteractions(sharedAccountActivityLogMapper);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(sharedAccountActivityLogRepository.findByOwnerIdOrMemberId(USER_ID, pageable))
                    .thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> service.getSharedAccountLogActivity(USER_ID, sort));

            verifyNoInteractions(authBackendClient, sharedAccountActivityLogMapper);
        }

        @Test
        void shouldThrowExceptionWhenSortIsNull() {
            assertThrows(NullPointerException.class, () -> service.getSharedAccountLogActivity(USER_ID, null));

            verifyNoInteractions(sharedAccountActivityLogRepository, authBackendClient, sharedAccountActivityLogMapper);
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteActivitiesWhenUserIdIsProvided() {
            service.deleteByUserId(USER_ID);

            verify(sharedAccountActivityLogRepository).deleteByUserId(USER_ID);
        }

        @Test
        void shouldDelegateDeletionWithNullWhenUserIdIsNull() {
            service.deleteByUserId(null);

            verify(sharedAccountActivityLogRepository).deleteByUserId(null);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryDeleteFails() {
            doThrow(new IllegalStateException()).when(sharedAccountActivityLogRepository).deleteByUserId(USER_ID);

            assertThrows(IllegalStateException.class, () -> service.deleteByUserId(USER_ID));
        }
    }
}