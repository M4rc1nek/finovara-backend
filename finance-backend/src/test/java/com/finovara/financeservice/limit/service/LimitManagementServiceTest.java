package com.finovara.financeservice.limit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.mainaccount.activity.event.limit.LimitActivityEvent;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.limit.dto.LimitDto;
import com.finovara.financeservice.limit.dto.LimitStatsDto;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.limit.model.LimitStatus;
import com.finovara.financeservice.limit.repository.LimitRepository;
import com.finovara.financeservice.util.limit.manager.LimitManagerService;
import com.finovara.financeservice.util.limit.validator.LimitExpensesValidator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LimitManagementServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long LIMIT_ID = 10L;
    private static final Long OTHER_LIMIT_ID = 20L;
    private static final String AUTH_CODE = "123456";
    private static final String AGGREGATE = "Limit";
    private static final String OUTBOX_TOPIC = "limit.changed";
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(500);
    private static final BigDecimal NEW_AMOUNT = BigDecimal.valueOf(700);

    @Mock
    private LimitRepository limitRepository;

    @Mock
    private LimitManagerService limitManagerService;

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

    @Mock
    private LimitStatsDto firstStats;

    @Mock
    private LimitStatsDto secondStats;

    @Captor
    private ArgumentCaptor<Limit> limitCaptor;

    @Captor
    private ArgumentCaptor<LocalDate> dateCaptor;

    @InjectMocks
    private LimitManagementService limitManagementService;

    private Limit limit;
    private Limit savedLimit;
    private LimitDto limitDto;
    private LimitDto generalLimitDto;

    @BeforeEach
    void setUp() {
        limit = Limit.builder()
                .id(LIMIT_ID)
                .userId(USER_ID)
                .periodType(PeriodType.MONTHLY)
                .category(ExpenseCategory.FOOD)
                .amount(AMOUNT)
                .isActive(true)
                .build();

        savedLimit = Limit.builder()
                .id(LIMIT_ID)
                .userId(USER_ID)
                .periodType(PeriodType.MONTHLY)
                .category(ExpenseCategory.FOOD)
                .amount(NEW_AMOUNT)
                .isActive(true)
                .build();

        limitDto = new LimitDto(USER_ID, LIMIT_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD, LimitStatus.LOW, NEW_AMOUNT, true, AUTH_CODE);
        generalLimitDto = new LimitDto(USER_ID, null, PeriodType.MONTHLY, null, null, NEW_AMOUNT, true, AUTH_CODE);
    }

    @Nested
    class CreateLimit {

        @Nested
        class CategoryLimit {

            @BeforeEach
            void setUp() {
                when(limitRepository.findCategoryLimit(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD))
                        .thenReturn(Optional.empty());
                when(limitRepository.save(any(Limit.class))).thenReturn(savedLimit);
            }

            @Test
            void shouldReturnSavedLimitIdWhenCategoryLimitIsCreated() {
                Long result = limitManagementService.createLimit(limitDto, USER_ID);

                assertEquals(LIMIT_ID, result);
            }

            @Test
            void shouldSaveActiveLimitWithDtoDataWhenCategoryLimitIsCreated() {
                limitManagementService.createLimit(limitDto, USER_ID);

                verify(limitRepository).save(limitCaptor.capture());
                Limit captured = limitCaptor.getValue();
                assertEquals(PeriodType.MONTHLY, captured.getPeriodType());
                assertEquals(ExpenseCategory.FOOD, captured.getCategory());
                assertEquals(NEW_AMOUNT, captured.getAmount());
                assertEquals(USER_ID, captured.getUserId());
                assertTrue(captured.getIsActive());
            }

            @Test
            void shouldConfirmAuthorizationCodeBeforeValidatingExpensesWhenCategoryLimitIsCreated() {
                limitManagementService.createLimit(limitDto, USER_ID);

                InOrder inOrder = inOrder(authBackendClient, limitExpensesValidator);
                inOrder.verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
                inOrder.verify(limitExpensesValidator).validateCurrentExpensesDoNotExceedLimit(USER_ID, limitDto);
                verify(additionalAuthorizationCodeResolver).resolve(AUTH_CODE);
            }

            @Test
            void shouldSaveOutboxEventWhenCategoryLimitIsCreated() {
                limitManagementService.createLimit(limitDto, USER_ID);

                verify(outboxService).save(eq(AGGREGATE), eq(LIMIT_ID.toString()), eq(OUTBOX_TOPIC), any(LimitActivityEvent.class));
            }

            @Test
            void shouldSaveLimitBeforeOutboxEventWhenCategoryLimitIsCreated() {
                limitManagementService.createLimit(limitDto, USER_ID);

                InOrder inOrder = inOrder(limitRepository, outboxService);
                inOrder.verify(limitRepository).save(any(Limit.class));
                inOrder.verify(outboxService).save(eq(AGGREGATE), eq(LIMIT_ID.toString()), eq(OUTBOX_TOPIC), any(LimitActivityEvent.class));
            }

            @Test
            void shouldNotLookUpGeneralLimitWhenCategoryLimitIsCreated() {
                limitManagementService.createLimit(limitDto, USER_ID);

                verify(limitRepository, never()).findGeneralLimit(any(), any());
            }
        }

        @Nested
        class GeneralLimit {

            @BeforeEach
            void setUp() {
                when(limitRepository.findGeneralLimit(USER_ID, PeriodType.MONTHLY)).thenReturn(Optional.empty());
                when(limitRepository.save(any(Limit.class))).thenReturn(savedLimit);
            }

            @Test
            void shouldReturnSavedLimitIdWhenGeneralLimitIsCreated() {
                Long result = limitManagementService.createLimit(generalLimitDto, USER_ID);

                assertEquals(LIMIT_ID, result);
            }

            @Test
            void shouldSaveActiveLimitWithNullCategoryWhenGeneralLimitIsCreated() {
                limitManagementService.createLimit(generalLimitDto, USER_ID);

                verify(limitRepository).save(limitCaptor.capture());
                Limit captured = limitCaptor.getValue();
                assertNull(captured.getCategory());
                assertEquals(PeriodType.MONTHLY, captured.getPeriodType());
                assertEquals(NEW_AMOUNT, captured.getAmount());
                assertEquals(USER_ID, captured.getUserId());
                assertTrue(captured.getIsActive());
            }

            @Test
            void shouldSaveOutboxEventWhenGeneralLimitIsCreated() {
                limitManagementService.createLimit(generalLimitDto, USER_ID);

                verify(outboxService).save(eq(AGGREGATE), eq(LIMIT_ID.toString()), eq(OUTBOX_TOPIC), any(LimitActivityEvent.class));
            }

            @Test
            void shouldNotLookUpCategoryLimitWhenGeneralLimitIsCreated() {
                limitManagementService.createLimit(generalLimitDto, USER_ID);

                verify(limitRepository, never()).findCategoryLimit(any(), any(), any());
            }
        }

        @Nested
        class Failure {

            @Test
            void shouldThrowExceptionWhenDtoIsNull() {
                assertThrows(NullPointerException.class, () -> limitManagementService.createLimit(null, USER_ID));

                verifyNoInteractions(authBackendClient, limitExpensesValidator, limitRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenAuthorizationCodeConfirmationFails() {
                doThrow(new InvalidInputException("Invalid authorization code"))
                        .when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

                assertThrows(InvalidInputException.class, () -> limitManagementService.createLimit(limitDto, USER_ID));

                verifyNoInteractions(limitExpensesValidator, limitRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenCurrentExpensesExceedLimit() {
                doThrow(new InvalidInputException("Current expenses exceed limit"))
                        .when(limitExpensesValidator).validateCurrentExpensesDoNotExceedLimit(USER_ID, limitDto);

                assertThrows(InvalidInputException.class, () -> limitManagementService.createLimit(limitDto, USER_ID));

                verifyNoInteractions(limitRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenGeneralLimitAlreadyExists() {
                when(limitRepository.findGeneralLimit(USER_ID, PeriodType.MONTHLY)).thenReturn(Optional.of(limit));

                assertThrows(EntityAlreadyExistsException.class, () -> limitManagementService.createLimit(generalLimitDto, USER_ID));

                verify(limitRepository, never()).save(any(Limit.class));
                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldThrowExceptionWhenCategoryLimitAlreadyExists() {
                when(limitRepository.findCategoryLimit(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD))
                        .thenReturn(Optional.of(limit));

                assertThrows(EntityAlreadyExistsException.class, () -> limitManagementService.createLimit(limitDto, USER_ID));

                verify(limitRepository, never()).save(any(Limit.class));
                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldThrowExceptionWhenLimitLookupFails() {
                when(limitRepository.findCategoryLimit(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD))
                        .thenThrow(new IllegalStateException("db failed"));

                assertThrows(IllegalStateException.class, () -> limitManagementService.createLimit(limitDto, USER_ID));

                verify(limitRepository, never()).save(any(Limit.class));
                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldThrowExceptionWhenRepositorySaveFails() {
                when(limitRepository.findCategoryLimit(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD))
                        .thenReturn(Optional.empty());
                when(limitRepository.save(any(Limit.class))).thenThrow(new IllegalStateException("db failed"));

                assertThrows(IllegalStateException.class, () -> limitManagementService.createLimit(limitDto, USER_ID));

                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldThrowExceptionWhenRepositoryReturnsNull() {
                when(limitRepository.findCategoryLimit(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD))
                        .thenReturn(Optional.empty());

                assertThrows(NullPointerException.class, () -> limitManagementService.createLimit(limitDto, USER_ID));

                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldThrowExceptionWhenOutboxServiceFails() {
                when(limitRepository.findCategoryLimit(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD))
                        .thenReturn(Optional.empty());
                when(limitRepository.save(any(Limit.class))).thenReturn(savedLimit);
                doThrow(new IllegalStateException("outbox failed")).when(outboxService)
                        .save(eq(AGGREGATE), eq(LIMIT_ID.toString()), eq(OUTBOX_TOPIC), any(LimitActivityEvent.class));

                assertThrows(IllegalStateException.class, () -> limitManagementService.createLimit(limitDto, USER_ID));
            }
        }

        @Test
        void shouldCreateLimitWhenPeriodTypeIsNull() {
            LimitDto dto = new LimitDto(USER_ID, null, null, ExpenseCategory.FOOD, null, NEW_AMOUNT, true, AUTH_CODE);
            when(limitRepository.findCategoryLimit(USER_ID, null, ExpenseCategory.FOOD)).thenReturn(Optional.empty());
            when(limitRepository.save(any(Limit.class))).thenReturn(savedLimit);

            Long result = limitManagementService.createLimit(dto, USER_ID);

            assertEquals(LIMIT_ID, result);
            verify(limitRepository).save(limitCaptor.capture());
            assertNull(limitCaptor.getValue().getPeriodType());
            verify(outboxService).save(eq(AGGREGATE), eq(LIMIT_ID.toString()), eq(OUTBOX_TOPIC), any(LimitActivityEvent.class));
        }
    }

    @Nested
    class EditLimit {

        @Nested
        class Success {

            @BeforeEach
            void setUp() {
                when(limitManagerService.getLimitByUserIdOrThrow(USER_ID, LIMIT_ID)).thenReturn(limit);
            }

            @Test
            void shouldReturnLimitIdWhenLimitIsEdited() {
                Long result = limitManagementService.editLimit(limitDto, LIMIT_ID, USER_ID);

                assertEquals(LIMIT_ID, result);
            }

            @Test
            void shouldUpdateLimitFieldsWhenLimitIsEdited() {
                limitManagementService.editLimit(generalLimitDto, LIMIT_ID, USER_ID);

                assertEquals(PeriodType.MONTHLY, limit.getPeriodType());
                assertNull(limit.getCategory());
                assertEquals(NEW_AMOUNT, limit.getAmount());
                verify(limitRepository).save(limit);
            }

            @Test
            void shouldKeepUserIdAndActiveFlagWhenLimitIsEdited() {
                limitManagementService.editLimit(limitDto, LIMIT_ID, USER_ID);

                assertEquals(USER_ID, limit.getUserId());
                assertTrue(limit.getIsActive());
            }

            @Test
            void shouldClearPeriodTypeWhenDtoPeriodTypeIsNull() {
                LimitDto dto = new LimitDto(USER_ID, LIMIT_ID, null, ExpenseCategory.FOOD, null, NEW_AMOUNT, true, AUTH_CODE);

                limitManagementService.editLimit(dto, LIMIT_ID, USER_ID);

                assertNull(limit.getPeriodType());
                verify(limitRepository).save(limit);
                verify(outboxService).save(eq(AGGREGATE), eq(LIMIT_ID.toString()), eq(OUTBOX_TOPIC), any(LimitActivityEvent.class));
            }

            @Test
            void shouldConfirmAuthorizationCodeBeforeLoadingAndValidatingWhenLimitIsEdited() {
                limitManagementService.editLimit(limitDto, LIMIT_ID, USER_ID);

                InOrder inOrder = inOrder(authBackendClient, limitManagerService, limitExpensesValidator);
                inOrder.verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
                inOrder.verify(limitManagerService).getLimitByUserIdOrThrow(USER_ID, LIMIT_ID);
                inOrder.verify(limitExpensesValidator).validateCurrentExpensesDoNotExceedLimit(USER_ID, limitDto);
                verify(additionalAuthorizationCodeResolver).resolve(AUTH_CODE);
            }

            @Test
            void shouldSaveOutboxEventWhenLimitIsEdited() {
                limitManagementService.editLimit(limitDto, LIMIT_ID, USER_ID);

                verify(outboxService).save(eq(AGGREGATE), eq(LIMIT_ID.toString()), eq(OUTBOX_TOPIC), any(LimitActivityEvent.class));
            }

            @Test
            void shouldSaveLimitBeforeOutboxEventWhenLimitIsEdited() {
                limitManagementService.editLimit(limitDto, LIMIT_ID, USER_ID);

                InOrder inOrder = inOrder(limitRepository, outboxService);
                inOrder.verify(limitRepository).save(limit);
                inOrder.verify(outboxService).save(eq(AGGREGATE), eq(LIMIT_ID.toString()), eq(OUTBOX_TOPIC), any(LimitActivityEvent.class));
            }
        }

        @Nested
        class Failure {

            @Test
            void shouldThrowExceptionWhenDtoIsNull() {
                assertThrows(NullPointerException.class, () -> limitManagementService.editLimit(null, LIMIT_ID, USER_ID));

                verifyNoInteractions(authBackendClient, limitManagerService, limitExpensesValidator, limitRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenAuthorizationCodeConfirmationFails() {
                doThrow(new InvalidInputException("Invalid authorization code"))
                        .when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

                assertThrows(InvalidInputException.class, () -> limitManagementService.editLimit(limitDto, LIMIT_ID, USER_ID));

                verifyNoInteractions(limitManagerService, limitExpensesValidator, limitRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenLimitDoesNotExist() {
                when(limitManagerService.getLimitByUserIdOrThrow(USER_ID, LIMIT_ID))
                        .thenThrow(new RequestedEntityNotFoundException("Limit not found"));

                assertThrows(RequestedEntityNotFoundException.class, () -> limitManagementService.editLimit(limitDto, LIMIT_ID, USER_ID));

                verifyNoInteractions(limitExpensesValidator, limitRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenCurrentExpensesExceedLimit() {
                when(limitManagerService.getLimitByUserIdOrThrow(USER_ID, LIMIT_ID)).thenReturn(limit);
                doThrow(new InvalidInputException("Current expenses exceed limit"))
                        .when(limitExpensesValidator).validateCurrentExpensesDoNotExceedLimit(USER_ID, limitDto);

                assertThrows(InvalidInputException.class, () -> limitManagementService.editLimit(limitDto, LIMIT_ID, USER_ID));

                assertEquals(AMOUNT, limit.getAmount());
                assertEquals(ExpenseCategory.FOOD, limit.getCategory());
                verifyNoInteractions(limitRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenRepositorySaveFails() {
                when(limitManagerService.getLimitByUserIdOrThrow(USER_ID, LIMIT_ID)).thenReturn(limit);
                when(limitRepository.save(limit)).thenThrow(new IllegalStateException("db failed"));

                assertThrows(IllegalStateException.class, () -> limitManagementService.editLimit(limitDto, LIMIT_ID, USER_ID));

                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldThrowExceptionWhenLimitIdIsNull() {
                when(limitManagerService.getLimitByUserIdOrThrow(USER_ID, null)).thenReturn(limit);

                assertThrows(NullPointerException.class, () -> limitManagementService.editLimit(limitDto, null, USER_ID));

                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldThrowExceptionWhenOutboxServiceFails() {
                when(limitManagerService.getLimitByUserIdOrThrow(USER_ID, LIMIT_ID)).thenReturn(limit);
                doThrow(new IllegalStateException("outbox failed")).when(outboxService)
                        .save(eq(AGGREGATE), eq(LIMIT_ID.toString()), eq(OUTBOX_TOPIC), any(LimitActivityEvent.class));

                assertThrows(IllegalStateException.class, () -> limitManagementService.editLimit(limitDto, LIMIT_ID, USER_ID));
            }
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
        void shouldReturnStatsWhenSingleLimitExists() {
            when(limitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(limit));
            when(limitCalculateService.calculateLimitStats(eq(USER_ID), eq(LIMIT_ID), any(LocalDate.class))).thenReturn(firstStats);

            List<LimitStatsDto> result = limitManagementService.getLimitStats(USER_ID);

            assertEquals(List.of(firstStats), result);
        }

        @Test
        void shouldReturnStatsInRepositoryOrderWhenMultipleLimitsExist() {
            Limit secondLimit = Limit.builder().id(OTHER_LIMIT_ID).userId(USER_ID).amount(BigDecimal.valueOf(300)).build();
            when(limitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(secondLimit, limit));
            when(limitCalculateService.calculateLimitStats(eq(USER_ID), eq(LIMIT_ID), any(LocalDate.class))).thenReturn(firstStats);
            when(limitCalculateService.calculateLimitStats(eq(USER_ID), eq(OTHER_LIMIT_ID), any(LocalDate.class))).thenReturn(secondStats);

            List<LimitStatsDto> result = limitManagementService.getLimitStats(USER_ID);

            assertEquals(2, result.size());
            assertEquals(secondStats, result.get(0));
            assertEquals(firstStats, result.get(1));
        }

        @Test
        void shouldCalculateStatsWithSameDateForEveryLimitWhenMultipleLimitsExist() {
            Limit secondLimit = Limit.builder().id(OTHER_LIMIT_ID).userId(USER_ID).build();
            when(limitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(limit, secondLimit));
            when(limitCalculateService.calculateLimitStats(eq(USER_ID), anyLong(), any(LocalDate.class))).thenReturn(firstStats);

            limitManagementService.getLimitStats(USER_ID);

            verify(limitCalculateService, times(2)).calculateLimitStats(eq(USER_ID), anyLong(), dateCaptor.capture());
            List<LocalDate> dates = dateCaptor.getAllValues();
            assertEquals(dates.get(0), dates.get(1));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(limitRepository.findAllByUserId(USER_ID)).thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> limitManagementService.getLimitStats(USER_ID));

            verifyNoInteractions(limitCalculateService);
        }

        @Test
        void shouldThrowExceptionWhenCalculateServiceFails() {
            when(limitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(limit));
            when(limitCalculateService.calculateLimitStats(eq(USER_ID), eq(LIMIT_ID), any(LocalDate.class)))
                    .thenThrow(new IllegalStateException("calculation failed"));

            assertThrows(IllegalStateException.class, () -> limitManagementService.getLimitStats(USER_ID));
        }
    }

    @Nested
    class DeleteLimit {

        @Nested
        class Success {

            @BeforeEach
            void setUp() {
                when(limitManagerService.getLimitByUserIdOrThrow(USER_ID, LIMIT_ID)).thenReturn(limit);
            }

            @Test
            void shouldDeleteLimitWhenLimitExists() {
                limitManagementService.deleteLimit(USER_ID, LIMIT_ID, AUTH_CODE);

                verify(limitRepository).delete(limit);
            }

            @Test
            void shouldDeleteLimitWhenAuthorizationCodeIsNull() {
                limitManagementService.deleteLimit(USER_ID, LIMIT_ID, null);

                verify(limitRepository).delete(limit);
            }

            @Test
            void shouldSaveOutboxEventWhenLimitIsDeleted() {
                limitManagementService.deleteLimit(USER_ID, LIMIT_ID, AUTH_CODE);

                verify(outboxService).save(eq(AGGREGATE), eq(LIMIT_ID.toString()), eq(OUTBOX_TOPIC), any(LimitActivityEvent.class));
            }

            @Test
            void shouldSaveOutboxEventBeforeDeletingLimitWhenLimitIsDeleted() {
                limitManagementService.deleteLimit(USER_ID, LIMIT_ID, AUTH_CODE);

                InOrder inOrder = inOrder(outboxService, limitRepository);
                inOrder.verify(outboxService).save(eq(AGGREGATE), eq(LIMIT_ID.toString()), eq(OUTBOX_TOPIC), any(LimitActivityEvent.class));
                inOrder.verify(limitRepository).delete(limit);
            }

            @Test
            void shouldConfirmAuthorizationCodeBeforeLoadingLimitWhenLimitIsDeleted() {
                limitManagementService.deleteLimit(USER_ID, LIMIT_ID, AUTH_CODE);

                InOrder inOrder = inOrder(authBackendClient, limitManagerService);
                inOrder.verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
                inOrder.verify(limitManagerService).getLimitByUserIdOrThrow(USER_ID, LIMIT_ID);
                verify(additionalAuthorizationCodeResolver).resolve(AUTH_CODE);
            }

            @Test
            void shouldNotValidateExpensesWhenLimitIsDeleted() {
                limitManagementService.deleteLimit(USER_ID, LIMIT_ID, AUTH_CODE);

                verifyNoInteractions(limitExpensesValidator, limitCalculateService);
            }
        }

        @Nested
        class Failure {

            @Test
            void shouldThrowExceptionWhenAuthorizationCodeConfirmationFails() {
                doThrow(new InvalidInputException("Invalid authorization code"))
                        .when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

                assertThrows(InvalidInputException.class, () -> limitManagementService.deleteLimit(USER_ID, LIMIT_ID, AUTH_CODE));

                verifyNoInteractions(limitManagerService, limitRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenLimitDoesNotExist() {
                when(limitManagerService.getLimitByUserIdOrThrow(USER_ID, LIMIT_ID))
                        .thenThrow(new RequestedEntityNotFoundException("Limit not found"));

                assertThrows(RequestedEntityNotFoundException.class, () -> limitManagementService.deleteLimit(USER_ID, LIMIT_ID, AUTH_CODE));

                verifyNoInteractions(limitRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenLimitIdIsNull() {
                when(limitManagerService.getLimitByUserIdOrThrow(USER_ID, null)).thenReturn(limit);

                assertThrows(NullPointerException.class, () -> limitManagementService.deleteLimit(USER_ID, null, AUTH_CODE));

                verify(limitRepository, never()).delete(any(Limit.class));
                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldThrowExceptionWhenOutboxServiceFails() {
                when(limitManagerService.getLimitByUserIdOrThrow(USER_ID, LIMIT_ID)).thenReturn(limit);
                doThrow(new IllegalStateException("outbox failed")).when(outboxService)
                        .save(eq(AGGREGATE), eq(LIMIT_ID.toString()), eq(OUTBOX_TOPIC), any(LimitActivityEvent.class));

                assertThrows(IllegalStateException.class, () -> limitManagementService.deleteLimit(USER_ID, LIMIT_ID, AUTH_CODE));

                verify(limitRepository, never()).delete(any(Limit.class));
            }

            @Test
            void shouldThrowExceptionWhenRepositoryDeleteFails() {
                when(limitManagerService.getLimitByUserIdOrThrow(USER_ID, LIMIT_ID)).thenReturn(limit);
                doThrow(new IllegalStateException("db failed")).when(limitRepository).delete(limit);

                assertThrows(IllegalStateException.class, () -> limitManagementService.deleteLimit(USER_ID, LIMIT_ID, AUTH_CODE));
            }
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteAllUserLimitsWhenUserDataIsDeleted() {
            limitManagementService.deleteByUserId(USER_ID);

            verify(limitRepository).deleteByUserId(USER_ID);
        }

        @Test
        void shouldNotUseOutboxOrAuthorizationWhenUserDataIsDeleted() {
            limitManagementService.deleteByUserId(USER_ID);

            verifyNoInteractions(outboxService, authBackendClient, additionalAuthorizationCodeResolver);
        }

        @Test
        void shouldDelegateToRepositoryWhenUserIdIsNull() {
            limitManagementService.deleteByUserId(null);

            verify(limitRepository).deleteByUserId(null);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            doThrow(new IllegalStateException("db failed")).when(limitRepository).deleteByUserId(USER_ID);

            assertThrows(IllegalStateException.class, () -> limitManagementService.deleteByUserId(USER_ID));
        }
    }
}