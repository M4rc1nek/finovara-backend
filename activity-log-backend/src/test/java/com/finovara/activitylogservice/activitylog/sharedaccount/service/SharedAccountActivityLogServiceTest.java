package com.finovara.activitylogservice.activitylog.sharedaccount.service;

import com.finovara.activitylogservice.activitylog.sharedaccount.dto.SharedAccountActivityLogDto;
import com.finovara.activitylogservice.activitylog.sharedaccount.dto.SharedAccountActivityLogFilterDto;
import com.finovara.activitylogservice.activitylog.sharedaccount.mapper.SharedAccountActivityLogMapper;
import com.finovara.activitylogservice.activitylog.sharedaccount.model.SharedAccountActivityLog;
import com.finovara.activitylogservice.activitylog.sharedaccount.repository.SharedAccountActivityLogRepository;
import com.finovara.activitylogservice.activitylog.util.UserSessionResolver;
import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.contracts.sharedaccount.SharedRole;
import com.finovara.contracts.sharedaccount.SharedAccountMemberInfoDto;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.contracts.user.authorization.dto.UserDataDto;
import com.finovara.contracts.util.SortType;
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

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class SharedAccountActivityLogServiceTest {

    private static final int PAGE_SIZE = 25;
    private static final int PAGE = 1;
    private static final SortType SORT = SortType.values()[0];
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long THIRD_USER_ID = 3L;
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
    private UserSessionResolver userSessionResolver;

    @Mock
    private SharedAccountActivityLogEvent event;

    @Mock
    private SharedAccountActivityLog firstActivity;

    @Mock
    private SharedAccountActivityLog secondActivity;

    @Mock
    private SharedAccountActivityLog thirdActivity;

    @Mock
    private SharedAccountActivityLogDto firstDto;

    @Mock
    private SharedAccountActivityLogDto secondDto;

    @Mock
    private SharedAccountActivityLogDto thirdDto;

    @Mock
    private UserDataDto userData;

    @Mock
    private UserDataDto otherUserData;

    @Mock
    private UserDataDto thirdUserData;

    @Captor
    private ArgumentCaptor<SharedAccountActivityLog> activityCaptor;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    @InjectMocks
    private SharedAccountActivityLogService service;

    @BeforeEach
    void setUp() {
        setField(service, "pageSize", PAGE_SIZE);
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
        void shouldSaveActivityWithNullMemberWhenEventHasNoMember() {
            when(event.ownerId()).thenReturn(OWNER_ID);
            when(event.memberId()).thenReturn(null);
            when(event.userId()).thenReturn(USER_ID);

            service.createSharedAccountActivityLog(event);

            verify(sharedAccountActivityLogRepository).save(activityCaptor.capture());
            SharedAccountActivityLog saved = activityCaptor.getValue();
            assertEquals(OWNER_ID, saved.getOwnerId());
            assertNull(saved.getMemberId());
        }

        @Test
        void shouldSaveActivityExactlyOnceWhenEventIsValid() {
            service.createSharedAccountActivityLog(event);

            verify(sharedAccountActivityLogRepository, times(1)).save(any(SharedAccountActivityLog.class));
            verifyNoMoreInteractions(sharedAccountActivityLogRepository);
        }

        @Test
        void shouldNotUseAuthBackendWhenCreatingActivity() {
            service.createSharedAccountActivityLog(event);

            verifyNoInteractions(authBackendClient, sharedAccountActivityLogMapper);
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
            when(sharedAccountActivityLogRepository.findAllByOwnerIdOrMemberId(any(Long.class), any(Pageable.class)))
                    .thenReturn(List.of(firstActivity, secondActivity));
            when(firstActivity.getUserId()).thenReturn(USER_ID);
            when(secondActivity.getUserId()).thenReturn(OTHER_USER_ID);
            when(userSessionResolver.getUserInfo(USER_ID)).thenReturn(userData);
            when(userSessionResolver.getUserInfo(OTHER_USER_ID)).thenReturn(otherUserData);
            when(sharedAccountActivityLogMapper.mapToDto(firstActivity, userData)).thenReturn(firstDto);
            when(sharedAccountActivityLogMapper.mapToDto(secondActivity, otherUserData)).thenReturn(secondDto);

            List<SharedAccountActivityLogDto> result = service.getSharedAccountLogActivity(USER_ID, SORT, PAGE);

            assertEquals(2, result.size());
            assertSame(firstDto, result.getFirst());
            assertSame(secondDto, result.get(1));
        }

        @Test
        void shouldReturnEmptyListWhenNoActivitiesFound() {
            when(sharedAccountActivityLogRepository.findAllByOwnerIdOrMemberId(any(Long.class), any(Pageable.class)))
                    .thenReturn(List.of());

            List<SharedAccountActivityLogDto> result = service.getSharedAccountLogActivity(USER_ID, SORT, PAGE);

            assertTrue(result.isEmpty());
            verifyNoInteractions(authBackendClient, sharedAccountActivityLogMapper);
        }

        @Test
        void shouldRequestPageableWithConfiguredPageSizeWhenFetchingActivities() {
            when(sharedAccountActivityLogRepository.findAllByOwnerIdOrMemberId(any(Long.class), any(Pageable.class)))
                    .thenReturn(List.of());

            service.getSharedAccountLogActivity(USER_ID, SORT, PAGE);

            verify(sharedAccountActivityLogRepository)
                    .findAllByOwnerIdOrMemberId(any(Long.class), pageableCaptor.capture());
            assertEquals(PAGE_SIZE, pageableCaptor.getValue().getPageSize());
        }

        @Test
        void shouldRequestActivitiesForGivenUserWhenFetchingActivities() {
            when(sharedAccountActivityLogRepository.findAllByOwnerIdOrMemberId(any(Long.class), any(Pageable.class)))
                    .thenReturn(List.of());

            service.getSharedAccountLogActivity(OTHER_USER_ID, SORT, PAGE);

            verify(sharedAccountActivityLogRepository)
                    .findAllByOwnerIdOrMemberId(any(Long.class), pageableCaptor.capture());
            verify(sharedAccountActivityLogRepository, times(1))
                    .findAllByOwnerIdOrMemberId(OTHER_USER_ID, pageableCaptor.getValue());
        }

        @Test
        void shouldReturnEmptyListWhenUserIdIsNull() {
            when(sharedAccountActivityLogRepository.findAllByOwnerIdOrMemberId(any(), any(Pageable.class)))
                    .thenReturn(List.of());

            List<SharedAccountActivityLogDto> result = service.getSharedAccountLogActivity(null, SORT, PAGE);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldFetchUserDataOnlyOnceWhenActivitiesShareSameUser() {
            when(sharedAccountActivityLogRepository.findAllByOwnerIdOrMemberId(any(Long.class), any(Pageable.class)))
                    .thenReturn(List.of(firstActivity, secondActivity));
            when(firstActivity.getUserId()).thenReturn(USER_ID);
            when(secondActivity.getUserId()).thenReturn(USER_ID);
            when(userSessionResolver.getUserInfo(USER_ID)).thenReturn(userData);
            when(sharedAccountActivityLogMapper.mapToDto(firstActivity, userData)).thenReturn(firstDto);
            when(sharedAccountActivityLogMapper.mapToDto(secondActivity, userData)).thenReturn(secondDto);

            List<SharedAccountActivityLogDto> result = service.getSharedAccountLogActivity(USER_ID, SORT, PAGE);

            assertEquals(2, result.size());
            verify(userSessionResolver, times(1)).getUserInfo(USER_ID);
        }

        @Test
        void shouldFetchUserDataOncePerDistinctUserWhenActivitiesHaveMixedUsers() {
            when(sharedAccountActivityLogRepository.findAllByOwnerIdOrMemberId(any(Long.class), any(Pageable.class)))
                    .thenReturn(List.of(firstActivity, secondActivity, thirdActivity));
            when(firstActivity.getUserId()).thenReturn(USER_ID);
            when(secondActivity.getUserId()).thenReturn(OTHER_USER_ID);
            when(thirdActivity.getUserId()).thenReturn(USER_ID);
            when(userSessionResolver.getUserInfo(USER_ID)).thenReturn(userData);
            when(userSessionResolver.getUserInfo(OTHER_USER_ID)).thenReturn(otherUserData);
            when(sharedAccountActivityLogMapper.mapToDto(firstActivity, userData)).thenReturn(firstDto);
            when(sharedAccountActivityLogMapper.mapToDto(secondActivity, otherUserData)).thenReturn(secondDto);
            when(sharedAccountActivityLogMapper.mapToDto(thirdActivity, userData)).thenReturn(thirdDto);

            List<SharedAccountActivityLogDto> result = service.getSharedAccountLogActivity(USER_ID, SORT, PAGE);

            assertEquals(3, result.size());
            assertSame(firstDto, result.getFirst());
            assertSame(secondDto, result.get(1));
            assertSame(thirdDto, result.get(2));
            verify(userSessionResolver, times(1)).getUserInfo(USER_ID);
            verify(userSessionResolver, times(1)).getUserInfo(OTHER_USER_ID);
            verifyNoMoreInteractions(authBackendClient);
        }

        @Test
        void shouldMapEachActivityWithMatchingUserDataWhenThreeDistinctUsersExist() {
            when(sharedAccountActivityLogRepository.findAllByOwnerIdOrMemberId(any(Long.class), any(Pageable.class)))
                    .thenReturn(List.of(firstActivity, secondActivity, thirdActivity));
            when(firstActivity.getUserId()).thenReturn(USER_ID);
            when(secondActivity.getUserId()).thenReturn(OTHER_USER_ID);
            when(thirdActivity.getUserId()).thenReturn(THIRD_USER_ID);
            when(userSessionResolver.getUserInfo(USER_ID)).thenReturn(userData);
            when(userSessionResolver.getUserInfo(OTHER_USER_ID)).thenReturn(otherUserData);
            when(userSessionResolver.getUserInfo(THIRD_USER_ID)).thenReturn(thirdUserData);
            when(sharedAccountActivityLogMapper.mapToDto(firstActivity, userData)).thenReturn(firstDto);
            when(sharedAccountActivityLogMapper.mapToDto(secondActivity, otherUserData)).thenReturn(secondDto);
            when(sharedAccountActivityLogMapper.mapToDto(thirdActivity, thirdUserData)).thenReturn(thirdDto);

            List<SharedAccountActivityLogDto> result = service.getSharedAccountLogActivity(USER_ID, SORT, PAGE);

            assertEquals(List.of(firstDto, secondDto, thirdDto), result);
        }

        @Test
        void shouldPreserveRepositoryOrderWhenMappingActivities() {
            when(sharedAccountActivityLogRepository.findAllByOwnerIdOrMemberId(any(Long.class), any(Pageable.class)))
                    .thenReturn(List.of(secondActivity, firstActivity));
            when(firstActivity.getUserId()).thenReturn(USER_ID);
            when(secondActivity.getUserId()).thenReturn(OTHER_USER_ID);
            when(userSessionResolver.getUserInfo(USER_ID)).thenReturn(userData);
            when(userSessionResolver.getUserInfo(OTHER_USER_ID)).thenReturn(otherUserData);
            when(sharedAccountActivityLogMapper.mapToDto(firstActivity, userData)).thenReturn(firstDto);
            when(sharedAccountActivityLogMapper.mapToDto(secondActivity, otherUserData)).thenReturn(secondDto);

            List<SharedAccountActivityLogDto> result = service.getSharedAccountLogActivity(USER_ID, SORT, PAGE);

            assertSame(secondDto, result.getFirst());
            assertSame(firstDto, result.get(1));
        }

        @Test
        void shouldMapEachActivityExactlyOnceWhenActivitiesExist() {
            when(sharedAccountActivityLogRepository.findAllByOwnerIdOrMemberId(any(Long.class), any(Pageable.class)))
                    .thenReturn(List.of(firstActivity, secondActivity));
            when(firstActivity.getUserId()).thenReturn(USER_ID);
            when(secondActivity.getUserId()).thenReturn(OTHER_USER_ID);
            when(userSessionResolver.getUserInfo(USER_ID)).thenReturn(userData);
            when(userSessionResolver.getUserInfo(OTHER_USER_ID)).thenReturn(otherUserData);

            service.getSharedAccountLogActivity(USER_ID, SORT, PAGE);

            verify(sharedAccountActivityLogMapper, times(1)).mapToDto(firstActivity, userData);
            verify(sharedAccountActivityLogMapper, times(1)).mapToDto(secondActivity, otherUserData);
            verifyNoMoreInteractions(sharedAccountActivityLogMapper);
        }

        @Test
        void shouldMapWithNullUserDataWhenResolverReturnsNull() {
            when(sharedAccountActivityLogRepository.findAllByOwnerIdOrMemberId(any(Long.class), any(Pageable.class)))
                    .thenReturn(List.of(firstActivity));
            when(firstActivity.getUserId()).thenReturn(USER_ID);
            when(userSessionResolver.getUserInfo(USER_ID)).thenReturn(null);
            when(sharedAccountActivityLogMapper.mapToDto(firstActivity, null)).thenReturn(firstDto);

            List<SharedAccountActivityLogDto> result = service.getSharedAccountLogActivity(USER_ID, SORT, PAGE);

            assertEquals(1, result.size());
            assertSame(firstDto, result.getFirst());
        }

        @Test
        void shouldResolveRemainingUsersWhenOneUserIsNotResolved() {
            when(sharedAccountActivityLogRepository.findAllByOwnerIdOrMemberId(any(Long.class), any(Pageable.class)))
                    .thenReturn(List.of(firstActivity, secondActivity));
            when(firstActivity.getUserId()).thenReturn(USER_ID);
            when(secondActivity.getUserId()).thenReturn(OTHER_USER_ID);
            when(userSessionResolver.getUserInfo(USER_ID)).thenReturn(null);
            when(userSessionResolver.getUserInfo(OTHER_USER_ID)).thenReturn(otherUserData);
            when(sharedAccountActivityLogMapper.mapToDto(firstActivity, null)).thenReturn(firstDto);
            when(sharedAccountActivityLogMapper.mapToDto(secondActivity, otherUserData)).thenReturn(secondDto);

            List<SharedAccountActivityLogDto> result = service.getSharedAccountLogActivity(USER_ID, SORT, PAGE);

            assertEquals(2, result.size());
            assertSame(firstDto, result.getFirst());
            assertSame(secondDto, result.get(1));
        }

        @Test
        void shouldPropagateExceptionWhenUserResolverFails() {
            when(sharedAccountActivityLogRepository.findAllByOwnerIdOrMemberId(any(Long.class), any(Pageable.class)))
                    .thenReturn(List.of(firstActivity));
            when(firstActivity.getUserId()).thenReturn(USER_ID);
            when(userSessionResolver.getUserInfo(USER_ID)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class,
                    () -> service.getSharedAccountLogActivity(USER_ID, SORT, PAGE));

            verifyNoInteractions(sharedAccountActivityLogMapper);
        }

        @Test
        void shouldThrowExceptionWhenMapperFails() {
            when(sharedAccountActivityLogRepository.findAllByOwnerIdOrMemberId(any(Long.class), any(Pageable.class)))
                    .thenReturn(List.of(firstActivity));
            when(firstActivity.getUserId()).thenReturn(USER_ID);
            when(userSessionResolver.getUserInfo(USER_ID)).thenReturn(userData);
            when(sharedAccountActivityLogMapper.mapToDto(firstActivity, userData))
                    .thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class,
                    () -> service.getSharedAccountLogActivity(USER_ID, SORT, PAGE));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(sharedAccountActivityLogRepository.findAllByOwnerIdOrMemberId(any(Long.class), any(Pageable.class)))
                    .thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class,
                    () -> service.getSharedAccountLogActivity(USER_ID, SORT, PAGE));

            verifyNoInteractions(authBackendClient, sharedAccountActivityLogMapper);
        }

        @Test
        void shouldThrowExceptionWhenSortIsNull() {
            assertThrows(NullPointerException.class,
                    () -> service.getSharedAccountLogActivity(USER_ID, null, PAGE));

            verifyNoInteractions(sharedAccountActivityLogRepository, authBackendClient, sharedAccountActivityLogMapper);
        }
    }

    @Nested
    class FilterSharedAccountLogActivity {

        @Test
        void shouldResolveMemberByRoleAndCallRepositoryWithRoleActorId() {
         SharedAccountActivityLogFilterDto filter =
                    new SharedAccountActivityLogFilterDto(
                            null,
                            null,
                            null,
                           SharedRole.OWNER,
                            LocalDate.now()
                    );
            SharedAccountActivityLog activity = mock(SharedAccountActivityLog.class);
            when(sharedAccountActivityLogRepository.findFilteredActivities(eq(USER_ID), eq(filter), eq(OWNER_ID), any(), any(), any(Pageable.class)))
                    .thenReturn(List.of(activity));
            when(activity.getUserId()).thenReturn(USER_ID);
            when(userSessionResolver.getUserInfo(USER_ID)).thenReturn(userData);
            when(sharedAccountActivityLogMapper.mapToDto(activity, userData)).thenReturn(firstDto);
            when(authBackendClient.getSharedAccountMembers(USER_ID)).thenReturn(List.of(
                    new SharedAccountMemberInfoDto(OWNER_ID, "owner", SharedRole.OWNER),
                    new SharedAccountMemberInfoDto(OTHER_USER_ID, "member", SharedRole.MEMBER)
            ));

            List<SharedAccountActivityLogDto> result = service.filterSharedAccountLogActivity(USER_ID, filter, 2);

            assertEquals(List.of(firstDto), result);
            verify(authBackendClient).getSharedAccountMembers(USER_ID);
            verify(sharedAccountActivityLogRepository).findFilteredActivities(eq(USER_ID), eq(filter), eq(OWNER_ID), any(), any(), any(Pageable.class));
        }

        @Test
        void shouldReturnEmptyListWhenRequestedRoleIsMissingFromSharedAccountMembers() {
            SharedAccountActivityLogFilterDto filter =
                    new SharedAccountActivityLogFilterDto(
                            null,
                            null,
                            null,
                            SharedRole.MEMBER,
                            LocalDate.now()
                    );
            when(authBackendClient.getSharedAccountMembers(USER_ID)).thenReturn(List.of(
                    new SharedAccountMemberInfoDto(OWNER_ID, "owner", SharedRole.OWNER)
            ));

            List<SharedAccountActivityLogDto> result = service.filterSharedAccountLogActivity(USER_ID, filter, 0);

            assertTrue(result.isEmpty());
            verify(authBackendClient).getSharedAccountMembers(USER_ID);
            verifyNoInteractions(sharedAccountActivityLogRepository);
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteActivitiesWhenUserIdIsProvided() {
            when(sharedAccountActivityLogRepository.findAllByUserId(USER_ID)).thenReturn(List.of(firstActivity));

            service.deleteByUserId(USER_ID);

            verify(sharedAccountActivityLogRepository).findAllByUserId(USER_ID);
            verify(sharedAccountActivityLogRepository).deleteAll(List.of(firstActivity));
        }

        @Test
        void shouldDeleteActivitiesExactlyOnceWhenUserIdIsProvided() {
            when(sharedAccountActivityLogRepository.findAllByUserId(USER_ID)).thenReturn(List.of(firstActivity));

            service.deleteByUserId(USER_ID);

            verify(sharedAccountActivityLogRepository, times(1)).deleteAll(List.of(firstActivity));
            verifyNoMoreInteractions(sharedAccountActivityLogRepository);
        }

        @Test
        void shouldDelegateDeletionWithNullWhenUserIdIsNull() {
            when(sharedAccountActivityLogRepository.findAllByUserId(null)).thenReturn(List.of());

            service.deleteByUserId(null);

            verify(sharedAccountActivityLogRepository).findAllByUserId(null);
            verify(sharedAccountActivityLogRepository).deleteAll(List.of());
        }

        @Test
        void shouldNotUseAuthBackendWhenDeletingActivities() {
            when(sharedAccountActivityLogRepository.findAllByUserId(USER_ID)).thenReturn(List.of(firstActivity));

            service.deleteByUserId(USER_ID);

            verifyNoInteractions(authBackendClient, sharedAccountActivityLogMapper);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryDeleteFails() {
            when(sharedAccountActivityLogRepository.findAllByUserId(USER_ID)).thenReturn(List.of(firstActivity));
            doThrow(new IllegalStateException()).when(sharedAccountActivityLogRepository)
                    .deleteAll(List.of(firstActivity));

            assertThrows(IllegalStateException.class, () -> service.deleteByUserId(USER_ID));
        }
    }
}
