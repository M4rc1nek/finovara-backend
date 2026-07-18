package com.finovara.financeservice.sharedaccount.limit.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.financeservice.sharedaccount.limit.dto.SharedLimitDto;
import com.finovara.financeservice.sharedaccount.limit.dto.SharedLimitStatsDto;
import com.finovara.financeservice.sharedaccount.limit.model.SharedLimit;
import com.finovara.financeservice.sharedaccount.limit.repository.SharedLimitRepository;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.util.limit.validator.LimitExpensesValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedLimitManagementServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long LIMIT_ID = 10L;

    @Mock
    private SharedLimitRepository sharedLimitRepository;

    @Mock
    private SharedLimitCalculateService sharedLimitCalculateService;

    @Mock
    private SharedAccountParticipantsService sharedAccountParticipantsService;

    @Mock
    private LimitExpensesValidator limitExpensesValidator;

    @InjectMocks
    private SharedLimitManagementService sharedLimitManagementService;

    private SharedLimit limit;

    @BeforeEach
    void setUp() {
        limit = SharedLimit.builder()
                .id(LIMIT_ID)
                .ownerId(OWNER_ID)
                .memberId(MEMBER_ID)
                .periodType(PeriodType.MONTHLY)
                .category(ExpenseCategory.FOOD)
                .amount(BigDecimal.valueOf(500))
                .isActive(true)
                .build();
    }

    @Nested
    class CreateSharedLimit {

        @Test
        void shouldCreateGeneralLimitWhenGeneralLimitDoesNotExist() {
            SharedLimitDto dto = mock(SharedLimitDto.class);
            when(dto.periodType()).thenReturn(PeriodType.MONTHLY);
            when(dto.category()).thenReturn(null);
            when(dto.amount()).thenReturn(BigDecimal.valueOf(1000));

            SharedAccountParticipantsResponse participants = mock(SharedAccountParticipantsResponse.class);
            when(participants.ownerId()).thenReturn(OWNER_ID);
            when(participants.memberId()).thenReturn(MEMBER_ID);

            SharedLimit savedLimit = SharedLimit.builder().id(LIMIT_ID).ownerId(OWNER_ID).memberId(MEMBER_ID).periodType(PeriodType.MONTHLY).category(null).amount(BigDecimal.valueOf(1000)).isActive(true).build();

            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
            when(sharedLimitRepository.findGeneralLimit(USER_ID, PeriodType.MONTHLY)).thenReturn(Optional.empty());
            when(sharedLimitRepository.save(any(SharedLimit.class))).thenReturn(savedLimit);

            Long result = sharedLimitManagementService.createSharedLimit(dto, USER_ID);

            assertEquals(LIMIT_ID, result);
            verify(limitExpensesValidator).validateCurrentSharedExpensesDoNotExceedLimit(USER_ID, dto);
            verify(sharedLimitRepository).save(any(SharedLimit.class));
        }

        @Test
        void shouldCreateCategoryLimitWhenCategoryLimitDoesNotExist() {
            SharedLimitDto dto = mock(SharedLimitDto.class);
            when(dto.periodType()).thenReturn(PeriodType.MONTHLY);
            when(dto.category()).thenReturn(ExpenseCategory.FOOD);
            when(dto.amount()).thenReturn(BigDecimal.valueOf(700));

            SharedAccountParticipantsResponse participants = mock(SharedAccountParticipantsResponse.class);
            when(participants.ownerId()).thenReturn(OWNER_ID);
            when(participants.memberId()).thenReturn(MEMBER_ID);

            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
            when(sharedLimitRepository.findCategoryLimit(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD)).thenReturn(Optional.empty());
            when(sharedLimitRepository.save(any(SharedLimit.class))).thenReturn(limit);

            Long result = sharedLimitManagementService.createSharedLimit(dto, USER_ID);

            assertEquals(LIMIT_ID, result);
            verify(limitExpensesValidator).validateCurrentSharedExpensesDoNotExceedLimit(USER_ID, dto);
            verify(sharedLimitRepository).save(any(SharedLimit.class));
        }

        @Test
        void shouldThrowEntityAlreadyExistsExceptionWhenGeneralLimitAlreadyExists() {
            SharedLimitDto dto = mock(SharedLimitDto.class);
            when(dto.periodType()).thenReturn(PeriodType.MONTHLY);
            when(dto.category()).thenReturn(null);

            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(mock(SharedAccountParticipantsResponse.class));
            when(sharedLimitRepository.findGeneralLimit(USER_ID, PeriodType.MONTHLY)).thenReturn(Optional.of(limit));

            assertThrows(EntityAlreadyExistsException.class, () -> sharedLimitManagementService.createSharedLimit(dto, USER_ID));

            verify(sharedLimitRepository, never()).save(any());
        }

        @Test
        void shouldThrowEntityAlreadyExistsExceptionWhenCategoryLimitAlreadyExists() {
            SharedLimitDto dto = mock(SharedLimitDto.class);
            when(dto.periodType()).thenReturn(PeriodType.MONTHLY);
            when(dto.category()).thenReturn(ExpenseCategory.FOOD);

            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(mock(SharedAccountParticipantsResponse.class));
            when(sharedLimitRepository.findCategoryLimit(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD)).thenReturn(Optional.of(limit));

            assertThrows(EntityAlreadyExistsException.class, () -> sharedLimitManagementService.createSharedLimit(dto, USER_ID));

            verify(sharedLimitRepository, never()).save(any());
        }

        @Test
        void shouldThrowInvalidInputExceptionWhenValidatorRejectsLimit() {
            SharedLimitDto dto = mock(SharedLimitDto.class);
            doThrow(new InvalidInputException("exceeds"))
                    .when(limitExpensesValidator).validateCurrentSharedExpensesDoNotExceedLimit(USER_ID, dto);

            assertThrows(InvalidInputException.class, () -> sharedLimitManagementService.createSharedLimit(dto, USER_ID));

            verifyNoInteractions(sharedAccountParticipantsService);
            verify(sharedLimitRepository, never()).save(any());
        }
    }

    @Nested
    class EditSharedLimit {

        private SharedLimitDto limitDto;

        @Test
        void shouldEditLimitWhenLimitExists() {
            limitDto = mock(SharedLimitDto.class);
            when(limitDto.periodType()).thenReturn(PeriodType.MONTHLY);
            when(limitDto.category()).thenReturn(ExpenseCategory.FOOD);
            when(limitDto.amount()).thenReturn(BigDecimal.valueOf(700));


            when(sharedLimitRepository.findByIdAndUserId(USER_ID, LIMIT_ID)).thenReturn(Optional.of(limit));
            when(sharedLimitRepository.save(any(SharedLimit.class))).thenReturn(limit);

            Long result = sharedLimitManagementService.editSharedLimit(limitDto, LIMIT_ID, USER_ID);

            assertEquals(LIMIT_ID, result);
            assertEquals(limitDto.amount(), limit.getAmount());
            assertEquals(limitDto.category(), limit.getCategory());
            assertEquals(limitDto.periodType(), limit.getPeriodType());

            verify(limitExpensesValidator).validateCurrentSharedExpensesDoNotExceedLimit(USER_ID, limitDto);
            verify(sharedLimitRepository).save(limit);
        }

        @Test
        void shouldThrowRequestedEntityNotFoundExceptionWhenLimitDoesNotExist() {
            assertThrows(RequestedEntityNotFoundException.class, () -> sharedLimitManagementService.editSharedLimit(limitDto, LIMIT_ID, USER_ID));

            verify(sharedLimitRepository, never()).save(any());
            verifyNoInteractions(limitExpensesValidator);
        }

    }

    @Nested
    class GetSharedLimitStats {

        @Test
        void shouldReturnEmptyListWhenUserHasNoLimits() {
            when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

            List<SharedLimitStatsDto> result = sharedLimitManagementService.getSharedLimitStats(USER_ID);

            assertTrue(result.isEmpty());
            verifyNoInteractions(sharedLimitCalculateService);
        }

        @Test
        void shouldReturnCalculatedStatsForAllLimits() {
            SharedLimit secondLimit = SharedLimit.builder().id(20L).ownerId(OWNER_ID).memberId(MEMBER_ID).amount(BigDecimal.valueOf(300)).build();

            SharedLimitStatsDto firstStats = mock(SharedLimitStatsDto.class);
            SharedLimitStatsDto secondStats = mock(SharedLimitStatsDto.class);

            when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(limit, secondLimit));
            when(sharedLimitCalculateService.calculateLimitStats(eq(USER_ID), eq(LIMIT_ID), any(LocalDate.class))).thenReturn(firstStats);
            when(sharedLimitCalculateService.calculateLimitStats(eq(USER_ID), eq(20L), any(LocalDate.class))).thenReturn(secondStats);

            List<SharedLimitStatsDto> result = sharedLimitManagementService.getSharedLimitStats(USER_ID);

            assertEquals(2, result.size());
            assertEquals(firstStats, result.get(0));
            assertEquals(secondStats, result.get(1));
        }

        @Test
        void shouldCalculateStatsForEveryLimitWhenMultipleLimitsExist() {
            SharedLimit secondLimit = SharedLimit.builder().id(20L).ownerId(OWNER_ID).memberId(MEMBER_ID).build();

            when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(limit, secondLimit));
            when(sharedLimitCalculateService.calculateLimitStats(anyLong(), anyLong(), any(LocalDate.class))).thenReturn(mock(SharedLimitStatsDto.class));

            sharedLimitManagementService.getSharedLimitStats(USER_ID);

            verify(sharedLimitCalculateService, times(2)).calculateLimitStats(anyLong(), anyLong(), any(LocalDate.class));
        }
    }

    @Nested
    class DeleteSharedLimit {

        @Test
        void shouldDeleteLimitWhenLimitExists() {
            when(sharedLimitRepository.findByIdAndUserId(USER_ID, LIMIT_ID)).thenReturn(Optional.of(limit));

            sharedLimitManagementService.deleteSharedLimit(USER_ID, LIMIT_ID);

            verify(sharedLimitRepository).delete(limit);
        }

        @Test
        void shouldThrowRequestedEntityNotFoundExceptionWhenDeletingNonExistingLimit() {
            when(sharedLimitRepository.findByIdAndUserId(USER_ID, LIMIT_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> sharedLimitManagementService.deleteSharedLimit(USER_ID, LIMIT_ID));

            verify(sharedLimitRepository, never()).delete(any());
        }
    }
}