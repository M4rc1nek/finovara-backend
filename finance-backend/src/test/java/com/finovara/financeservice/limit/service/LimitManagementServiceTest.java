package com.finovara.financeservice.limit.service;

import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.activity.event.limit.LimitActivityEvent;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.limit.dto.LimitDto;
import com.finovara.financeservice.limit.dto.LimitStatsDto;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.limit.model.LimitStatus;
import com.finovara.financeservice.limit.repository.LimitRepository;
import com.finovara.financeservice.util.limit.validator.LimitExpensesValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
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
class LimitManagementServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long LIMIT_ID = 10L;
    private static final Long OTHER_LIMIT_ID = 20L;
    private static final Long FOREIGN_USER_ID = 999L;

    @Mock
    private LimitRepository limitRepository;

    @Mock
    private LimitCalculateService limitCalculateService;

    @Mock
    private LimitExpensesValidator limitExpensesValidator;

    @Mock
    private OutboxService outboxService;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @InjectMocks
    private LimitManagementService limitManagementService;

    private Limit limit;
    private LimitDto limitDto;

    @BeforeEach
    void setUp() {
        limit = Limit.builder()
                .id(LIMIT_ID)
                .userId(USER_ID)
                .periodType(PeriodType.MONTHLY)
                .category(ExpenseCategory.FOOD)
                .amount(BigDecimal.valueOf(500))
                .isActive(true)
                .build();

        limitDto = new LimitDto(USER_ID, LIMIT_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD, LimitStatus.LOW, BigDecimal.valueOf(700), true, null);
    }

    @Nested
    class CreateLimit {

        @Test
        void shouldCreateGeneralLimitWhenGeneralLimitDoesNotExist() {
            LimitDto dto = new LimitDto(USER_ID, null, PeriodType.MONTHLY, null, null, BigDecimal.valueOf(1000), true, null);
            Limit savedLimit = Limit.builder().id(LIMIT_ID).userId(USER_ID).periodType(dto.periodType()).category(null).amount(dto.amount()).isActive(true).build();

            when(limitRepository.findGeneralLimit(USER_ID, PeriodType.MONTHLY)).thenReturn(Optional.empty());
            when(limitRepository.save(any(Limit.class))).thenReturn(savedLimit);

            Long result = limitManagementService.createLimit(dto, USER_ID);

            assertEquals(LIMIT_ID, result);
            verify(limitExpensesValidator).validateCurrentExpensesDoNotExceedLimit(USER_ID, dto);
            verify(limitRepository).save(any(Limit.class));
            verify(outboxService).save(eq("Limit"), eq(LIMIT_ID.toString()), eq("activity.limit"), any(LimitActivityEvent.class));
        }

        @Test
        void shouldCreateCategoryLimitWhenCategoryLimitDoesNotExist() {
            when(limitRepository.findCategoryLimit(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD)).thenReturn(Optional.empty());
            when(limitRepository.save(any(Limit.class))).thenReturn(limit);

            Long result = limitManagementService.createLimit(limitDto, USER_ID);

            assertEquals(LIMIT_ID, result);
            verify(limitExpensesValidator).validateCurrentExpensesDoNotExceedLimit(USER_ID, limitDto);
            verify(limitRepository).save(any(Limit.class));
            verify(outboxService).save(eq("Limit"), eq(LIMIT_ID.toString()), eq("activity.limit"), any(LimitActivityEvent.class));
        }

        @Test
        void shouldThrowEntityAlreadyExistsExceptionWhenGeneralLimitAlreadyExists() {
            LimitDto dto = new LimitDto(USER_ID, null, PeriodType.MONTHLY, null, null, BigDecimal.valueOf(1000), true, null);

            when(limitRepository.findGeneralLimit(USER_ID, PeriodType.MONTHLY)).thenReturn(Optional.of(limit));

            assertThrows(EntityAlreadyExistsException.class, () -> limitManagementService.createLimit(dto, USER_ID));

            verify(limitRepository, never()).save(any());
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowEntityAlreadyExistsExceptionWhenCategoryLimitAlreadyExists() {
            when(limitRepository.findCategoryLimit(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD)).thenReturn(Optional.of(limit));

            assertThrows(EntityAlreadyExistsException.class, () -> limitManagementService.createLimit(limitDto, USER_ID));

            verify(limitRepository, never()).save(any());
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowInvalidInputExceptionWhenCurrentExpensesExceedLimit() {
            LimitDto dto = new LimitDto(USER_ID, null, PeriodType.MONTHLY, ExpenseCategory.FOOD, null, BigDecimal.valueOf(700), true, null);

            doThrow(new InvalidInputException("Current expenses exceed limit"))
                    .when(limitExpensesValidator).validateCurrentExpensesDoNotExceedLimit(USER_ID, dto);

            assertThrows(InvalidInputException.class, () -> limitManagementService.createLimit(dto, USER_ID));

            verify(limitRepository, never()).findGeneralLimit(any(), any());
            verify(limitRepository, never()).findCategoryLimit(any(), any(), any());
            verify(limitRepository, never()).save(any());
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldSetIsActiveTrueWhenCreatingLimit() {
            when(limitRepository.findCategoryLimit(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD)).thenReturn(Optional.empty());
            when(limitRepository.save(any(Limit.class))).thenReturn(limit);

            limitManagementService.createLimit(limitDto, USER_ID);

            verify(limitRepository).save(ArgumentMatchers.argThat(Limit::getIsActive));
        }
    }

    @Nested
    class EditLimit {

        @Test
        void shouldEditLimitWhenLimitExists() {
            when(limitRepository.findByIdAndUserId(USER_ID, LIMIT_ID)).thenReturn(Optional.of(limit));
            when(limitRepository.save(any(Limit.class))).thenReturn(limit);

            Long result = limitManagementService.editLimit(limitDto, LIMIT_ID, USER_ID);

            assertEquals(LIMIT_ID, result);
            assertEquals(limitDto.amount(), limit.getAmount());
            assertEquals(limitDto.category(), limit.getCategory());
            assertEquals(limitDto.periodType(), limit.getPeriodType());

            verify(limitExpensesValidator).validateCurrentExpensesDoNotExceedLimit(USER_ID, limitDto);
            verify(limitRepository).save(limit);
            verify(outboxService).save(eq("Limit"), eq(LIMIT_ID.toString()), eq("activity.limit"), any(LimitActivityEvent.class));
        }

        @Test
        void shouldThrowRequestedEntityNotFoundExceptionWhenLimitDoesNotExist() {
            when(limitRepository.findByIdAndUserId(USER_ID, LIMIT_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> limitManagementService.editLimit(limitDto, LIMIT_ID, USER_ID));

            verify(limitRepository, never()).save(any());
            verifyNoInteractions(outboxService);
            verifyNoInteractions(limitExpensesValidator);
        }

        @Test
        void shouldThrowInvalidInputExceptionWhenCurrentExpensesExceedLimit() {
            when(limitRepository.findByIdAndUserId(USER_ID, LIMIT_ID)).thenReturn(Optional.of(limit));
            doThrow(new InvalidInputException("Current expenses exceed limit"))
                    .when(limitExpensesValidator).validateCurrentExpensesDoNotExceedLimit(USER_ID, limitDto);

            assertThrows(InvalidInputException.class, () -> limitManagementService.editLimit(limitDto, LIMIT_ID, USER_ID));

            verify(limitRepository, never()).save(any());
            verifyNoInteractions(outboxService);
        }
    }

    @Nested
    class GetLimitStats {

        @Test
        void shouldReturnEmptyListWhenUserHasNoLimits() {
            when(limitRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

            List<LimitStatsDto> result = limitManagementService.getLimitStats(USER_ID);

            assertTrue(result.isEmpty());
            verifyNoInteractions(limitCalculateService);
        }

        @Test
        void shouldReturnCalculatedStatsForAllLimits() {
            Limit secondLimit = Limit.builder().id(OTHER_LIMIT_ID).userId(USER_ID).amount(BigDecimal.valueOf(300)).build();

            LimitStatsDto firstStats = mock(LimitStatsDto.class);
            LimitStatsDto secondStats = mock(LimitStatsDto.class);

            when(limitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(limit, secondLimit));
            when(limitCalculateService.calculateLimitStats(eq(USER_ID), eq(LIMIT_ID), any(LocalDate.class))).thenReturn(firstStats);
            when(limitCalculateService.calculateLimitStats(eq(USER_ID), eq(OTHER_LIMIT_ID), any(LocalDate.class))).thenReturn(secondStats);

            List<LimitStatsDto> result = limitManagementService.getLimitStats(USER_ID);

            assertEquals(2, result.size());
            assertEquals(firstStats, result.get(0));
            assertEquals(secondStats, result.get(1));
        }

        @Test
        void shouldCalculateStatsForEveryLimitWhenMultipleLimitsExist() {
            Limit secondLimit = Limit.builder().id(OTHER_LIMIT_ID).userId(USER_ID).build();

            when(limitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(limit, secondLimit));
            when(limitCalculateService.calculateLimitStats(anyLong(), anyLong(), any(LocalDate.class))).thenReturn(mock(LimitStatsDto.class));

            limitManagementService.getLimitStats(USER_ID);

            verify(limitCalculateService, times(2)).calculateLimitStats(anyLong(), anyLong(), any(LocalDate.class));
        }
    }

    @Nested
    class DeleteLimit {

        @Test
        void shouldDeleteLimitWhenLimitExists() {
            when(limitRepository.findByIdAndUserId(USER_ID, LIMIT_ID)).thenReturn(Optional.of(limit));

            limitManagementService.deleteLimit(USER_ID, LIMIT_ID, "authCode");

            verify(limitRepository).delete(limit);
            verify(outboxService).save(eq("Limit"), eq(LIMIT_ID.toString()), eq("activity.limit"), any(LimitActivityEvent.class));
        }

        @Test
        void shouldThrowRequestedEntityNotFoundExceptionWhenDeletingNonExistingLimit() {
            when(limitRepository.findByIdAndUserId(USER_ID, LIMIT_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> limitManagementService.deleteLimit(USER_ID, LIMIT_ID, "authCode"));

            verify(limitRepository, never()).delete(any());
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowRequestedEntityNotFoundExceptionWhenLimitBelongsToAnotherUser() {
            when(limitRepository.findByIdAndUserId(USER_ID, LIMIT_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> limitManagementService.deleteLimit(USER_ID, LIMIT_ID, "authCode"));

            verify(limitRepository, never()).delete(any());
            verifyNoInteractions(outboxService);
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteAllUserLimitsWhenDeletingUserData() {
            limitManagementService.deleteByUserId(USER_ID);

            verify(limitRepository).deleteByUserId(USER_ID);
        }
    }
}