package com.finovara.activitylogservice.activitylog.securitymonitoring.service;

import com.finovara.activitylogservice.activitylog.securitymonitoring.dto.RiskOperationActivityDto;
import com.finovara.activitylogservice.activitylog.securitymonitoring.mapper.RiskOperationActivityMapper;
import com.finovara.activitylogservice.activitylog.securitymonitoring.model.RiskOperationActivity;
import com.finovara.activitylogservice.activitylog.securitymonitoring.model.RiskRuleCollectionActivity;
import com.finovara.activitylogservice.activitylog.securitymonitoring.repository.RiskOperationActivityRepository;
import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.contracts.activity.event.securitymonitoring.RiskOperationCreatedEvent;
import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.model.SortType;
import com.finovara.contracts.securitymonitoring.model.RiskAction;
import com.finovara.contracts.securitymonitoring.model.RiskRule;
import com.finovara.contracts.securitymonitoring.model.RiskTriggerType;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class RiskOperationLogServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final String SOURCE_EVENT_ID = "source-event-1";
    private static final int PAGE_SIZE = 10;
    private static final int SCORE = 85;
    private static final RiskTriggerType TRIGGER_TYPE = RiskTriggerType.values()[0];
    private static final RiskAction ACTION = RiskAction.values()[0];
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
        setField(riskOperationLogService, "pageSize", PAGE_SIZE);
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldCallRepositoryDeleteByUserIdWhenUserIdIsValid() {
            riskOperationLogService.deleteByUserId(USER_ID);

            verify(riskOperationActivityRepository).deleteByUserId(USER_ID);
            verifyNoMoreInteractions(riskOperationActivityRepository);
        }

        @Test
        void shouldDeleteOnlyActivitiesOfGivenUserWhenDifferentUsersExist() {
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
            doThrow(new IllegalStateException("delete failed"))
                    .when(riskOperationActivityRepository).deleteByUserId(USER_ID);

            assertThrows(IllegalStateException.class, () -> riskOperationLogService.deleteByUserId(USER_ID));
        }
    }

    @Nested
    class HandleEvent {

        private RiskRule firstRule;
        private RiskRule secondRule;
        private RiskOperationCreatedEvent event;

        @BeforeEach
        void setUp() {
            firstRule = mock(RiskRule.class);
            secondRule = mock(RiskRule.class);
            event = new RiskOperationCreatedEvent(
                    USER_ID,
                    SOURCE_EVENT_ID,
                    TRIGGER_TYPE,
                    SCORE,
                    ACTION,
                    OPERATION_DATE,
                    CREATED_AT,
                    List.of(firstRule, secondRule)
            );
        }

        @Test
        void shouldSaveActivityWithEventDataWhenEventIsNotDuplicate() {
            riskOperationLogService.handleEvent(event);

            ArgumentCaptor<RiskOperationActivity> captor = ArgumentCaptor.forClass(RiskOperationActivity.class);
            verify(riskOperationActivityRepository).save(captor.capture());

            RiskOperationActivity activity = captor.getValue();
            assertThat(activity.getUserId()).isEqualTo(USER_ID);
            assertThat(activity.getSourceEventId()).isEqualTo(SOURCE_EVENT_ID);
            assertThat(activity.getTriggerType()).isEqualTo(TRIGGER_TYPE);
            assertThat(activity.getScore()).isEqualTo(SCORE);
            assertThat(activity.getAction()).isEqualTo(ACTION);
            assertThat(activity.getOperationDate()).isEqualTo(OPERATION_DATE);
            assertThat(activity.getCreatedAt()).isEqualTo(CREATED_AT);
        }

        @Test
        void shouldCheckDuplicateBySourceEventIdWhenHandlingEvent() {
            riskOperationLogService.handleEvent(event);

            verify(riskOperationActivityRepository).existsBySourceEventId(SOURCE_EVENT_ID);
        }

        @Test
        void shouldMapAllRiskRulesToCollectionActivitiesWhenEventHasMultipleRules() {
            riskOperationLogService.handleEvent(event);

            ArgumentCaptor<RiskOperationActivity> captor = ArgumentCaptor.forClass(RiskOperationActivity.class);
            verify(riskOperationActivityRepository).save(captor.capture());

            List<RiskRuleCollectionActivity> ruleActivities = captor.getValue().getRiskRuleCollectionActivities();
            assertThat(ruleActivities).hasSize(2);
            assertThat(ruleActivities).extracting(RiskRuleCollectionActivity::getRiskRule)
                    .containsExactly(firstRule, secondRule);
        }

        @Test
        void shouldLinkEachRuleActivityToParentActivityWhenSavingEvent() {
            riskOperationLogService.handleEvent(event);

            ArgumentCaptor<RiskOperationActivity> captor = ArgumentCaptor.forClass(RiskOperationActivity.class);
            verify(riskOperationActivityRepository).save(captor.capture());

            RiskOperationActivity activity = captor.getValue();
            assertThat(activity.getRiskRuleCollectionActivities())
                    .allSatisfy(ruleActivity -> assertThat(ruleActivity.getRiskOperationActivity()).isSameAs(activity));
        }

        @Test
        void shouldSaveActivityWithSingleRuleWhenEventHasOneRule() {
            RiskOperationCreatedEvent singleRuleEvent = new RiskOperationCreatedEvent(
                    USER_ID,
                    SOURCE_EVENT_ID,
                    TRIGGER_TYPE,
                    SCORE,
                    ACTION,
                    OPERATION_DATE,
                    CREATED_AT,
                    List.of(firstRule)
            );

            riskOperationLogService.handleEvent(singleRuleEvent);

            ArgumentCaptor<RiskOperationActivity> captor = ArgumentCaptor.forClass(RiskOperationActivity.class);
            verify(riskOperationActivityRepository).save(captor.capture());
            assertThat(captor.getValue().getRiskRuleCollectionActivities()).hasSize(1);
            assertThat(captor.getValue().getRiskRuleCollectionActivities().get(0).getRiskRule()).isSameAs(firstRule);
        }

        @Test
        void shouldSaveActivityWithEmptyRuleCollectionWhenEventHasNoRules() {
            RiskOperationCreatedEvent noRulesEvent = new RiskOperationCreatedEvent(
                    USER_ID,
                    SOURCE_EVENT_ID,
                    TRIGGER_TYPE,
                    SCORE,
                    ACTION,
                    OPERATION_DATE,
                    CREATED_AT,
                    List.of()
            );

            riskOperationLogService.handleEvent(noRulesEvent);

            ArgumentCaptor<RiskOperationActivity> captor = ArgumentCaptor.forClass(RiskOperationActivity.class);
            verify(riskOperationActivityRepository).save(captor.capture());
            assertThat(captor.getValue().getRiskRuleCollectionActivities()).isEmpty();
        }

        @Test
        void shouldSaveActivityWhenScoreIsZero() {
            RiskOperationCreatedEvent zeroScoreEvent = new RiskOperationCreatedEvent(
                    USER_ID,
                    SOURCE_EVENT_ID,
                    TRIGGER_TYPE,
                    0,
                    ACTION,
                    OPERATION_DATE,
                    CREATED_AT,
                    List.of(firstRule)
            );

            riskOperationLogService.handleEvent(zeroScoreEvent);

            ArgumentCaptor<RiskOperationActivity> captor = ArgumentCaptor.forClass(RiskOperationActivity.class);
            verify(riskOperationActivityRepository).save(captor.capture());
            assertThat(captor.getValue().getScore()).isZero();
        }

        @Test
        void shouldSaveActivityWhenTriggerTypeAndActionAreNull() {
            RiskOperationCreatedEvent nullFieldsEvent = new RiskOperationCreatedEvent(
                    USER_ID,
                    SOURCE_EVENT_ID,
                    null,
                    SCORE,
                    null,
                    OPERATION_DATE,
                    CREATED_AT,
                    List.of(firstRule)
            );

            riskOperationLogService.handleEvent(nullFieldsEvent);

            ArgumentCaptor<RiskOperationActivity> captor = ArgumentCaptor.forClass(RiskOperationActivity.class);
            verify(riskOperationActivityRepository).save(captor.capture());
            assertThat(captor.getValue().getTriggerType()).isNull();
            assertThat(captor.getValue().getAction()).isNull();
        }

        @Test
        void shouldSkipSavingWhenEventIsDuplicate() {
            when(riskOperationActivityRepository.existsBySourceEventId(SOURCE_EVENT_ID)).thenReturn(true);

            riskOperationLogService.handleEvent(event);

            verify(riskOperationActivityRepository).existsBySourceEventId(SOURCE_EVENT_ID);
            verify(riskOperationActivityRepository, never()).save(any(RiskOperationActivity.class));
            verifyNoMoreInteractions(riskOperationActivityRepository);
        }

        @Test
        void shouldNotInteractWithMapperAndAuthClientWhenHandlingEvent() {
            riskOperationLogService.handleEvent(event);

            verifyNoInteractions(riskOperationActivityMapper, authBackendClient);
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

        @Test
        void shouldThrowExceptionWhenRepositorySaveFails() {
            when(riskOperationActivityRepository.save(any(RiskOperationActivity.class)))
                    .thenThrow(new IllegalStateException("save failed"));

            assertThrows(IllegalStateException.class, () -> riskOperationLogService.handleEvent(event));

            verify(riskOperationActivityRepository).save(any(RiskOperationActivity.class));
        }
    }

    @Nested
    class ConfirmPassword {

        private ConfirmPasswordDto confirmPasswordDto;

        @BeforeEach
        void setUp() {
            confirmPasswordDto = mock(ConfirmPasswordDto.class);
        }

        @Test
        void shouldDelegatePasswordVerificationToAuthBackendClientWhenConfirmingPassword() {
            riskOperationLogService.confirmPassword(USER_ID, confirmPasswordDto);

            verify(authBackendClient).verifyPassword(USER_ID, confirmPasswordDto);
            verifyNoMoreInteractions(authBackendClient);
        }

        @Test
        void shouldNotInteractWithRepositoryAndMapperWhenConfirmingPassword() {
            riskOperationLogService.confirmPassword(USER_ID, confirmPasswordDto);

            verifyNoInteractions(riskOperationActivityRepository, riskOperationActivityMapper);
        }

        @Test
        void shouldPassNullDtoToClientWhenDtoIsNull() {
            riskOperationLogService.confirmPassword(USER_ID, null);

            verify(authBackendClient).verifyPassword(USER_ID, null);
        }

        @Test
        void shouldPassNullUserIdToClientWhenUserIdIsNull() {
            riskOperationLogService.confirmPassword(null, confirmPasswordDto);

            verify(authBackendClient).verifyPassword(null, confirmPasswordDto);
        }

        @Test
        void shouldThrowExceptionWhenPasswordVerificationFails() {
            doThrow(new IllegalArgumentException("invalid password"))
                    .when(authBackendClient).verifyPassword(USER_ID, confirmPasswordDto);

            assertThrows(IllegalArgumentException.class,
                    () -> riskOperationLogService.confirmPassword(USER_ID, confirmPasswordDto));
        }
    }

    @Nested
    class GetRiskActivity {

        private RiskOperationActivity firstActivity;
        private RiskOperationActivity secondActivity;
        private RiskOperationActivityDto firstDto;
        private RiskOperationActivityDto secondDto;

        @BeforeEach
        void setUp() {
            firstActivity = RiskOperationActivity.builder().userId(USER_ID).sourceEventId("first").build();
            secondActivity = RiskOperationActivity.builder().userId(USER_ID).sourceEventId("second").build();
            firstDto = mock(RiskOperationActivityDto.class);
            secondDto = mock(RiskOperationActivityDto.class);
        }

        @Test
        void shouldReturnMappedActivitiesWhenUserHasActivities() {
            when(riskOperationActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of(firstActivity));
            when(riskOperationActivityMapper.mapToRiskOperationActivity(firstActivity)).thenReturn(firstDto);

            List<RiskOperationActivityDto> result = riskOperationLogService.getRiskActivity(USER_ID, SortType.NEWEST);

            assertThat(result).containsExactly(firstDto);
            verify(riskOperationActivityRepository).findByUserId(eq(USER_ID), any(Pageable.class));
            verify(riskOperationActivityMapper).mapToRiskOperationActivity(firstActivity);
        }

        @Test
        void shouldReturnAllMappedActivitiesInRepositoryOrderWhenUserHasMultipleActivities() {
            when(riskOperationActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of(firstActivity, secondActivity));
            when(riskOperationActivityMapper.mapToRiskOperationActivity(firstActivity)).thenReturn(firstDto);
            when(riskOperationActivityMapper.mapToRiskOperationActivity(secondActivity)).thenReturn(secondDto);

            List<RiskOperationActivityDto> result = riskOperationLogService.getRiskActivity(USER_ID, SortType.NEWEST);

            assertThat(result).containsExactly(firstDto, secondDto);
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoActivities() {
            when(riskOperationActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of());

            List<RiskOperationActivityDto> result = riskOperationLogService.getRiskActivity(USER_ID, SortType.OLDEST);

            assertThat(result).isEmpty();
            verifyNoInteractions(riskOperationActivityMapper);
        }

        @ParameterizedTest
        @EnumSource(SortType.class)
        void shouldQueryRepositoryWhenSortTypeIsSupported(SortType sortType) {
            when(riskOperationActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of());

            List<RiskOperationActivityDto> result = riskOperationLogService.getRiskActivity(USER_ID, sortType);

            assertThat(result).isEmpty();
            verify(riskOperationActivityRepository).findByUserId(eq(USER_ID), any(Pageable.class));
        }

        @Test
        void shouldUseConfiguredPageSizeWhenFetchingActivities() {
            when(riskOperationActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of());

            riskOperationLogService.getRiskActivity(USER_ID, SortType.NEWEST);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(riskOperationActivityRepository).findByUserId(eq(USER_ID), captor.capture());
            assertThat(captor.getValue().getPageSize()).isEqualTo(PAGE_SIZE);
        }

        @Test
        void shouldUseUpdatedPageSizeWhenPageSizeIsChanged() {
            setField(riskOperationLogService, "pageSize", 25);
            when(riskOperationActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of());

            riskOperationLogService.getRiskActivity(USER_ID, SortType.NEWEST);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(riskOperationActivityRepository).findByUserId(eq(USER_ID), captor.capture());
            assertThat(captor.getValue().getPageSize()).isEqualTo(25);
        }

        @Test
        void shouldQueryRepositoryWithGivenUserIdWhenDifferentUserRequestsActivities() {
            when(riskOperationActivityRepository.findByUserId(eq(OTHER_USER_ID), any(Pageable.class)))
                    .thenReturn(List.of());

            riskOperationLogService.getRiskActivity(OTHER_USER_ID, SortType.NEWEST);

            verify(riskOperationActivityRepository).findByUserId(eq(OTHER_USER_ID), any(Pageable.class));
            verify(riskOperationActivityRepository, never()).findByUserId(eq(USER_ID), any(Pageable.class));
        }

        @Test
        void shouldNotInteractWithAuthClientWhenGettingRiskActivity() {
            when(riskOperationActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of());

            riskOperationLogService.getRiskActivity(USER_ID, SortType.NEWEST);

            verifyNoInteractions(authBackendClient);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFailsWhileFetchingActivities() {
            when(riskOperationActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenThrow(new IllegalStateException("query failed"));

            assertThrows(IllegalStateException.class,
                    () -> riskOperationLogService.getRiskActivity(USER_ID, SortType.NEWEST));

            verifyNoInteractions(riskOperationActivityMapper);
        }

        @Test
        void shouldThrowExceptionWhenMapperFailsWhileMappingActivity() {
            when(riskOperationActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(List.of(firstActivity));
            when(riskOperationActivityMapper.mapToRiskOperationActivity(firstActivity))
                    .thenThrow(new IllegalStateException("mapping failed"));

            assertThrows(IllegalStateException.class,
                    () -> riskOperationLogService.getRiskActivity(USER_ID, SortType.NEWEST));
        }
    }
}