package com.finovara.financeservice.piggybank.goalplanner.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.piggybank.goalplanner.dto.GoalPlannerCompletionSummaryDto;
import com.finovara.financeservice.piggybank.goalplanner.dto.GoalPlannerDto;
import com.finovara.financeservice.piggybank.goalplanner.dto.GoalPlannerSummaryDto;
import com.finovara.financeservice.piggybank.goalplanner.mapper.GoalPlannerMapper;
import com.finovara.financeservice.piggybank.goalplanner.model.GoalPlanner;
import com.finovara.financeservice.piggybank.goalplanner.repository.GoalPlannerRepository;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.util.transaction.piggybank.manager.PiggyBankManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalPlannerServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PIGGY_BANK_ID = 2L;
    private static final Long GOAL_PLANNER_ID = 3L;

    @Mock
    private GoalPlannerRepository goalPlannerRepository;

    @Mock
    private PiggyBankManagerService piggyBankManagerService;

    @Mock
    private GoalPlannerMapper goalPlannerMapper;

    @InjectMocks
    private GoalPlannerService goalPlannerService;

    @BeforeEach
    void setUp() {
        goalPlannerService = new GoalPlannerService(goalPlannerRepository, piggyBankManagerService, goalPlannerMapper);
    }

    @Nested
    class CreateGoalPlanner {

        @Test
        void shouldReturnGoalPlannerIdWhenPiggyBankExists() {
            PiggyBank piggyBank = PiggyBank.builder().id(PIGGY_BANK_ID).build();
            GoalPlannerDto dto = new GoalPlannerDto(null, PIGGY_BANK_ID, "Wakacje", null, LocalDate.now().plusDays(30), null);
            GoalPlanner savedGoalPlanner = GoalPlanner.builder().id(GOAL_PLANNER_ID).build();

            when(piggyBankManagerService.getPiggyBankByUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(piggyBank);
            when(goalPlannerRepository.save(any(GoalPlanner.class))).thenReturn(savedGoalPlanner);

            Long result = goalPlannerService.createGoalPlanner(USER_ID, dto);

            assertEquals(GOAL_PLANNER_ID, result);
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankNotFound() {
            GoalPlannerDto dto = new GoalPlannerDto(null, PIGGY_BANK_ID, "Wakacje", null, LocalDate.now().plusDays(30), null);

            when(piggyBankManagerService.getPiggyBankByUserId(PIGGY_BANK_ID, USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("Piggy bank not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> goalPlannerService.createGoalPlanner(USER_ID, dto));
        }
    }

    @Nested
    class GetGoalPlanner {

        @Test
        void shouldReturnGoalPlannerDtoWhenExists() {
            GoalPlanner goalPlanner = GoalPlanner.builder().id(GOAL_PLANNER_ID).build();
            GoalPlannerDto expectedDto = new GoalPlannerDto(GOAL_PLANNER_ID, PIGGY_BANK_ID, "Wakacje", new BigDecimal("1000"), LocalDate.now().plusDays(30), LocalDateTime.now());

            when(goalPlannerRepository.findByPiggyBankIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(Optional.of(goalPlanner));
            when(goalPlannerMapper.toDto(goalPlanner)).thenReturn(expectedDto);

            GoalPlannerDto result = goalPlannerService.getGoalPlanner(USER_ID, PIGGY_BANK_ID);

            assertEquals(expectedDto, result);
        }

        @Test
        void shouldThrowExceptionWhenGoalPlannerNotFound() {
            when(goalPlannerRepository.findByPiggyBankIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> goalPlannerService.getGoalPlanner(USER_ID, PIGGY_BANK_ID));
        }
    }

    @Nested
    class GetGoalPlannerSummary {

        @Test
        void shouldReturnSummaryDtoWhenGoalPlannerExists() {
            GoalPlanner goalPlanner = GoalPlanner.builder().id(GOAL_PLANNER_ID).build();
            GoalPlannerSummaryDto expectedDto = new GoalPlannerSummaryDto(new BigDecimal("10"), new BigDecimal("70"), new BigDecimal("300"), 15L);

            when(goalPlannerRepository.findByPiggyBankIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(Optional.of(goalPlanner));
            when(goalPlannerMapper.toSummaryDto(goalPlanner)).thenReturn(expectedDto);

            GoalPlannerSummaryDto result = goalPlannerService.getGoalPlannerSummary(USER_ID, PIGGY_BANK_ID);

            assertEquals(expectedDto, result);
        }

        @Test
        void shouldThrowExceptionWhenGoalPlannerNotFound() {
            when(goalPlannerRepository.findByPiggyBankIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> goalPlannerService.getGoalPlannerSummary(USER_ID, PIGGY_BANK_ID));
        }
    }

    @Nested
    class GetGoalCompletionSummary {

        @Test
        void shouldReturnCompletionDtoWhenGoalIsCompleted() {
            GoalPlanner goalPlanner = GoalPlanner.builder()
                    .id(GOAL_PLANNER_ID)
                    .completedAt(LocalDateTime.now())
                    .build();
            GoalPlannerCompletionSummaryDto expectedDto = new GoalPlannerCompletionSummaryDto(10L, 240L, 14400L, new BigDecimal("1000"), LocalDateTime.now());

            when(goalPlannerRepository.findByPiggyBankIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(Optional.of(goalPlanner));
            when(goalPlannerMapper.toCompletionDto(goalPlanner)).thenReturn(expectedDto);

            GoalPlannerCompletionSummaryDto result = goalPlannerService.getGoalCompletionSummary(USER_ID, PIGGY_BANK_ID);

            assertEquals(expectedDto, result);
        }

        @Test
        void shouldThrowExceptionWhenGoalNotYetCompleted() {
            GoalPlanner goalPlanner = GoalPlanner.builder()
                    .id(GOAL_PLANNER_ID)
                    .completedAt(null)
                    .build();

            when(goalPlannerRepository.findByPiggyBankIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(Optional.of(goalPlanner));

            assertThrows(RequestedEntityNotFoundException.class, () -> goalPlannerService.getGoalCompletionSummary(USER_ID, PIGGY_BANK_ID));
        }

        @Test
        void shouldThrowExceptionWhenGoalPlannerNotFound() {
            when(goalPlannerRepository.findByPiggyBankIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> goalPlannerService.getGoalCompletionSummary(USER_ID, PIGGY_BANK_ID));
        }
    }

    @Nested
    class CheckAndMarkGoalCompletion {

        @Test
        void shouldDoNothingWhenGoalPlannerIsNull() {
            goalPlannerService.checkAndMarkGoalCompletion(null);

            verify(goalPlannerRepository, never()).save(any(GoalPlanner.class));
        }

        @Test
        void shouldDoNothingWhenGoalAlreadyCompletedAndAmountStillMeetsGoal() {
            PiggyBank piggyBank = PiggyBank.builder().amount(new BigDecimal("1000")).goalAmount(new BigDecimal("1000")).build();
            GoalPlanner goalPlanner = GoalPlanner.builder()
                    .piggyBankAssigned(piggyBank)
                    .completedAt(LocalDateTime.now())
                    .build();

            goalPlannerService.checkAndMarkGoalCompletion(goalPlanner);

            verify(goalPlannerRepository, never()).save(any(GoalPlanner.class));
        }

        @Test
        void shouldMarkAsCompletedWhenCurrentAmountEqualsGoalAmount() {
            PiggyBank piggyBank = PiggyBank.builder().amount(new BigDecimal("1000")).goalAmount(new BigDecimal("1000")).build();
            GoalPlanner goalPlanner = GoalPlanner.builder()
                    .piggyBankAssigned(piggyBank)
                    .completedAt(null)
                    .build();

            goalPlannerService.checkAndMarkGoalCompletion(goalPlanner);

            assertNotNull(goalPlanner.getCompletedAt());
            verify(goalPlannerRepository, times(1)).save(goalPlanner);
        }

        @Test
        void shouldMarkAsCompletedWhenCurrentAmountExceedsGoalAmount() {
            PiggyBank piggyBank = PiggyBank.builder().amount(new BigDecimal("1500")).goalAmount(new BigDecimal("1000")).build();
            GoalPlanner goalPlanner = GoalPlanner.builder()
                    .piggyBankAssigned(piggyBank)
                    .completedAt(null)
                    .build();

            goalPlannerService.checkAndMarkGoalCompletion(goalPlanner);

            assertNotNull(goalPlanner.getCompletedAt());
            verify(goalPlannerRepository, times(1)).save(goalPlanner);
        }

        @Test
        void shouldNotMarkAsCompletedWhenCurrentAmountLessThanGoalAmount() {
            PiggyBank piggyBank = PiggyBank.builder().amount(new BigDecimal("500")).goalAmount(new BigDecimal("1000")).build();
            GoalPlanner goalPlanner = GoalPlanner.builder()
                    .piggyBankAssigned(piggyBank)
                    .completedAt(null)
                    .build();

            goalPlannerService.checkAndMarkGoalCompletion(goalPlanner);

            assertNull(goalPlanner.getCompletedAt());
            verify(goalPlannerRepository, never()).save(any(GoalPlanner.class));
        }

        @Test
        void shouldResetCompletedAtWhenCurrentAmountDropsBelowGoal() {
            PiggyBank piggyBank = PiggyBank.builder().amount(new BigDecimal("500")).goalAmount(new BigDecimal("1000")).build();
            GoalPlanner goalPlanner = GoalPlanner.builder()
                    .piggyBankAssigned(piggyBank)
                    .completedAt(LocalDateTime.now())
                    .build();

            goalPlannerService.checkAndMarkGoalCompletion(goalPlanner);

            assertNull(goalPlanner.getCompletedAt());
            verify(goalPlannerRepository, times(1)).save(goalPlanner);
        }

        @Test
        void shouldResetCompletedAtWhenCurrentAmountIsZeroAndGoalIsPositive() {
            PiggyBank piggyBank = PiggyBank.builder().amount(BigDecimal.ZERO).goalAmount(new BigDecimal("1000")).build();
            GoalPlanner goalPlanner = GoalPlanner.builder()
                    .piggyBankAssigned(piggyBank)
                    .completedAt(LocalDateTime.now())
                    .build();

            goalPlannerService.checkAndMarkGoalCompletion(goalPlanner);

            assertNull(goalPlanner.getCompletedAt());
            verify(goalPlannerRepository, times(1)).save(goalPlanner);
        }
    }
}