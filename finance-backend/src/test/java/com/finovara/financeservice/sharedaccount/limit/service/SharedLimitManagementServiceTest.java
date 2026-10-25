package com.finovara.financeservice.sharedaccount.limit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.financeservice.sharedaccount.limit.dto.SharedLimitDto;
import com.finovara.financeservice.sharedaccount.limit.dto.SharedLimitStatsDto;
import com.finovara.financeservice.sharedaccount.limit.model.SharedLimit;
import com.finovara.financeservice.sharedaccount.limit.repository.SharedLimitRepository;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import com.finovara.financeservice.util.limit.validator.LimitExpensesValidator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
class SharedLimitManagementServiceTest {

    private static final Long USER_ID = 10L;
    private static final Long OWNER_ID = 10L;
    private static final Long MEMBER_ID = 20L;
    private static final Long LIMIT_ID = 100L;
    private static final Long SECOND_LIMIT_ID = 200L;
    private static final String AGGREGATE = "SharedAccountLimit";
    private static final String TOPIC = "shared-account.activity";
    private static final BigDecimal AMOUNT = new BigDecimal("500.00");
    private static final ExpenseCategory CATEGORY = ExpenseCategory.values()[0];

    @Mock
    private SharedLimitRepository sharedLimitRepository;

    @Mock
    private SharedLimitCalculateService sharedLimitCalculateService;

    @Mock
    private SharedAccountParticipantsService sharedAccountParticipantsService;

    @Mock
    private SharedWalletService sharedWalletService;

    @Mock
    private LimitExpensesValidator limitExpensesValidator;

    @Mock
    private OutboxService outboxService;

    @Mock
    private SharedLimitDto limitDto;

    @Mock
    private SharedAccountParticipantsResponse participantsResponse;

    @Mock
    private SharedLimit existingLimit;

    @Mock
    private SharedLimit savedLimit;

    @Mock
    private SharedLimitStatsDto firstStats;

    @Mock
    private SharedLimitStatsDto secondStats;

    @Captor
    private ArgumentCaptor<SharedLimit> limitCaptor;

    @Captor
    private ArgumentCaptor<SharedAccountActivityLogEvent> eventCaptor;

    @Captor
    private ArgumentCaptor<LocalDate> dateCaptor;

    @InjectMocks
    private SharedLimitManagementService service;

    private void stubParticipants() {
        when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);
        when(participantsResponse.ownerId()).thenReturn(OWNER_ID);
        when(participantsResponse.memberId()).thenReturn(MEMBER_ID);
    }

    private void verifyOutboxEvent(SharedAccountActivityLogType type) {
        verify(outboxService).save(eq(AGGREGATE), eq(LIMIT_ID.toString()), eq(TOPIC), eventCaptor.capture());
        SharedAccountActivityLogEvent event = eventCaptor.getValue();
        assertEquals(OWNER_ID, event.ownerId());
        assertEquals(MEMBER_ID, event.memberId());
        assertEquals(USER_ID, event.userId());
        assertEquals(LIMIT_ID, event.targetId());
        assertEquals(type, event.type());
        assertNotNull(event.createdAt());
    }

    @Nested
    class CreateSharedLimit {

        private void stubSavedLimit() {
            when(sharedLimitRepository.save(any(SharedLimit.class))).thenReturn(savedLimit);
            when(savedLimit.getId()).thenReturn(LIMIT_ID);
        }

        @Test
        void shouldReturnSavedLimitIdWhenGeneralLimitIsCreated() {
            stubParticipants();
            when(limitDto.amount()).thenReturn(AMOUNT);
            stubSavedLimit();

            Long result = service.createSharedLimit(limitDto, USER_ID);

            assertEquals(LIMIT_ID, result);
        }

        @Test
        void shouldReturnSavedLimitIdWhenCategoryLimitIsCreated() {
            stubParticipants();
            when(limitDto.category()).thenReturn(CATEGORY);
            when(limitDto.amount()).thenReturn(AMOUNT);
            stubSavedLimit();

            Long result = service.createSharedLimit(limitDto, USER_ID);

            assertEquals(LIMIT_ID, result);
        }

        @Test
        void shouldLockWalletWhenLimitIsCreated() {
            stubParticipants();
            when(limitDto.amount()).thenReturn(AMOUNT);
            stubSavedLimit();

            service.createSharedLimit(limitDto, USER_ID);

            verify(sharedWalletService).lockWalletForUpdate(USER_ID);
        }

        @Test
        void shouldSaveLimitWithDtoAndParticipantDataWhenGeneralLimitIsCreated() {
            stubParticipants();
            when(limitDto.amount()).thenReturn(AMOUNT);
            stubSavedLimit();

            service.createSharedLimit(limitDto, USER_ID);

            verify(sharedLimitRepository).save(limitCaptor.capture());
            SharedLimit saved = limitCaptor.getValue();
            assertEquals(limitDto.periodType(), saved.getPeriodType());
            assertNull(saved.getCategory());
            assertEquals(AMOUNT, saved.getAmount());
            assertEquals(OWNER_ID, saved.getOwnerId());
            assertEquals(MEMBER_ID, saved.getMemberId());
        }

        @Test
        void shouldSaveLimitWithCategoryWhenCategoryLimitIsCreated() {
            stubParticipants();
            when(limitDto.category()).thenReturn(CATEGORY);
            when(limitDto.amount()).thenReturn(AMOUNT);
            stubSavedLimit();

            service.createSharedLimit(limitDto, USER_ID);

            verify(sharedLimitRepository).save(limitCaptor.capture());
            assertEquals(CATEGORY, limitCaptor.getValue().getCategory());
        }

        @Test
        void shouldCheckOnlyGeneralLimitWhenCategoryIsNull() {
            stubParticipants();
            when(limitDto.amount()).thenReturn(AMOUNT);
            stubSavedLimit();

            service.createSharedLimit(limitDto, USER_ID);

            verify(sharedLimitRepository).findGeneralLimit(eq(USER_ID), any());
            verify(sharedLimitRepository, never()).findCategoryLimit(any(), any(), any());
        }

        @Test
        void shouldCheckOnlyCategoryLimitWhenCategoryIsGiven() {
            stubParticipants();
            when(limitDto.category()).thenReturn(CATEGORY);
            when(limitDto.amount()).thenReturn(AMOUNT);
            stubSavedLimit();

            service.createSharedLimit(limitDto, USER_ID);

            verify(sharedLimitRepository).findCategoryLimit(eq(USER_ID), any(), eq(CATEGORY));
            verify(sharedLimitRepository, never()).findGeneralLimit(any(), any());
        }

        @Test
        void shouldValidateCurrentExpensesWhenLimitIsCreated() {
            stubParticipants();
            when(limitDto.amount()).thenReturn(AMOUNT);
            stubSavedLimit();

            service.createSharedLimit(limitDto, USER_ID);

            verify(limitExpensesValidator).validateCurrentSharedExpensesDoNotExceedLimit(USER_ID, limitDto);
        }

        @Test
        void shouldSaveOutboxEventWhenLimitIsCreated() {
            stubParticipants();
            when(limitDto.amount()).thenReturn(AMOUNT);
            stubSavedLimit();

            service.createSharedLimit(limitDto, USER_ID);

            verifyOutboxEvent(SharedAccountActivityLogType.LIMIT_CREATED);
        }

        @Test
        void shouldLockValidateSaveAndLogInOrderWhenLimitIsCreated() {
            stubParticipants();
            when(limitDto.amount()).thenReturn(AMOUNT);
            stubSavedLimit();

            service.createSharedLimit(limitDto, USER_ID);

            InOrder inOrder = inOrder(sharedWalletService, limitExpensesValidator, sharedLimitRepository, outboxService);
            inOrder.verify(sharedWalletService).lockWalletForUpdate(USER_ID);
            inOrder.verify(limitExpensesValidator).validateCurrentSharedExpensesDoNotExceedLimit(USER_ID, limitDto);
            inOrder.verify(sharedLimitRepository).save(any(SharedLimit.class));
            inOrder.verify(outboxService).save(anyString(), anyString(), anyString(), any());
        }

        @Test
        void shouldThrowExceptionWhenWalletLockFails() {
            doThrow(new RequestedEntityNotFoundException("wallet not found")).when(sharedWalletService).lockWalletForUpdate(USER_ID);

            assertThrows(RequestedEntityNotFoundException.class, () -> service.createSharedLimit(limitDto, USER_ID));

            verifyNoInteractions(limitExpensesValidator, sharedAccountParticipantsService, sharedLimitRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenValidatorFails() {
            doThrow(new IllegalStateException("limit exceeded")).when(limitExpensesValidator)
                    .validateCurrentSharedExpensesDoNotExceedLimit(USER_ID, limitDto);

            assertThrows(IllegalStateException.class, () -> service.createSharedLimit(limitDto, USER_ID));

            verifyNoInteractions(sharedAccountParticipantsService, sharedLimitRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenGeneralLimitAlreadyExists() {
            when(sharedLimitRepository.findGeneralLimit(eq(USER_ID), any())).thenReturn(Optional.of(savedLimit));
            when(savedLimit.getId()).thenReturn(LIMIT_ID);

            assertThrows(EntityAlreadyExistsException.class, () -> service.createSharedLimit(limitDto, USER_ID));

            verify(sharedLimitRepository, never()).save(any(SharedLimit.class));
            verifyNoInteractions(sharedAccountParticipantsService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenCategoryLimitAlreadyExists() {
            when(limitDto.category()).thenReturn(CATEGORY);
            when(sharedLimitRepository.findCategoryLimit(eq(USER_ID), any(), eq(CATEGORY))).thenReturn(Optional.of(savedLimit));
            when(savedLimit.getId()).thenReturn(LIMIT_ID);

            assertThrows(EntityAlreadyExistsException.class, () -> service.createSharedLimit(limitDto, USER_ID));

            verify(sharedLimitRepository, never()).save(any(SharedLimit.class));
            verifyNoInteractions(sharedAccountParticipantsService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenParticipantsServiceFails() {
            when(sharedAccountParticipantsService.getParticipants(USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("participants not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.createSharedLimit(limitDto, USER_ID));

            verify(sharedLimitRepository, never()).save(any(SharedLimit.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenLimitDtoIsNull() {
            assertThrows(NullPointerException.class, () -> service.createSharedLimit(null, USER_ID));

            verify(sharedLimitRepository, never()).save(any(SharedLimit.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenParticipantsResponseIsNull() {
            assertThrows(NullPointerException.class, () -> service.createSharedLimit(limitDto, USER_ID));

            verify(sharedLimitRepository, never()).save(any(SharedLimit.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenRepositorySaveFails() {
            stubParticipants();
            when(limitDto.amount()).thenReturn(AMOUNT);
            when(sharedLimitRepository.save(any(SharedLimit.class))).thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> service.createSharedLimit(limitDto, USER_ID));

            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenOutboxServiceFails() {
            stubParticipants();
            when(limitDto.amount()).thenReturn(AMOUNT);
            stubSavedLimit();
            doThrow(new IllegalStateException("outbox failed")).when(outboxService)
                    .save(anyString(), anyString(), anyString(), any());

            assertThrows(IllegalStateException.class, () -> service.createSharedLimit(limitDto, USER_ID));
        }
    }

    @Nested
    class EditSharedLimit {

        private void stubSuccessfulEdit() {
            when(sharedLimitRepository.findByIdAndOwnerIdOrMemberId(USER_ID, LIMIT_ID)).thenReturn(Optional.of(existingLimit));
            stubParticipants();
        }

        @Test
        void shouldReturnLimitIdWhenLimitIsEdited() {
            stubSuccessfulEdit();

            Long result = service.editSharedLimit(limitDto, LIMIT_ID, USER_ID);

            assertEquals(LIMIT_ID, result);
        }

        @Test
        void shouldLockWalletWhenLimitIsEdited() {
            stubSuccessfulEdit();

            service.editSharedLimit(limitDto, LIMIT_ID, USER_ID);

            verify(sharedWalletService).lockWalletForUpdate(USER_ID);
        }

        @Test
        void shouldUpdateLimitFieldsWhenLimitIsEdited() {
            stubSuccessfulEdit();
            when(limitDto.category()).thenReturn(CATEGORY);
            when(limitDto.amount()).thenReturn(AMOUNT);

            service.editSharedLimit(limitDto, LIMIT_ID, USER_ID);

            verify(existingLimit).setPeriodType(limitDto.periodType());
            verify(existingLimit).setCategory(CATEGORY);
            verify(existingLimit).setAmount(AMOUNT);
        }

        @Test
        void shouldValidateCurrentExpensesWhenLimitIsEdited() {
            stubSuccessfulEdit();

            service.editSharedLimit(limitDto, LIMIT_ID, USER_ID);

            verify(limitExpensesValidator).validateCurrentSharedExpensesDoNotExceedLimit(USER_ID, limitDto);
        }

        @Test
        void shouldCheckOnlyGeneralLimitWhenEditedCategoryIsNull() {
            stubSuccessfulEdit();

            service.editSharedLimit(limitDto, LIMIT_ID, USER_ID);

            verify(sharedLimitRepository).findGeneralLimit(eq(USER_ID), any());
            verify(sharedLimitRepository, never()).findCategoryLimit(any(), any(), any());
        }

        @Test
        void shouldCheckOnlyCategoryLimitWhenEditedCategoryIsGiven() {
            stubSuccessfulEdit();
            when(limitDto.category()).thenReturn(CATEGORY);

            service.editSharedLimit(limitDto, LIMIT_ID, USER_ID);

            verify(sharedLimitRepository).findCategoryLimit(eq(USER_ID), any(), eq(CATEGORY));
            verify(sharedLimitRepository, never()).findGeneralLimit(any(), any());
        }

        @Test
        void shouldAllowEditWhenFoundGeneralLimitIsTheEditedOne() {
            stubSuccessfulEdit();
            when(sharedLimitRepository.findGeneralLimit(eq(USER_ID), any())).thenReturn(Optional.of(existingLimit));
            when(existingLimit.getId()).thenReturn(LIMIT_ID);

            Long result = service.editSharedLimit(limitDto, LIMIT_ID, USER_ID);

            assertEquals(LIMIT_ID, result);
        }

        @Test
        void shouldSaveOutboxEventWhenLimitIsEdited() {
            stubSuccessfulEdit();

            service.editSharedLimit(limitDto, LIMIT_ID, USER_ID);

            verifyOutboxEvent(SharedAccountActivityLogType.LIMIT_EDITED);
        }

        @Test
        void shouldLockValidateAndLogInOrderWhenLimitIsEdited() {
            stubSuccessfulEdit();

            service.editSharedLimit(limitDto, LIMIT_ID, USER_ID);

            InOrder inOrder = inOrder(sharedWalletService, limitExpensesValidator, outboxService);
            inOrder.verify(sharedWalletService).lockWalletForUpdate(USER_ID);
            inOrder.verify(limitExpensesValidator).validateCurrentSharedExpensesDoNotExceedLimit(USER_ID, limitDto);
            inOrder.verify(outboxService).save(anyString(), anyString(), anyString(), any());
        }

        @Test
        void shouldThrowExceptionWhenWalletLockFails() {
            doThrow(new RequestedEntityNotFoundException("wallet not found")).when(sharedWalletService).lockWalletForUpdate(USER_ID);

            assertThrows(RequestedEntityNotFoundException.class, () -> service.editSharedLimit(limitDto, LIMIT_ID, USER_ID));

            verifyNoInteractions(sharedLimitRepository, limitExpensesValidator, sharedAccountParticipantsService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenLimitDoesNotExist() {
            when(sharedLimitRepository.findByIdAndOwnerIdOrMemberId(USER_ID, LIMIT_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> service.editSharedLimit(limitDto, LIMIT_ID, USER_ID));

            verifyNoInteractions(limitExpensesValidator, sharedAccountParticipantsService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenValidatorFails() {
            when(sharedLimitRepository.findByIdAndOwnerIdOrMemberId(USER_ID, LIMIT_ID)).thenReturn(Optional.of(existingLimit));
            doThrow(new IllegalStateException("limit exceeded")).when(limitExpensesValidator)
                    .validateCurrentSharedExpensesDoNotExceedLimit(USER_ID, limitDto);

            assertThrows(IllegalStateException.class, () -> service.editSharedLimit(limitDto, LIMIT_ID, USER_ID));

            verify(existingLimit, never()).setAmount(any());
            verifyNoInteractions(sharedAccountParticipantsService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenAnotherGeneralLimitAlreadyExists() {
            when(sharedLimitRepository.findByIdAndOwnerIdOrMemberId(USER_ID, LIMIT_ID)).thenReturn(Optional.of(existingLimit));
            when(sharedLimitRepository.findGeneralLimit(eq(USER_ID), any())).thenReturn(Optional.of(savedLimit));
            when(savedLimit.getId()).thenReturn(SECOND_LIMIT_ID);

            assertThrows(EntityAlreadyExistsException.class, () -> service.editSharedLimit(limitDto, LIMIT_ID, USER_ID));

            verify(existingLimit, never()).setAmount(any());
            verifyNoInteractions(sharedAccountParticipantsService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenAnotherCategoryLimitAlreadyExists() {
            when(sharedLimitRepository.findByIdAndOwnerIdOrMemberId(USER_ID, LIMIT_ID)).thenReturn(Optional.of(existingLimit));
            when(limitDto.category()).thenReturn(CATEGORY);
            when(sharedLimitRepository.findCategoryLimit(eq(USER_ID), any(), eq(CATEGORY))).thenReturn(Optional.of(savedLimit));
            when(savedLimit.getId()).thenReturn(SECOND_LIMIT_ID);

            assertThrows(EntityAlreadyExistsException.class, () -> service.editSharedLimit(limitDto, LIMIT_ID, USER_ID));

            verify(existingLimit, never()).setAmount(any());
            verifyNoInteractions(sharedAccountParticipantsService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenLimitDtoIsNull() {
            when(sharedLimitRepository.findByIdAndOwnerIdOrMemberId(USER_ID, LIMIT_ID)).thenReturn(Optional.of(existingLimit));

            assertThrows(NullPointerException.class, () -> service.editSharedLimit(null, LIMIT_ID, USER_ID));

            verify(existingLimit, never()).setAmount(any());
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryLookupFails() {
            when(sharedLimitRepository.findByIdAndOwnerIdOrMemberId(USER_ID, LIMIT_ID)).thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> service.editSharedLimit(limitDto, LIMIT_ID, USER_ID));

            verifyNoInteractions(limitExpensesValidator, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenParticipantsServiceFails() {
            when(sharedLimitRepository.findByIdAndOwnerIdOrMemberId(USER_ID, LIMIT_ID)).thenReturn(Optional.of(existingLimit));
            when(sharedAccountParticipantsService.getParticipants(USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("participants not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.editSharedLimit(limitDto, LIMIT_ID, USER_ID));

            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenOutboxServiceFails() {
            stubSuccessfulEdit();
            doThrow(new IllegalStateException("outbox failed")).when(outboxService)
                    .save(anyString(), anyString(), anyString(), any());

            assertThrows(IllegalStateException.class, () -> service.editSharedLimit(limitDto, LIMIT_ID, USER_ID));
        }
    }

    @Nested
    class GetSharedLimitStats {

        @Test
        void shouldReturnEmptyListWhenNoLimitsExist() {
            when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

            List<SharedLimitStatsDto> result = service.getSharedLimitStats(USER_ID);

            assertTrue(result.isEmpty());
            verifyNoInteractions(sharedLimitCalculateService, sharedAccountParticipantsService);
        }

        @Test
        void shouldReturnCalculatedStatsWhenLimitsExist() {
            List<SharedLimit> limits = List.of(existingLimit, savedLimit);
            when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(limits);
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);
            when(sharedLimitCalculateService.calculateLimitStats(eq(limits), eq(participantsResponse), any(LocalDate.class)))
                    .thenReturn(List.of(firstStats, secondStats));

            List<SharedLimitStatsDto> result = service.getSharedLimitStats(USER_ID);

            assertEquals(List.of(firstStats, secondStats), result);
        }

        @Test
        void shouldUseTodayWhenCalculatingStats() {
            List<SharedLimit> limits = List.of(existingLimit);
            when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(limits);
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);

            service.getSharedLimitStats(USER_ID);

            verify(sharedLimitCalculateService).calculateLimitStats(eq(limits), eq(participantsResponse), dateCaptor.capture());
            assertEquals(LocalDate.now(), dateCaptor.getValue());
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(sharedLimitRepository.findAllByUserId(USER_ID)).thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> service.getSharedLimitStats(USER_ID));

            verifyNoInteractions(sharedAccountParticipantsService, sharedLimitCalculateService);
        }

        @Test
        void shouldThrowExceptionWhenParticipantsServiceFails() {
            when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(existingLimit));
            when(sharedAccountParticipantsService.getParticipants(USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("participants not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.getSharedLimitStats(USER_ID));

            verifyNoInteractions(sharedLimitCalculateService);
        }

        @Test
        void shouldThrowExceptionWhenCalculateServiceFails() {
            List<SharedLimit> limits = List.of(existingLimit);
            when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(limits);
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);
            when(sharedLimitCalculateService.calculateLimitStats(eq(limits), eq(participantsResponse), any(LocalDate.class)))
                    .thenThrow(new RequestedEntityNotFoundException("Active Limit not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.getSharedLimitStats(USER_ID));
        }
    }

    @Nested
    class DeleteSharedLimit {

        private void stubSuccessfulDelete() {
            when(sharedLimitRepository.findByIdAndOwnerIdOrMemberId(USER_ID, LIMIT_ID)).thenReturn(Optional.of(existingLimit));
            stubParticipants();
        }

        @Test
        void shouldDeleteLimitWhenLimitExists() {
            stubSuccessfulDelete();

            service.deleteSharedLimit(USER_ID, LIMIT_ID);

            verify(sharedLimitRepository).delete(existingLimit);
        }

        @Test
        void shouldSaveOutboxEventWhenLimitIsDeleted() {
            stubSuccessfulDelete();

            service.deleteSharedLimit(USER_ID, LIMIT_ID);

            verifyOutboxEvent(SharedAccountActivityLogType.LIMIT_DELETED);
        }

        @Test
        void shouldDeleteLimitBeforeSavingOutboxEventWhenLimitIsDeleted() {
            stubSuccessfulDelete();

            service.deleteSharedLimit(USER_ID, LIMIT_ID);

            InOrder inOrder = inOrder(sharedLimitRepository, outboxService);
            inOrder.verify(sharedLimitRepository).delete(existingLimit);
            inOrder.verify(outboxService).save(anyString(), anyString(), anyString(), any());
        }

        @Test
        void shouldThrowExceptionWhenLimitDoesNotExist() {
            when(sharedLimitRepository.findByIdAndOwnerIdOrMemberId(USER_ID, LIMIT_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.deleteSharedLimit(USER_ID, LIMIT_ID));

            verify(sharedLimitRepository, never()).delete(any(SharedLimit.class));
            verifyNoInteractions(sharedAccountParticipantsService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryLookupFails() {
            when(sharedLimitRepository.findByIdAndOwnerIdOrMemberId(USER_ID, LIMIT_ID)).thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> service.deleteSharedLimit(USER_ID, LIMIT_ID));

            verifyNoInteractions(sharedAccountParticipantsService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenParticipantsServiceFails() {
            when(sharedLimitRepository.findByIdAndOwnerIdOrMemberId(USER_ID, LIMIT_ID)).thenReturn(Optional.of(existingLimit));
            when(sharedAccountParticipantsService.getParticipants(USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("participants not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.deleteSharedLimit(USER_ID, LIMIT_ID));

            verify(sharedLimitRepository).delete(existingLimit);
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenOutboxServiceFails() {
            stubSuccessfulDelete();
            doThrow(new IllegalStateException("outbox failed")).when(outboxService)
                    .save(anyString(), anyString(), anyString(), any());

            assertThrows(IllegalStateException.class, () -> service.deleteSharedLimit(USER_ID, LIMIT_ID));
        }
    }
}