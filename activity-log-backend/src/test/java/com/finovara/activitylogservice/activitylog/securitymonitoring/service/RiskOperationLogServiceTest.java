package com.finovara.activitylogservice.activitylog.securitymonitoring.service;

import com.finovara.activitylogservice.activitylog.securitymonitoring.dto.RiskOperationActivityDto;
import com.finovara.activitylogservice.activitylog.securitymonitoring.mapper.RiskOperationActivityMapper;
import com.finovara.activitylogservice.activitylog.securitymonitoring.model.RiskOperationActivity;
import com.finovara.activitylogservice.activitylog.securitymonitoring.repository.RiskOperationActivityRepository;
import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.contracts.mainaccount.activity.event.securitymonitoring.RiskOperationCreatedEvent;
import com.finovara.contracts.securitymonitoring.model.RiskAction;
import com.finovara.contracts.securitymonitoring.model.RiskRule;
import com.finovara.contracts.securitymonitoring.model.RiskTriggerType;
import com.finovara.contracts.user.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.util.SortType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskOperationLogServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final String SOURCE_EVENT_ID = "source-event-1";
    private static final int PAGE_SIZE = 10;
    private static final int SCORE = 85;
    private static final RiskTriggerType TRIGGER_TYPE = RiskTriggerType.LOGIN;
    private static final RiskAction ACTION = RiskAction.LOG_ONLY;
    private static final LocalDate OPERATION_DATE = LocalDate.of(2026, 5, 25);
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 5, 25, 10, 0);

    @Mock
    private RiskOperationActivityRepository riskOperationActivityRepository;

    @Mock
    private RiskOperationActivityMapper riskOperationActivityMapper;

    @Mock
    private AuthBackendClient authBackendClient;

    @InjectMocks
    private RiskOperationLogService riskOperationLogService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(riskOperationLogService, "pageSize", PAGE_SIZE);
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteActivitiesWhenUserIdIsProvided() {
            riskOperationLogService.deleteByUserId(USER_ID);

            verify(riskOperationActivityRepository).deleteByUserId(USER_ID);
        }

        @Test
        void shouldDeleteActivitiesWhenUserIdIsDifferent() {
            riskOperationLogService.deleteByUserId(OTHER_USER_ID);

            verify(riskOperationActivityRepository).deleteByUserId(OTHER_USER_ID);
            verify(riskOperationActivityRepository, never()).deleteByUserId(USER_ID);
        }

        @Test
        void shouldNotInteractWithOtherDependenciesWhenDeletingUserData() {
            riskOperationLogService.deleteByUserId(USER_ID);

            verifyNoInteractions(riskOperationActivityMapper, authBackendClient);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryDeleteFails() {
            doThrow(new IllegalStateException("delete failed")).when(riskOperationActivityRepository).deleteByUserId(USER_ID);

            assertThrows(IllegalStateException.class, () -> riskOperationLogService.deleteByUserId(USER_ID));
        }
    }

    @Nested
    class HandleEvent {

        private RiskOperationCreatedEvent event;

        @BeforeEach
        void setUp() {
            event = new RiskOperationCreatedEvent(
                    USER_ID,
                    SOURCE_EVENT_ID,
                    TRIGGER_TYPE,
                    SCORE,
                    ACTION,
                    OPERATION_DATE,
                    CREATED_AT,
                    List.of(RiskRule.REPEAT_CHANGE, RiskRule.UNKNOWN_DEVICE)
            );
        }

        @Test
        void shouldSkipSavingWhenEventIsDuplicate() {
            when(riskOperationActivityRepository.existsBySourceEventId(SOURCE_EVENT_ID)).thenReturn(true);

            riskOperationLogService.handleEvent(event);

            verify(riskOperationActivityRepository).existsBySourceEventId(SOURCE_EVENT_ID);
            verify(riskOperationActivityRepository, never()).save(any(RiskOperationActivity.class));
        }

        @Test
        void shouldThrowExceptionWhenEventIsNull() {
            assertThrows(NullPointerException.class, () -> riskOperationLogService.handleEvent(null));
            verify(riskOperationActivityRepository, never()).save(any(RiskOperationActivity.class));
        }

        @Test
        void shouldThrowExceptionWhenRiskRulesAreNull() {
            RiskOperationCreatedEvent nullRulesEvent = new RiskOperationCreatedEvent(
                    USER_ID,
                    SOURCE_EVENT_ID,
                    TRIGGER_TYPE,
                    SCORE,
                    ACTION,
                    OPERATION_DATE,
                    CREATED_AT,
                    null
            );

            assertThrows(NullPointerException.class, () -> riskOperationLogService.handleEvent(nullRulesEvent));
            verify(riskOperationActivityRepository, never()).save(any(RiskOperationActivity.class));
        }

        @Test
        void shouldThrowExceptionWhenDuplicateCheckFails() {
            when(riskOperationActivityRepository.existsBySourceEventId(SOURCE_EVENT_ID))
                    .thenThrow(new IllegalStateException("exists failed"));

            assertThrows(IllegalStateException.class, () -> riskOperationLogService.handleEvent(event));
            verify(riskOperationActivityRepository, never()).save(any(RiskOperationActivity.class));
        }
    }

    @Nested
    class ConfirmPassword {

        @Test
        void shouldDelegateVerificationWhenDtoIsProvided() {
            ConfirmPasswordDto confirmPasswordDto = new ConfirmPasswordDto("Secret123");

            riskOperationLogService.confirmPassword(USER_ID, confirmPasswordDto);

            verify(authBackendClient).verifyPassword(USER_ID, confirmPasswordDto);
        }

        @Test
        void shouldDelegateVerificationWhenDtoIsNull() {
            riskOperationLogService.confirmPassword(USER_ID, null);

            verify(authBackendClient).verifyPassword(USER_ID, null);
        }

        @Test
        void shouldThrowExceptionWhenVerificationFails() {
            ConfirmPasswordDto confirmPasswordDto = new ConfirmPasswordDto("Secret123");
            doThrow(new IllegalArgumentException("invalid password")).when(authBackendClient)
                    .verifyPassword(USER_ID, confirmPasswordDto);

            assertThrows(IllegalArgumentException.class,
                    () -> riskOperationLogService.confirmPassword(USER_ID, confirmPasswordDto));
        }
    }

    @Nested
    class GetRiskActivity {

        private RiskOperationActivity firstActivity;
        private RiskOperationActivity secondActivity;

        @BeforeEach
        void setUp() {
            firstActivity = RiskOperationActivity.builder().userId(USER_ID).sourceEventId("first").build();
            secondActivity = RiskOperationActivity.builder().userId(USER_ID).sourceEventId("second").build();
        }

        @Test
        void shouldReturnMappedDtosWhenActivitiesExist() {
            RiskOperationActivityDto firstDto = new RiskOperationActivityDto("first", TRIGGER_TYPE, SCORE, ACTION, List.of(), CREATED_AT, OPERATION_DATE);
            when(riskOperationActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of(firstActivity));
            when(riskOperationActivityMapper.mapToRiskOperationActivity(firstActivity)).thenReturn(firstDto);

            List<RiskOperationActivityDto> result = riskOperationLogService.getRiskActivity(USER_ID, SortType.NEWEST, 1);

            assertEquals(List.of(firstDto), result);
        }

        @Test
        void shouldReturnAllMappedDtosInRepositoryOrder() {
            RiskOperationActivityDto firstDto = new RiskOperationActivityDto("first", TRIGGER_TYPE, SCORE, ACTION, List.of(), CREATED_AT, OPERATION_DATE);
            RiskOperationActivityDto secondDto = new RiskOperationActivityDto("second", TRIGGER_TYPE, SCORE, ACTION, List.of(), CREATED_AT, OPERATION_DATE);
            when(riskOperationActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of(firstActivity, secondActivity));
            when(riskOperationActivityMapper.mapToRiskOperationActivity(firstActivity)).thenReturn(firstDto);
            when(riskOperationActivityMapper.mapToRiskOperationActivity(secondActivity)).thenReturn(secondDto);

            List<RiskOperationActivityDto> result = riskOperationLogService.getRiskActivity(USER_ID, SortType.NEWEST, 1);

            assertEquals(List.of(firstDto, secondDto), result);
        }

        @Test
        void shouldReturnEmptyListWhenNoActivitiesExist() {
            when(riskOperationActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of());

            List<RiskOperationActivityDto> result = riskOperationLogService.getRiskActivity(USER_ID, SortType.NEWEST, 1);

            assertTrue(result.isEmpty());
            verifyNoInteractions(riskOperationActivityMapper);
        }

        @ParameterizedTest
        @EnumSource(SortType.class)
        void shouldQueryRepositoryForSupportedSortTypes(SortType sortType) {
            when(riskOperationActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of());

            List<RiskOperationActivityDto> result = riskOperationLogService.getRiskActivity(USER_ID, sortType, 1);

            assertTrue(result.isEmpty());
            verify(riskOperationActivityRepository).findByUserId(eq(USER_ID), any(Pageable.class));
        }

        @Test
        void shouldUseConfiguredPageSizeWhenFetchingActivities() {
            when(riskOperationActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of());

            riskOperationLogService.getRiskActivity(USER_ID, SortType.NEWEST, 1);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(riskOperationActivityRepository).findByUserId(eq(USER_ID), captor.capture());
            assertEquals(PAGE_SIZE, captor.getValue().getPageSize());
        }

        @Test
        void shouldUseUpdatedPageSizeWhenFieldChanges() {
            ReflectionTestUtils.setField(riskOperationLogService, "pageSize", 25);
            when(riskOperationActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of());

            riskOperationLogService.getRiskActivity(USER_ID, SortType.NEWEST, 1);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(riskOperationActivityRepository).findByUserId(eq(USER_ID), captor.capture());
            assertEquals(25, captor.getValue().getPageSize());
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(riskOperationActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenThrow(new IllegalStateException("query failed"));

            assertThrows(IllegalStateException.class,
                    () -> riskOperationLogService.getRiskActivity(USER_ID, SortType.NEWEST, 1));
        }

        @Test
        void shouldThrowExceptionWhenMapperFails() {
            when(riskOperationActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of(firstActivity));
            when(riskOperationActivityMapper.mapToRiskOperationActivity(firstActivity))
                    .thenThrow(new IllegalStateException("mapping failed"));

            assertThrows(IllegalStateException.class,
                    () -> riskOperationLogService.getRiskActivity(USER_ID, SortType.NEWEST, 1));
        }
    }
}
