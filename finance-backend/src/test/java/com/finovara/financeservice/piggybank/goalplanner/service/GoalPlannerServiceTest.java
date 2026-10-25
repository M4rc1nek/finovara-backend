package com.finovara.financeservice.piggybank.goalplanner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.piggybank.goalplanner.dto.GoalPlannerCompletionSummaryDto;
import com.finovara.financeservice.piggybank.goalplanner.dto.GoalPlannerDto;
import com.finovara.financeservice.piggybank.goalplanner.dto.GoalPlannerSummaryDto;
import com.finovara.financeservice.piggybank.goalplanner.mapper.GoalPlannerMapper;
import com.finovara.financeservice.piggybank.goalplanner.model.GoalPlanner;
import com.finovara.financeservice.piggybank.goalplanner.repository.GoalPlannerRepository;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.piggybank.repository.PiggyBankRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoalPlannerServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PIGGY_BANK_ID = 10L;
    private static final Long GOAL_PLANNER_ID = 7L;
    private static final BigDecimal GOAL_AMOUNT = new BigDecimal("1000.00");
    private static final LocalDateTime COMPLETED_AT = LocalDateTime.of(2024, 1, 1, 10, 0);

    @Mock
    private GoalPlannerRepository goalPlannerRepository;
    @Mock
    private PiggyBankRepository piggyBankRepository;
    @Mock
    private GoalPlannerMapper goalPlannerMapper;
    @Mock
    private GoalPlannerDto goalPlannerDto;
    @Mock
    private GoalPlannerSummaryDto goalPlannerSummaryDto;
    @Mock
    private GoalPlannerCompletionSummaryDto goalPlannerCompletionSummaryDto;

    @InjectMocks
    private GoalPlannerService service;

    private PiggyBank piggyBank;
    private GoalPlanner goalPlanner;

    @BeforeEach
    void setUp() {
        piggyBank = PiggyBank.builder()
                .id(PIGGY_BANK_ID)
                .userId(USER_ID)
                .amount(new BigDecimal("500.00"))
                .goalAmount(GOAL_AMOUNT)
                .build();
        goalPlanner = GoalPlanner.builder()
                .id(GOAL_PLANNER_ID)
                .build();
    }

    @Nested
    class CreateGoalPlanner {

        @Nested
        class WhenPiggyBankHasNoPlanner {

            @BeforeEach
            void setUp() {
                when(goalPlannerDto.piggyBankId()).thenReturn(PIGGY_BANK_ID);
                when(piggyBankRepository.findByIdAndUserIdForUpdate(PIGGY_BANK_ID, USER_ID)).thenReturn(Optional.of(piggyBank));
                when(goalPlannerRepository.save(any(GoalPlanner.class))).thenReturn(goalPlanner);
            }

            @Test
            void shouldReturnGoalPlannerIdWhenPiggyBankHasNoPlanner() {
                Long result = service.createGoalPlanner(USER_ID, goalPlannerDto);

                assertEquals(GOAL_PLANNER_ID, result);
            }

            @Test
            void shouldAssignGoalPlannerToPiggyBankWhenPiggyBankHasNoPlanner() {
                service.createGoalPlanner(USER_ID, goalPlannerDto);

                assertSame(goalPlanner, piggyBank.getGoalPlanner());
            }

            @Test
            void shouldSaveGoalPlannerWithUserIdAndCreationDateWhenPiggyBankHasNoPlanner() {
                ArgumentCaptor<GoalPlanner> captor = ArgumentCaptor.forClass(GoalPlanner.class);

                service.createGoalPlanner(USER_ID, goalPlannerDto);

                verify(goalPlannerRepository).save(captor.capture());
                assertEquals(USER_ID, captor.getValue().getUserId());
                assertNotNull(captor.getValue().getCreatedAt());
            }

            @Test
            void shouldNotMarkGoalAsCompletedWhenAmountIsBelowGoalAmount() {
                service.createGoalPlanner(USER_ID, goalPlannerDto);

                assertNull(goalPlanner.getCompletedAt());
            }

            @Test
            void shouldMarkGoalAsCompletedWhenAmountEqualsGoalAmount() {
                piggyBank.setAmount(GOAL_AMOUNT);

                service.createGoalPlanner(USER_ID, goalPlannerDto);

                assertNotNull(goalPlanner.getCompletedAt());
            }

            @Test
            void shouldMarkGoalAsCompletedWhenAmountExceedsGoalAmount() {
                piggyBank.setAmount(new BigDecimal("1500.00"));

                service.createGoalPlanner(USER_ID, goalPlannerDto);

                assertNotNull(goalPlanner.getCompletedAt());
            }

            @Test
            void shouldNotMarkGoalAsCompletedWhenGoalAmountIsNull() {
                piggyBank.setGoalAmount(null);

                service.createGoalPlanner(USER_ID, goalPlannerDto);

                assertNull(goalPlanner.getCompletedAt());
            }
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankDoesNotExist() {
            when(goalPlannerDto.piggyBankId()).thenReturn(PIGGY_BANK_ID);
            when(piggyBankRepository.findByIdAndUserIdForUpdate(PIGGY_BANK_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.createGoalPlanner(USER_ID, goalPlannerDto));

            verifyNoInteractions(goalPlannerRepository);
        }

        @Test
        void shouldThrowExceptionWhenGoalPlannerAlreadyExists() {
            piggyBank.setGoalPlanner(goalPlanner);
            when(goalPlannerDto.piggyBankId()).thenReturn(PIGGY_BANK_ID);
            when(piggyBankRepository.findByIdAndUserIdForUpdate(PIGGY_BANK_ID, USER_ID)).thenReturn(Optional.of(piggyBank));

            assertThrows(EntityAlreadyExistsException.class, () -> service.createGoalPlanner(USER_ID, goalPlannerDto));

            verify(goalPlannerRepository, never()).save(any(GoalPlanner.class));
        }
    }

    @Nested
    class GetGoalPlanner {

        @Test
        void shouldReturnGoalPlannerDtoWhenPlannerExists() {
            piggyBank.setGoalPlanner(goalPlanner);
            when(piggyBankRepository.findByIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(Optional.of(piggyBank));
            when(goalPlannerMapper.toDto(goalPlanner, piggyBank)).thenReturn(goalPlannerDto);

            GoalPlannerDto result = service.getGoalPlanner(USER_ID, PIGGY_BANK_ID);

            assertSame(goalPlannerDto, result);
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankDoesNotExist() {
            when(piggyBankRepository.findByIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.getGoalPlanner(USER_ID, PIGGY_BANK_ID));

            verifyNoInteractions(goalPlannerMapper);
        }

        @Test
        void shouldThrowExceptionWhenGoalPlannerDoesNotExist() {
            when(piggyBankRepository.findByIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(Optional.of(piggyBank));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.getGoalPlanner(USER_ID, PIGGY_BANK_ID));

            verifyNoInteractions(goalPlannerMapper);
        }
    }

    @Nested
    class GetGoalPlannerSummary {

        @Test
        void shouldReturnSummaryDtoWhenPlannerExists() {
            piggyBank.setGoalPlanner(goalPlanner);
            when(piggyBankRepository.findByIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(Optional.of(piggyBank));
            when(goalPlannerMapper.toSummaryDto(goalPlanner, piggyBank)).thenReturn(goalPlannerSummaryDto);

            GoalPlannerSummaryDto result = service.getGoalPlannerSummary(USER_ID, PIGGY_BANK_ID);

            assertSame(goalPlannerSummaryDto, result);
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankDoesNotExist() {
            when(piggyBankRepository.findByIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.getGoalPlannerSummary(USER_ID, PIGGY_BANK_ID));

            verifyNoInteractions(goalPlannerMapper);
        }

        @Test
        void shouldThrowExceptionWhenGoalPlannerDoesNotExist() {
            when(piggyBankRepository.findByIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(Optional.of(piggyBank));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.getGoalPlannerSummary(USER_ID, PIGGY_BANK_ID));

            verifyNoInteractions(goalPlannerMapper);
        }
    }

    @Nested
    class GetGoalCompletionSummary {

        @Test
        void shouldReturnCompletionDtoWhenGoalIsCompleted() {
            goalPlanner.setCompletedAt(COMPLETED_AT);
            piggyBank.setGoalPlanner(goalPlanner);
            when(piggyBankRepository.findByIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(Optional.of(piggyBank));
            when(goalPlannerMapper.toCompletionDto(goalPlanner, piggyBank)).thenReturn(goalPlannerCompletionSummaryDto);

            GoalPlannerCompletionSummaryDto result = service.getGoalCompletionSummary(USER_ID, PIGGY_BANK_ID);

            assertSame(goalPlannerCompletionSummaryDto, result);
        }

        @Test
        void shouldThrowExceptionWhenGoalIsNotCompleted() {
            piggyBank.setGoalPlanner(goalPlanner);
            when(piggyBankRepository.findByIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(Optional.of(piggyBank));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.getGoalCompletionSummary(USER_ID, PIGGY_BANK_ID));

            verifyNoInteractions(goalPlannerMapper);
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankDoesNotExist() {
            when(piggyBankRepository.findByIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.getGoalCompletionSummary(USER_ID, PIGGY_BANK_ID));

            verifyNoInteractions(goalPlannerMapper);
        }

        @Test
        void shouldThrowExceptionWhenGoalPlannerDoesNotExist() {
            when(piggyBankRepository.findByIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(Optional.of(piggyBank));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.getGoalCompletionSummary(USER_ID, PIGGY_BANK_ID));

            verifyNoInteractions(goalPlannerMapper);
        }
    }

    @Nested
    class CheckAndMarkGoalCompletion {

        @BeforeEach
        void setUp() {
            piggyBank.setGoalPlanner(goalPlanner);
        }

        @Test
        void shouldDoNothingWhenGoalPlannerIsNull() {
            piggyBank.setGoalPlanner(null);
            piggyBank.setAmount(GOAL_AMOUNT);

            service.checkAndMarkGoalCompletion(piggyBank);

            assertNull(piggyBank.getGoalPlanner());
        }

        @Test
        void shouldMarkGoalAsCompletedWhenAmountEqualsGoalAmount() {
            piggyBank.setAmount(GOAL_AMOUNT);

            service.checkAndMarkGoalCompletion(piggyBank);

            assertNotNull(goalPlanner.getCompletedAt());
        }

        @Test
        void shouldMarkGoalAsCompletedWhenAmountExceedsGoalAmount() {
            piggyBank.setAmount(new BigDecimal("1000.01"));

            service.checkAndMarkGoalCompletion(piggyBank);

            assertNotNull(goalPlanner.getCompletedAt());
        }

        @Test
        void shouldKeepCompletionDateWhenGoalIsReachedAndAlreadyCompleted() {
            goalPlanner.setCompletedAt(COMPLETED_AT);
            piggyBank.setAmount(GOAL_AMOUNT);

            service.checkAndMarkGoalCompletion(piggyBank);

            assertEquals(COMPLETED_AT, goalPlanner.getCompletedAt());
        }

        @Test
        void shouldResetCompletionDateWhenGoalIsNoLongerReached() {
            goalPlanner.setCompletedAt(COMPLETED_AT);
            piggyBank.setAmount(new BigDecimal("999.99"));

            service.checkAndMarkGoalCompletion(piggyBank);

            assertNull(goalPlanner.getCompletedAt());
        }

        @Test
        void shouldKeepGoalNotCompletedWhenGoalIsNotReached() {
            piggyBank.setAmount(new BigDecimal("999.99"));

            service.checkAndMarkGoalCompletion(piggyBank);

            assertNull(goalPlanner.getCompletedAt());
        }

        @Test
        void shouldResetCompletionDateWhenGoalAmountIsNull() {
            goalPlanner.setCompletedAt(COMPLETED_AT);
            piggyBank.setGoalAmount(null);

            service.checkAndMarkGoalCompletion(piggyBank);

            assertNull(goalPlanner.getCompletedAt());
        }

        @Test
        void shouldKeepGoalNotCompletedWhenGoalAmountIsNull() {
            piggyBank.setGoalAmount(null);

            service.checkAndMarkGoalCompletion(piggyBank);

            assertNull(goalPlanner.getCompletedAt());
        }
    }
}