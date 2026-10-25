package com.finovara.activitylogservice.activitylog.mainaccount.piggybank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.finovara.activitylogservice.activitylog.mainaccount.piggybank.dto.PiggyBankActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.piggybank.mapper.PiggyBankActivityMapper;
import com.finovara.activitylogservice.activitylog.mainaccount.piggybank.model.PiggyBankActivity;
import com.finovara.activitylogservice.activitylog.mainaccount.piggybank.repository.PiggyBankActivityRepository;
import com.finovara.contracts.mainaccount.activity.event.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.mainaccount.activity.event.piggybank.PiggyBankEditActivityEvent;
import com.finovara.contracts.util.SortType;
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
class PiggyBankActivityServiceTest {

    private static final int PAGE_SIZE = 15;
    private static final int PAGE = 1;
    private static final SortType SORT = SortType.values()[0];
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final String NAME = "Vacation";
    private static final String PREVIOUS_NAME = "Holiday";

    @Mock
    private PiggyBankActivityRepository piggyBankActivityRepository;

    @Mock
    private PiggyBankActivityMapper piggyBankActivityMapper;

    @Mock
    private PiggyBankActivityEvent event;

    @Mock
    private PiggyBankEditActivityEvent editEvent;

    @Mock
    private PiggyBankActivity firstActivity;

    @Mock
    private PiggyBankActivity secondActivity;

    @Mock
    private PiggyBankActivityDto firstDto;

    @Mock
    private PiggyBankActivityDto secondDto;

    @Captor
    private ArgumentCaptor<PiggyBankActivity> activityCaptor;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    @InjectMocks
    private PiggyBankActivityService service;

    @BeforeEach
    void setUp() {
        setField(service, "pageSize", PAGE_SIZE);
    }

    @Nested
    class HandleEvent {

        @Test
        void shouldSaveActivityWithEventDataWhenEventIsValid() {
            when(event.userId()).thenReturn(USER_ID);
            when(event.name()).thenReturn(NAME);

            service.handleEvent(event);

            verify(piggyBankActivityRepository).save(activityCaptor.capture());
            PiggyBankActivity saved = activityCaptor.getValue();
            assertEquals(USER_ID, saved.getUserId());
            assertEquals(NAME, saved.getPiggyBankName());
            assertEquals(event.type(), saved.getActivityType());
            assertEquals(event.goalType(), saved.getGoalType());
            assertEquals(event.goalAmount(), saved.getGoalAmount());
            assertEquals(event.amountPaid(), saved.getAmountPaid());
            assertEquals(event.occurredAt(), saved.getCreatedAt());
        }

        @Test
        void shouldNotSetPreviousValuesWhenHandlingRegularEvent() {
            when(event.userId()).thenReturn(USER_ID);
            when(event.name()).thenReturn(NAME);

            service.handleEvent(event);

            verify(piggyBankActivityRepository).save(activityCaptor.capture());
            PiggyBankActivity saved = activityCaptor.getValue();
            assertNull(saved.getPreviousPiggyBankName());
            assertNull(saved.getPreviousGoalType());
            assertNull(saved.getPreviousGoalAmount());
        }

        @Test
        void shouldSaveActivityWithNullNameWhenEventHasNoName() {
            when(event.userId()).thenReturn(USER_ID);
            when(event.name()).thenReturn(null);

            service.handleEvent(event);

            verify(piggyBankActivityRepository).save(activityCaptor.capture());
            assertNull(activityCaptor.getValue().getPiggyBankName());
        }

        @Test
        void shouldSaveActivityExactlyOnceWhenEventIsValid() {
            service.handleEvent(event);

            verify(piggyBankActivityRepository, times(1)).save(any(PiggyBankActivity.class));
            verifyNoMoreInteractions(piggyBankActivityRepository);
        }

        @Test
        void shouldNotUseMapperWhenHandlingEvent() {
            service.handleEvent(event);

            verifyNoInteractions(piggyBankActivityMapper);
        }

        @Test
        void shouldThrowExceptionWhenEventIsNull() {
            assertThrows(NullPointerException.class, () -> service.handleEvent(null));

            verifyNoInteractions(piggyBankActivityRepository);
        }

        @Test
        void shouldThrowExceptionWhenRepositorySaveFails() {
            doThrow(new IllegalStateException()).when(piggyBankActivityRepository)
                    .save(any(PiggyBankActivity.class));

            assertThrows(IllegalStateException.class, () -> service.handleEvent(event));
        }
    }

    @Nested
    class HandleEditEvent {

        @Test
        void shouldSaveActivityWithEventDataWhenEditEventIsValid() {
            when(editEvent.userId()).thenReturn(USER_ID);
            when(editEvent.name()).thenReturn(NAME);
            when(editEvent.previousName()).thenReturn(PREVIOUS_NAME);

            service.handleEditEvent(editEvent);

            verify(piggyBankActivityRepository).save(activityCaptor.capture());
            PiggyBankActivity saved = activityCaptor.getValue();
            assertEquals(USER_ID, saved.getUserId());
            assertEquals(NAME, saved.getPiggyBankName());
            assertEquals(PREVIOUS_NAME, saved.getPreviousPiggyBankName());
            assertEquals(editEvent.type(), saved.getActivityType());
            assertEquals(editEvent.goalType(), saved.getGoalType());
            assertEquals(editEvent.previousGoalType(), saved.getPreviousGoalType());
            assertEquals(editEvent.goalAmount(), saved.getGoalAmount());
            assertEquals(editEvent.previousGoalAmount(), saved.getPreviousGoalAmount());
            assertEquals(editEvent.occurredAt(), saved.getCreatedAt());
        }

        @Test
        void shouldNotSetAmountPaidWhenHandlingEditEvent() {
            when(editEvent.userId()).thenReturn(USER_ID);
            when(editEvent.name()).thenReturn(NAME);

            service.handleEditEvent(editEvent);

            verify(piggyBankActivityRepository).save(activityCaptor.capture());
            assertNull(activityCaptor.getValue().getAmountPaid());
        }

        @Test
        void shouldSaveActivityWithNullPreviousNameWhenEditEventHasNoPreviousName() {
            when(editEvent.userId()).thenReturn(USER_ID);
            when(editEvent.name()).thenReturn(NAME);
            when(editEvent.previousName()).thenReturn(null);

            service.handleEditEvent(editEvent);

            verify(piggyBankActivityRepository).save(activityCaptor.capture());
            assertNull(activityCaptor.getValue().getPreviousPiggyBankName());
        }

        @Test
        void shouldSaveActivityExactlyOnceWhenEditEventIsValid() {
            service.handleEditEvent(editEvent);

            verify(piggyBankActivityRepository, times(1)).save(any(PiggyBankActivity.class));
            verifyNoMoreInteractions(piggyBankActivityRepository);
        }

        @Test
        void shouldNotUseMapperWhenHandlingEditEvent() {
            service.handleEditEvent(editEvent);

            verifyNoInteractions(piggyBankActivityMapper);
        }

        @Test
        void shouldThrowExceptionWhenEditEventIsNull() {
            assertThrows(NullPointerException.class, () -> service.handleEditEvent(null));

            verifyNoInteractions(piggyBankActivityRepository);
        }

        @Test
        void shouldThrowExceptionWhenRepositorySaveFailsForEditEvent() {
            doThrow(new IllegalStateException()).when(piggyBankActivityRepository)
                    .save(any(PiggyBankActivity.class));

            assertThrows(IllegalStateException.class, () -> service.handleEditEvent(editEvent));
        }
    }

    @Nested
    class GetPiggyBankActivities {

        @Test
        void shouldReturnMappedDtosWhenActivitiesExist() {
            when(piggyBankActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of(firstActivity, secondActivity));
            when(piggyBankActivityMapper.mapToPiggyBankActivity(firstActivity)).thenReturn(firstDto);
            when(piggyBankActivityMapper.mapToPiggyBankActivity(secondActivity)).thenReturn(secondDto);

            List<PiggyBankActivityDto> result = service.getPiggyBankActivities(USER_ID, SORT, PAGE);

            assertEquals(2, result.size());
            assertSame(firstDto, result.get(0));
            assertSame(secondDto, result.get(1));
        }

        @Test
        void shouldReturnEmptyListWhenNoActivitiesFound() {
            when(piggyBankActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of());

            List<PiggyBankActivityDto> result = service.getPiggyBankActivities(USER_ID, SORT, PAGE);

            assertTrue(result.isEmpty());
            verifyNoInteractions(piggyBankActivityMapper);
        }

        @Test
        void shouldPreserveRepositoryOrderWhenMappingActivities() {
            when(piggyBankActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of(secondActivity, firstActivity));
            when(piggyBankActivityMapper.mapToPiggyBankActivity(firstActivity)).thenReturn(firstDto);
            when(piggyBankActivityMapper.mapToPiggyBankActivity(secondActivity)).thenReturn(secondDto);

            List<PiggyBankActivityDto> result = service.getPiggyBankActivities(USER_ID, SORT, PAGE);

            assertEquals(List.of(secondDto, firstDto), result);
        }

        @Test
        void shouldRequestPageableWithConfiguredPageSizeWhenFetchingActivities() {
            when(piggyBankActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of());

            service.getPiggyBankActivities(USER_ID, SORT, PAGE);

            verify(piggyBankActivityRepository).findByUserId(eq(USER_ID), pageableCaptor.capture());
            assertEquals(PAGE_SIZE, pageableCaptor.getValue().getPageSize());
        }

        @Test
        void shouldRequestPageableWithGivenPageNumberWhenFetchingActivities() {
            when(piggyBankActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of());

            service.getPiggyBankActivities(USER_ID, SORT, PAGE);

            verify(piggyBankActivityRepository).findByUserId(eq(USER_ID), pageableCaptor.capture());
            assertEquals(PAGE, pageableCaptor.getValue().getPageNumber());
        }

        @Test
        void shouldRequestActivitiesForGivenUserWhenFetchingActivities() {
            when(piggyBankActivityRepository.findByUserId(eq(OTHER_USER_ID), any(Pageable.class)))
                    .thenReturn(List.of());

            service.getPiggyBankActivities(OTHER_USER_ID, SORT, PAGE);

            verify(piggyBankActivityRepository, times(1)).findByUserId(eq(OTHER_USER_ID), any(Pageable.class));
            verifyNoMoreInteractions(piggyBankActivityRepository);
        }

        @Test
        void shouldMapEachActivityExactlyOnceWhenActivitiesExist() {
            when(piggyBankActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of(firstActivity, secondActivity));

            service.getPiggyBankActivities(USER_ID, SORT, PAGE);

            verify(piggyBankActivityMapper, times(1)).mapToPiggyBankActivity(firstActivity);
            verify(piggyBankActivityMapper, times(1)).mapToPiggyBankActivity(secondActivity);
            verifyNoMoreInteractions(piggyBankActivityMapper);
        }

        @Test
        void shouldThrowExceptionWhenMapperFails() {
            when(piggyBankActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of(firstActivity));
            when(piggyBankActivityMapper.mapToPiggyBankActivity(firstActivity))
                    .thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class,
                    () -> service.getPiggyBankActivities(USER_ID, SORT, PAGE));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(piggyBankActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class,
                    () -> service.getPiggyBankActivities(USER_ID, SORT, PAGE));

            verifyNoInteractions(piggyBankActivityMapper);
        }

        @Test
        void shouldThrowExceptionWhenSortIsNull() {
            assertThrows(NullPointerException.class,
                    () -> service.getPiggyBankActivities(USER_ID, null, PAGE));

            verifyNoInteractions(piggyBankActivityRepository, piggyBankActivityMapper);
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteActivitiesWhenUserIdIsProvided() {
            when(piggyBankActivityRepository.findAllByUserId(USER_ID)).thenReturn(List.of(firstActivity));

            service.deleteByUserId(USER_ID);

            verify(piggyBankActivityRepository).findAllByUserId(USER_ID);
            verify(piggyBankActivityRepository).deleteAll(List.of(firstActivity));
        }

        @Test
        void shouldDeleteActivitiesExactlyOnceWhenUserIdIsProvided() {
            when(piggyBankActivityRepository.findAllByUserId(USER_ID)).thenReturn(List.of(firstActivity));

            service.deleteByUserId(USER_ID);

            verify(piggyBankActivityRepository, times(1)).deleteAll(List.of(firstActivity));
            verifyNoMoreInteractions(piggyBankActivityRepository);
        }

        @Test
        void shouldDelegateDeletionWithNullWhenUserIdIsNull() {
            when(piggyBankActivityRepository.findAllByUserId(null)).thenReturn(List.of());

            service.deleteByUserId(null);

            verify(piggyBankActivityRepository).findAllByUserId(null);
            verify(piggyBankActivityRepository).deleteAll(List.of());
        }

        @Test
        void shouldNotUseMapperWhenDeletingActivities() {
            when(piggyBankActivityRepository.findAllByUserId(USER_ID)).thenReturn(List.of(firstActivity));

            service.deleteByUserId(USER_ID);

            verifyNoInteractions(piggyBankActivityMapper);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryDeleteFails() {
            when(piggyBankActivityRepository.findAllByUserId(USER_ID)).thenReturn(List.of(firstActivity));
            doThrow(new IllegalStateException()).when(piggyBankActivityRepository).deleteAll(List.of(firstActivity));

            assertThrows(IllegalStateException.class, () -> service.deleteByUserId(USER_ID));
        }
    }
}