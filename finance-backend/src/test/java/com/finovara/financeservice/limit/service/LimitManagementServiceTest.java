package com.finovara.financeservice.limit.service;

import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.mainaccount.activity.event.limit.LimitActivityEvent;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.limit.dto.LimitDto;
import com.finovara.financeservice.limit.dto.LimitStatsDto;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.limit.repository.LimitRepository;
import com.finovara.financeservice.util.limit.manager.LimitManagerService;
import com.finovara.financeservice.util.limit.validator.LimitExpensesValidator;
import com.finovara.financeservice.wallet.service.WalletService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LimitManagementServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long LIMIT_ID = 10L;
    private static final Long OTHER_LIMIT_ID = 20L;
    private static final String AUTH_CODE = "123456";
    private static final String OUTBOX_AGGREGATE = "Limit";
    private static final String OUTBOX_TOPIC = "limit.changed";
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(500);
    private static final BigDecimal OLD_AMOUNT = BigDecimal.valueOf(300);
    private static final PeriodType PERIOD = PeriodType.values()[0];
    private static final PeriodType OTHER_PERIOD = PeriodType.values()[1];
    private static final ExpenseCategory CATEGORY = ExpenseCategory.values()[0];
    private static final ExpenseCategory OTHER_CATEGORY = ExpenseCategory.values()[1];

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
    private WalletService walletService;

    @Mock
    private LimitDto limitDto;

    @Mock
    private LimitStatsDto statsDto;

    @Captor
    private ArgumentCaptor<Limit> limitCaptor;

    @Captor
    private ArgumentCaptor<List<Limit>> limitsCaptor;

    @InjectMocks
    private LimitManagementService service;

    private Limit existingLimit;
    private Limit savedLimit;

    @BeforeEach
    void setUp() {
        existingLimit = Limit.builder().id(LIMIT_ID).periodType(OTHER_PERIOD).category(OTHER_CATEGORY).amount(OLD_AMOUNT).userId(USER_ID).build();
        savedLimit = Limit.builder().id(LIMIT_ID).periodType(PERIOD).category(CATEGORY).amount(AMOUNT).userId(USER_ID).build();
    }

    @Nested
    class CreateLimit {

        @Test
        void shouldSaveCategoryLimitAndReturnIdWhenNoDuplicateExists() {
            when(limitDto.periodType()).thenReturn(PERIOD);
            when(limitDto.category()).thenReturn(CATEGORY);
            when(limitDto.amount()).thenReturn(AMOUNT);
            when(limitRepository.findCategoryLimit(USER_ID, PERIOD, CATEGORY)).thenReturn(Optional.empty());
            when(limitRepository.save(any(Limit.class))).thenReturn(savedLimit);

            Long result = service.createLimit(limitDto, USER_ID);

            assertEquals(LIMIT_ID, result);
            verify(limitRepository).save(limitCaptor.capture());
            assertEquals(PERIOD, limitCaptor.getValue().getPeriodType());
            assertEquals(CATEGORY, limitCaptor.getValue().getCategory());
            assertEquals(AMOUNT, limitCaptor.getValue().getAmount());
            assertEquals(USER_ID, limitCaptor.getValue().getUserId());
            verify(limitRepository, never()).findGeneralLimit(any(), any());
        }

        @Test
        void shouldSaveGeneralLimitWhenCategoryIsNull() {
            Limit generalSaved = Limit.builder().id(LIMIT_ID).periodType(PERIOD).category(null).amount(AMOUNT).userId(USER_ID).build();
            when(limitDto.periodType()).thenReturn(PERIOD);
            when(limitDto.amount()).thenReturn(AMOUNT);
            when(limitRepository.findGeneralLimit(USER_ID, PERIOD)).thenReturn(Optional.empty());
            when(limitRepository.save(any(Limit.class))).thenReturn(generalSaved);

            Long result = service.createLimit(limitDto, USER_ID);

            assertEquals(LIMIT_ID, result);
            verify(limitRepository).save(limitCaptor.capture());
            assertEquals(null, limitCaptor.getValue().getCategory());
            verify(limitRepository, never()).findCategoryLimit(any(), any(), any());
        }

        @Test
        void shouldConfirmAuthorizationCodeAndSaveOutboxEventWhenLimitCreated() {
            when(limitDto.periodType()).thenReturn(PERIOD);
            when(limitDto.category()).thenReturn(CATEGORY);
            when(limitDto.amount()).thenReturn(AMOUNT);
            when(limitRepository.findCategoryLimit(USER_ID, PERIOD, CATEGORY)).thenReturn(Optional.empty());
            when(limitRepository.save(any(Limit.class))).thenReturn(savedLimit);

            service.createLimit(limitDto, USER_ID);

            verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
            verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(LIMIT_ID.toString()), eq(OUTBOX_TOPIC), any(LimitActivityEvent.class));
        }

        @Test
        void shouldLockWalletAndValidateExpensesBeforeSavingLimit() {
            when(limitDto.periodType()).thenReturn(PERIOD);
            when(limitDto.category()).thenReturn(CATEGORY);
            when(limitDto.amount()).thenReturn(AMOUNT);
            when(limitRepository.findCategoryLimit(USER_ID, PERIOD, CATEGORY)).thenReturn(Optional.empty());
            when(limitRepository.save(any(Limit.class))).thenReturn(savedLimit);

            service.createLimit(limitDto, USER_ID);

            InOrder inOrder = inOrder(authBackendClient, walletService, limitExpensesValidator, limitRepository);
            inOrder.verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
            inOrder.verify(walletService).lockWalletForUpdate(USER_ID);
            inOrder.verify(limitExpensesValidator).validateCurrentExpensesDoNotExceedLimit(USER_ID, limitDto);
            inOrder.verify(limitRepository).save(any(Limit.class));
        }

        @Test
        void shouldThrowExceptionWhenCategoryLimitAlreadyExists() {
            when(limitDto.periodType()).thenReturn(PERIOD);
            when(limitDto.category()).thenReturn(CATEGORY);
            when(limitRepository.findCategoryLimit(USER_ID, PERIOD, CATEGORY)).thenReturn(Optional.of(existingLimit));

            assertThrows(EntityAlreadyExistsException.class, () -> service.createLimit(limitDto, USER_ID));
            verify(limitRepository, never()).save(any(Limit.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenGeneralLimitAlreadyExists() {
            when(limitDto.periodType()).thenReturn(PERIOD);
            when(limitRepository.findGeneralLimit(USER_ID, PERIOD)).thenReturn(Optional.of(existingLimit));

            assertThrows(EntityAlreadyExistsException.class, () -> service.createLimit(limitDto, USER_ID));
            verify(limitRepository, never()).save(any(Limit.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenCurrentExpensesExceedLimit() {
            doThrow(new IllegalStateException()).when(limitExpensesValidator).validateCurrentExpensesDoNotExceedLimit(USER_ID, limitDto);

            assertThrows(IllegalStateException.class, () -> service.createLimit(limitDto, USER_ID));
            verify(limitRepository, never()).save(any(Limit.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeIsInvalid() {
            doThrow(new IllegalStateException()).when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class, () -> service.createLimit(limitDto, USER_ID));
            verifyNoInteractions(walletService, limitExpensesValidator, limitRepository, outboxService);
        }
    }

    @Nested
    class EditLimit {

        @Test
        void shouldUpdateLimitFieldsAndReturnIdWhenLimitExists() {
            when(limitDto.periodType()).thenReturn(PERIOD);
            when(limitDto.category()).thenReturn(CATEGORY);
            when(limitDto.amount()).thenReturn(AMOUNT);
            when(limitManagerService.getLimitByUserIdOrThrow(USER_ID, LIMIT_ID)).thenReturn(existingLimit);
            when(limitRepository.findCategoryLimit(USER_ID, PERIOD, CATEGORY)).thenReturn(Optional.empty());

            Long result = service.editLimit(limitDto, LIMIT_ID, USER_ID);

            assertEquals(LIMIT_ID, result);
            assertEquals(PERIOD, existingLimit.getPeriodType());
            assertEquals(CATEGORY, existingLimit.getCategory());
            assertEquals(AMOUNT, existingLimit.getAmount());
        }

        @Test
        void shouldCheckGeneralLimitWhenEditedCategoryIsNull() {
            when(limitDto.periodType()).thenReturn(PERIOD);
            when(limitDto.amount()).thenReturn(AMOUNT);
            when(limitManagerService.getLimitByUserIdOrThrow(USER_ID, LIMIT_ID)).thenReturn(existingLimit);
            when(limitRepository.findGeneralLimit(USER_ID, PERIOD)).thenReturn(Optional.empty());

            Long result = service.editLimit(limitDto, LIMIT_ID, USER_ID);

            assertEquals(LIMIT_ID, result);
            assertEquals(null, existingLimit.getCategory());
            verify(limitRepository, never()).findCategoryLimit(any(), any(), any());
        }

        @Test
        void shouldNotThrowExceptionWhenDuplicateIsTheSameLimit() {
            Limit sameLimit = Limit.builder().id(LIMIT_ID).periodType(PERIOD).category(CATEGORY).amount(OLD_AMOUNT).userId(USER_ID).build();
            when(limitDto.periodType()).thenReturn(PERIOD);
            when(limitDto.category()).thenReturn(CATEGORY);
            when(limitDto.amount()).thenReturn(AMOUNT);
            when(limitManagerService.getLimitByUserIdOrThrow(USER_ID, LIMIT_ID)).thenReturn(existingLimit);
            when(limitRepository.findCategoryLimit(USER_ID, PERIOD, CATEGORY)).thenReturn(Optional.of(sameLimit));

            Long result = service.editLimit(limitDto, LIMIT_ID, USER_ID);

            assertEquals(LIMIT_ID, result);
            assertEquals(AMOUNT, existingLimit.getAmount());
        }

        @Test
        void shouldSaveOutboxEventAndLockWalletWhenLimitEdited() {
            when(limitDto.periodType()).thenReturn(PERIOD);
            when(limitDto.category()).thenReturn(CATEGORY);
            when(limitDto.amount()).thenReturn(AMOUNT);
            when(limitManagerService.getLimitByUserIdOrThrow(USER_ID, LIMIT_ID)).thenReturn(existingLimit);
            when(limitRepository.findCategoryLimit(USER_ID, PERIOD, CATEGORY)).thenReturn(Optional.empty());

            service.editLimit(limitDto, LIMIT_ID, USER_ID);

            InOrder inOrder = inOrder(authBackendClient, walletService, limitExpensesValidator);
            inOrder.verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
            inOrder.verify(walletService).lockWalletForUpdate(USER_ID);
            inOrder.verify(limitExpensesValidator).validateCurrentExpensesDoNotExceedLimit(USER_ID, limitDto);
            verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(LIMIT_ID.toString()), eq(OUTBOX_TOPIC), any(LimitActivityEvent.class));
        }

        @Test
        void shouldThrowExceptionWhenAnotherCategoryLimitAlreadyExists() {
            Limit anotherLimit = Limit.builder().id(OTHER_LIMIT_ID).periodType(PERIOD).category(CATEGORY).amount(OLD_AMOUNT).userId(USER_ID).build();
            when(limitDto.periodType()).thenReturn(PERIOD);
            when(limitDto.category()).thenReturn(CATEGORY);
            when(limitManagerService.getLimitByUserIdOrThrow(USER_ID, LIMIT_ID)).thenReturn(existingLimit);
            when(limitRepository.findCategoryLimit(USER_ID, PERIOD, CATEGORY)).thenReturn(Optional.of(anotherLimit));

            assertThrows(EntityAlreadyExistsException.class, () -> service.editLimit(limitDto, LIMIT_ID, USER_ID));
            assertEquals(OLD_AMOUNT, existingLimit.getAmount());
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenAnotherGeneralLimitAlreadyExists() {
            Limit anotherLimit = Limit.builder().id(OTHER_LIMIT_ID).periodType(PERIOD).category(null).amount(OLD_AMOUNT).userId(USER_ID).build();
            when(limitDto.periodType()).thenReturn(PERIOD);
            when(limitManagerService.getLimitByUserIdOrThrow(USER_ID, LIMIT_ID)).thenReturn(existingLimit);
            when(limitRepository.findGeneralLimit(USER_ID, PERIOD)).thenReturn(Optional.of(anotherLimit));

            assertThrows(EntityAlreadyExistsException.class, () -> service.editLimit(limitDto, LIMIT_ID, USER_ID));
            assertEquals(OLD_AMOUNT, existingLimit.getAmount());
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenCurrentExpensesExceedEditedLimit() {
            when(limitManagerService.getLimitByUserIdOrThrow(USER_ID, LIMIT_ID)).thenReturn(existingLimit);
            doThrow(new IllegalStateException()).when(limitExpensesValidator).validateCurrentExpensesDoNotExceedLimit(USER_ID, limitDto);

            assertThrows(IllegalStateException.class, () -> service.editLimit(limitDto, LIMIT_ID, USER_ID));
            assertEquals(OLD_AMOUNT, existingLimit.getAmount());
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenLimitDoesNotExist() {
            when(limitManagerService.getLimitByUserIdOrThrow(USER_ID, LIMIT_ID)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> service.editLimit(limitDto, LIMIT_ID, USER_ID));
            verifyNoInteractions(limitExpensesValidator, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeIsInvalid() {
            doThrow(new IllegalStateException()).when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class, () -> service.editLimit(limitDto, LIMIT_ID, USER_ID));
            verifyNoInteractions(walletService, limitManagerService, limitExpensesValidator, outboxService);
        }
    }

    @Nested
    class GetLimitStats {

        @Test
        void shouldReturnStatsWhenActiveLimitsExist() {
            List<Limit> limits = List.of(existingLimit, savedLimit);
            when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(limits);
            when(limitCalculateService.calculateLimitStats(eq(limits), eq(USER_ID), any(LocalDate.class))).thenReturn(List.of(statsDto));

            List<LimitStatsDto> result = service.getLimitStats(USER_ID);

            assertEquals(1, result.size());
            assertSame(statsDto, result.get(0));
            verify(limitCalculateService).calculateLimitStats(eq(limits), eq(USER_ID), any(LocalDate.class));
        }

        @Test
        void shouldReturnEmptyListWhenNoActiveLimitsExist() {
            when(limitRepository.findAllActiveByUserId(USER_ID)).thenReturn(List.of());
            when(limitCalculateService.calculateLimitStats(eq(List.<Limit>of()), eq(USER_ID), any(LocalDate.class))).thenReturn(List.of());

            List<LimitStatsDto> result = service.getLimitStats(USER_ID);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class DeleteLimit {

        @Test
        void shouldDeleteLimitAndSaveOutboxEventWhenLimitExists() {
            when(limitManagerService.getLimitByUserIdOrThrow(USER_ID, LIMIT_ID)).thenReturn(existingLimit);

            service.deleteLimit(USER_ID, LIMIT_ID, AUTH_CODE);

            verify(additionalAuthorizationCodeResolver).resolve(AUTH_CODE);
            verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
            verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(LIMIT_ID.toString()), eq(OUTBOX_TOPIC), any(LimitActivityEvent.class));
            verify(limitRepository).delete(existingLimit);
        }

        @Test
        void shouldThrowExceptionWhenLimitDoesNotExist() {
            when(limitManagerService.getLimitByUserIdOrThrow(USER_ID, LIMIT_ID)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> service.deleteLimit(USER_ID, LIMIT_ID, AUTH_CODE));
            verifyNoInteractions(outboxService, limitRepository);
        }

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeIsInvalid() {
            doThrow(new IllegalStateException()).when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class, () -> service.deleteLimit(USER_ID, LIMIT_ID, AUTH_CODE));
            verifyNoInteractions(limitManagerService, outboxService, limitRepository);
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteAllLimitsWhenUserHasLimits() {
            List<Limit> limits = List.of(existingLimit, savedLimit);
            when(limitRepository.findAllByUserId(USER_ID)).thenReturn(limits);

            service.deleteByUserId(USER_ID);

            verify(limitRepository).deleteAll(limitsCaptor.capture());
            assertEquals(limits, limitsCaptor.getValue());
        }

        @Test
        void shouldDeleteEmptyListWhenUserHasNoLimits() {
            when(limitRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

            service.deleteByUserId(USER_ID);

            verify(limitRepository).deleteAll(limitsCaptor.capture());
            assertTrue(limitsCaptor.getValue().isEmpty());
        }
    }
}