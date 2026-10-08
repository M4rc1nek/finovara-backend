package com.finovara.financeservice.sharedaccount.revenue.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.revenue.dto.SharedRevenueDto;
import com.finovara.financeservice.sharedaccount.revenue.dto.SharedRevenueResponse;
import com.finovara.financeservice.sharedaccount.revenue.mapper.SharedRevenueMapper;
import com.finovara.financeservice.sharedaccount.revenue.model.SharedRevenue;
import com.finovara.financeservice.sharedaccount.revenue.model.SharedRevenueRepository;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import com.finovara.financeservice.util.transaction.revenue.SharedRevenueManagerService;
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
class SharedRevenueServiceTest {

    private static final Long OWNER_ID = 10L;
    private static final Long MEMBER_ID = 20L;
    private static final Long OUTSIDER_ID = 99L;
    private static final Long REVENUE_ID = 100L;
    private static final String USERNAME = "john";
    private static final String OTHER_USERNAME = "anna";
    private static final String AGGREGATE = "SharedAccountRevenue";
    private static final String TOPIC = "shared-account.activity";
    private static final String DESCRIPTION = "Salary";
    private static final BigDecimal AMOUNT = new BigDecimal("150.00");
    private static final BigDecimal OLD_AMOUNT = new BigDecimal("40.00");

    @Mock
    private SharedRevenueMapper sharedRevenueMapper;

    @Mock
    private SharedRevenueRepository sharedRevenueRepository;

    @Mock
    private SharedAccountParticipantsService sharedAccountParticipantsService;

    @Mock
    private SharedWalletService sharedWalletService;

    @Mock
    private SharedRevenueManagerService sharedRevenueManagerService;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private OutboxService outboxService;

    @Mock
    private SharedRevenueDto revenueDto;

    @Mock
    private SharedAccountParticipantsResponse participantsResponse;

    @Mock
    private SharedRevenue savedRevenue;

    @Mock
    private SharedRevenue existingRevenue;

    @Captor
    private ArgumentCaptor<SharedRevenue> revenueCaptor;

    @Captor
    private ArgumentCaptor<SharedAccountActivityLogEvent> eventCaptor;

    @InjectMocks
    private SharedRevenueService service;

    private void stubParticipants(Long userId) {
        when(sharedAccountParticipantsService.getParticipants(userId)).thenReturn(participantsResponse);
        when(participantsResponse.ownerId()).thenReturn(OWNER_ID);
        when(participantsResponse.memberId()).thenReturn(MEMBER_ID);
    }

    private void verifyOutboxEvent(Long userId, SharedAccountActivityLogType type) {
        verify(outboxService).save(eq(AGGREGATE), eq(REVENUE_ID.toString()), eq(TOPIC), eventCaptor.capture());
        SharedAccountActivityLogEvent event = eventCaptor.getValue();
        assertEquals(OWNER_ID, event.ownerId());
        assertEquals(MEMBER_ID, event.memberId());
        assertEquals(userId, event.userId());
        assertEquals(REVENUE_ID, event.targetId());
        assertEquals(type, event.type());
        assertNotNull(event.createdAt());
    }

    @Nested
    class AddSharedRevenue {

        private void stubAddInputs() {
            stubParticipants(OWNER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
            when(revenueDto.amount()).thenReturn(AMOUNT);
            when(revenueDto.description()).thenReturn(DESCRIPTION);
        }

        private void stubSavedRevenue() {
            when(sharedRevenueRepository.save(any(SharedRevenue.class))).thenReturn(savedRevenue);
            when(savedRevenue.getId()).thenReturn(REVENUE_ID);
        }

        @Test
        void shouldReturnResponseWhenRevenueIsAdded() {
            stubAddInputs();
            stubSavedRevenue();

            SharedRevenueResponse result = service.addSharedRevenue(revenueDto, OWNER_ID);

            verify(sharedRevenueRepository).save(revenueCaptor.capture());
            assertEquals(new SharedRevenueResponse(revenueCaptor.getValue().getId(), OWNER_ID, USERNAME), result);
        }

        @Test
        void shouldSaveRevenueWithDtoAndParticipantDataWhenRevenueIsAdded() {
            stubAddInputs();
            stubSavedRevenue();

            service.addSharedRevenue(revenueDto, OWNER_ID);

            verify(sharedRevenueRepository).save(revenueCaptor.capture());
            SharedRevenue saved = revenueCaptor.getValue();
            assertEquals(AMOUNT, saved.getAmount());
            assertEquals(revenueDto.category(), saved.getCategory());
            assertEquals(DESCRIPTION, saved.getDescription());
            assertEquals(OWNER_ID, saved.getOwnerId());
            assertEquals(MEMBER_ID, saved.getMemberId());
            assertEquals(OWNER_ID, saved.getCreatedByUserId());
            assertEquals(LocalDate.now(), saved.getCreatedAt());
        }

        @Test
        void shouldSaveRevenueWithMemberAsCreatorWhenMemberAddsRevenue() {
            stubParticipants(MEMBER_ID);
            when(authBackendClient.getUsername(MEMBER_ID)).thenReturn(OTHER_USERNAME);
            when(revenueDto.amount()).thenReturn(AMOUNT);
            when(revenueDto.description()).thenReturn(DESCRIPTION);
            stubSavedRevenue();

            service.addSharedRevenue(revenueDto, MEMBER_ID);

            verify(sharedRevenueRepository).save(revenueCaptor.capture());
            assertEquals(MEMBER_ID, revenueCaptor.getValue().getCreatedByUserId());
            verify(sharedWalletService).addBalanceToWallet(MEMBER_ID, AMOUNT);
        }

        @Test
        void shouldAddAmountToWalletWhenRevenueIsAdded() {
            stubAddInputs();
            stubSavedRevenue();

            service.addSharedRevenue(revenueDto, OWNER_ID);

            verify(sharedWalletService).addBalanceToWallet(OWNER_ID, AMOUNT);
        }

        @Test
        void shouldSaveOutboxEventWhenRevenueIsAdded() {
            stubAddInputs();
            stubSavedRevenue();

            service.addSharedRevenue(revenueDto, OWNER_ID);

            verifyOutboxEvent(OWNER_ID, SharedAccountActivityLogType.REVENUE_CREATED);
        }

        @Test
        void shouldUpdateWalletBeforeSavingRevenueAndOutboxEventWhenRevenueIsAdded() {
            stubAddInputs();
            stubSavedRevenue();

            service.addSharedRevenue(revenueDto, OWNER_ID);

            InOrder inOrder = inOrder(sharedWalletService, sharedRevenueRepository, outboxService);
            inOrder.verify(sharedWalletService).addBalanceToWallet(OWNER_ID, AMOUNT);
            inOrder.verify(sharedRevenueRepository).save(any(SharedRevenue.class));
            inOrder.verify(outboxService).save(anyString(), anyString(), anyString(), any());
        }

        @Test
        void shouldThrowExceptionWhenDtoIsNull() {
            assertThrows(NullPointerException.class, () -> service.addSharedRevenue(null, OWNER_ID));

            verifyNoInteractions(sharedWalletService, sharedRevenueRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenParticipantsResponseIsNull() {
            assertThrows(NullPointerException.class, () -> service.addSharedRevenue(revenueDto, OWNER_ID));

            verifyNoInteractions(sharedWalletService, sharedRevenueRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenParticipantsServiceFails() {
            when(sharedAccountParticipantsService.getParticipants(OWNER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("participants not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.addSharedRevenue(revenueDto, OWNER_ID));

            verifyNoInteractions(authBackendClient, sharedWalletService, sharedRevenueRepository, outboxService);
        }


        @Test
        void shouldThrowExceptionWhenWalletUpdateFails() {
            stubAddInputs();
            doThrow(new IllegalStateException("wallet failed")).when(sharedWalletService).addBalanceToWallet(OWNER_ID, AMOUNT);

            assertThrows(IllegalStateException.class, () -> service.addSharedRevenue(revenueDto, OWNER_ID));

            verifyNoInteractions(sharedRevenueRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenRepositorySaveFails() {
            stubAddInputs();
            when(sharedRevenueRepository.save(any(SharedRevenue.class))).thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> service.addSharedRevenue(revenueDto, OWNER_ID));

            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenOutboxServiceFails() {
            stubAddInputs();
            stubSavedRevenue();
            doThrow(new IllegalStateException("outbox failed")).when(outboxService)
                    .save(anyString(), anyString(), anyString(), any());

            assertThrows(IllegalStateException.class, () -> service.addSharedRevenue(revenueDto, OWNER_ID));
        }
    }

    @Nested
    class EditRevenue {

        private void stubEditInputs() {
            when(revenueDto.amount()).thenReturn(AMOUNT);
            when(revenueDto.description()).thenReturn(DESCRIPTION);
        }

        private void stubSuccessfulEdit(Long userId) {
            when(sharedRevenueManagerService.getSharedRevenueOrThrow(REVENUE_ID)).thenReturn(existingRevenue);
            when(existingRevenue.getOwnerId()).thenReturn(OWNER_ID);
            when(existingRevenue.getAmount()).thenReturn(OLD_AMOUNT);
            when(existingRevenue.getId()).thenReturn(REVENUE_ID);
            when(sharedRevenueRepository.save(existingRevenue)).thenReturn(existingRevenue);
            stubEditInputs();
            stubParticipants(userId);
        }

        @Test
        void shouldReturnRevenueIdWhenOwnerEditsRevenue() {
            stubSuccessfulEdit(OWNER_ID);

            Long result = service.editRevenue(revenueDto, REVENUE_ID, OWNER_ID);

            assertEquals(REVENUE_ID, result);
        }

        @Test
        void shouldReturnRevenueIdWhenMemberEditsRevenue() {
            when(sharedRevenueManagerService.getSharedRevenueOrThrow(REVENUE_ID)).thenReturn(existingRevenue);
            when(existingRevenue.getOwnerId()).thenReturn(OWNER_ID);
            when(existingRevenue.getMemberId()).thenReturn(MEMBER_ID);
            when(existingRevenue.getAmount()).thenReturn(OLD_AMOUNT);
            when(existingRevenue.getId()).thenReturn(REVENUE_ID);
            when(sharedRevenueRepository.save(existingRevenue)).thenReturn(existingRevenue);
            stubEditInputs();
            stubParticipants(MEMBER_ID);

            Long result = service.editRevenue(revenueDto, REVENUE_ID, MEMBER_ID);

            assertEquals(REVENUE_ID, result);
            verifyOutboxEvent(MEMBER_ID, SharedAccountActivityLogType.REVENUE_EDITED);
        }

        @Test
        void shouldUpdateRevenueFieldsWhenRevenueIsEdited() {
            stubSuccessfulEdit(OWNER_ID);

            service.editRevenue(revenueDto, REVENUE_ID, OWNER_ID);

            verify(existingRevenue).setAmount(AMOUNT);
            verify(existingRevenue).setCategory(revenueDto.category());
            verify(existingRevenue).setDescription(DESCRIPTION);
            verify(sharedRevenueRepository).save(existingRevenue);
        }

        @Test
        void shouldAddNewAmountAndRemoveOldAmountFromWalletWhenRevenueIsEdited() {
            stubSuccessfulEdit(OWNER_ID);

            service.editRevenue(revenueDto, REVENUE_ID, OWNER_ID);

            InOrder inOrder = inOrder(sharedWalletService);
            inOrder.verify(sharedWalletService).addBalanceToWallet(OWNER_ID, AMOUNT);
            inOrder.verify(sharedWalletService).removeBalanceFromWallet(OWNER_ID, OLD_AMOUNT);
        }

        @Test
        void shouldSaveOutboxEventWhenRevenueIsEdited() {
            stubSuccessfulEdit(OWNER_ID);

            service.editRevenue(revenueDto, REVENUE_ID, OWNER_ID);

            verifyOutboxEvent(OWNER_ID, SharedAccountActivityLogType.REVENUE_EDITED);
        }

        @Test
        void shouldThrowExceptionWhenUserIsNeitherOwnerNorMember() {
            when(sharedRevenueManagerService.getSharedRevenueOrThrow(REVENUE_ID)).thenReturn(existingRevenue);
            when(existingRevenue.getOwnerId()).thenReturn(OWNER_ID);
            when(existingRevenue.getMemberId()).thenReturn(MEMBER_ID);

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> service.editRevenue(revenueDto, REVENUE_ID, OUTSIDER_ID));

            verifyNoInteractions(sharedWalletService, sharedRevenueRepository, outboxService, sharedAccountParticipantsService);
        }

        @Test
        void shouldThrowExceptionWhenRevenueDoesNotExist() {
            when(sharedRevenueManagerService.getSharedRevenueOrThrow(REVENUE_ID))
                    .thenThrow(new RequestedEntityNotFoundException("Revenue not found"));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> service.editRevenue(revenueDto, REVENUE_ID, OWNER_ID));

            verifyNoInteractions(sharedWalletService, sharedRevenueRepository, outboxService);
        }


        @Test
        void shouldThrowExceptionWhenDtoIsNull() {
            when(sharedRevenueManagerService.getSharedRevenueOrThrow(REVENUE_ID)).thenReturn(existingRevenue);
            when(existingRevenue.getOwnerId()).thenReturn(OWNER_ID);

            assertThrows(NullPointerException.class, () -> service.editRevenue(null, REVENUE_ID, OWNER_ID));

            verifyNoInteractions(sharedWalletService, sharedRevenueRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenAddingBalanceToWalletFails() {
            when(sharedRevenueManagerService.getSharedRevenueOrThrow(REVENUE_ID)).thenReturn(existingRevenue);
            when(existingRevenue.getOwnerId()).thenReturn(OWNER_ID);
            when(revenueDto.amount()).thenReturn(AMOUNT);
            doThrow(new IllegalStateException("wallet failed")).when(sharedWalletService).addBalanceToWallet(OWNER_ID, AMOUNT);

            assertThrows(IllegalStateException.class, () -> service.editRevenue(revenueDto, REVENUE_ID, OWNER_ID));

            verify(sharedWalletService, never()).removeBalanceFromWallet(any(Long.class), any(BigDecimal.class));
            verifyNoInteractions(sharedRevenueRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenRemovingBalanceFromWalletFails() {
            when(sharedRevenueManagerService.getSharedRevenueOrThrow(REVENUE_ID)).thenReturn(existingRevenue);
            when(existingRevenue.getOwnerId()).thenReturn(OWNER_ID);
            when(existingRevenue.getAmount()).thenReturn(OLD_AMOUNT);
            when(revenueDto.amount()).thenReturn(AMOUNT);
            doThrow(new IllegalStateException("wallet failed")).when(sharedWalletService)
                    .removeBalanceFromWallet(OWNER_ID, OLD_AMOUNT);

            assertThrows(IllegalStateException.class, () -> service.editRevenue(revenueDto, REVENUE_ID, OWNER_ID));

            verifyNoInteractions(sharedRevenueRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenRepositorySaveFails() {
            when(sharedRevenueManagerService.getSharedRevenueOrThrow(REVENUE_ID)).thenReturn(existingRevenue);
            when(existingRevenue.getOwnerId()).thenReturn(OWNER_ID);
            when(existingRevenue.getAmount()).thenReturn(OLD_AMOUNT);
            stubEditInputs();
            when(sharedRevenueRepository.save(existingRevenue)).thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> service.editRevenue(revenueDto, REVENUE_ID, OWNER_ID));

            verifyNoInteractions(outboxService, sharedAccountParticipantsService);
        }

        @Test
        void shouldThrowExceptionWhenOutboxServiceFails() {
            stubSuccessfulEdit(OWNER_ID);
            doThrow(new IllegalStateException("outbox failed")).when(outboxService)
                    .save(anyString(), anyString(), anyString(), any());

            assertThrows(IllegalStateException.class, () -> service.editRevenue(revenueDto, REVENUE_ID, OWNER_ID));
        }
    }

    @Nested
    class GetRevenue {

        @Test
        void shouldReturnEmptyListWhenNoRevenuesExist() {
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(OWNER_ID)).thenReturn(List.of());

            List<SharedRevenueDto> result = service.getRevenue(OWNER_ID);

            assertTrue(result.isEmpty());
            verifyNoInteractions(authBackendClient, sharedRevenueMapper);
        }

        @Test
        void shouldReturnMappedRevenueWithUsernameWhenSingleRevenueExists() {
            SharedRevenueDto mappedDto = mock(SharedRevenueDto.class);
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(OWNER_ID)).thenReturn(List.of(existingRevenue));
            when(existingRevenue.getCreatedByUserId()).thenReturn(OWNER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
            when(sharedRevenueMapper.mapToDto(existingRevenue, USERNAME)).thenReturn(mappedDto);

            List<SharedRevenueDto> result = service.getRevenue(OWNER_ID);

            assertEquals(List.of(mappedDto), result);
        }

        @Test
        void shouldMapEachRevenueWithItsCreatorUsernameWhenCreatorsDiffer() {
            SharedRevenueDto firstDto = mock(SharedRevenueDto.class);
            SharedRevenueDto secondDto = mock(SharedRevenueDto.class);
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(OWNER_ID))
                    .thenReturn(List.of(existingRevenue, savedRevenue));
            when(existingRevenue.getCreatedByUserId()).thenReturn(OWNER_ID);
            when(savedRevenue.getCreatedByUserId()).thenReturn(MEMBER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
            when(authBackendClient.getUsername(MEMBER_ID)).thenReturn(OTHER_USERNAME);
            when(sharedRevenueMapper.mapToDto(existingRevenue, USERNAME)).thenReturn(firstDto);
            when(sharedRevenueMapper.mapToDto(savedRevenue, OTHER_USERNAME)).thenReturn(secondDto);

            List<SharedRevenueDto> result = service.getRevenue(OWNER_ID);

            assertEquals(List.of(firstDto, secondDto), result);
        }

        @Test
        void shouldFetchUsernameOnceWhenRevenuesShareSameCreator() {
            SharedRevenueDto firstDto = mock(SharedRevenueDto.class);
            SharedRevenueDto secondDto = mock(SharedRevenueDto.class);
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(OWNER_ID))
                    .thenReturn(List.of(existingRevenue, savedRevenue));
            when(existingRevenue.getCreatedByUserId()).thenReturn(OWNER_ID);
            when(savedRevenue.getCreatedByUserId()).thenReturn(OWNER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
            when(sharedRevenueMapper.mapToDto(existingRevenue, USERNAME)).thenReturn(firstDto);
            when(sharedRevenueMapper.mapToDto(savedRevenue, USERNAME)).thenReturn(secondDto);

            List<SharedRevenueDto> result = service.getRevenue(OWNER_ID);

            assertEquals(2, result.size());
            verify(authBackendClient).getUsername(OWNER_ID);
        }

        @Test
        void shouldThrowExceptionWhenUsernameIsNull() {
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(OWNER_ID)).thenReturn(List.of(existingRevenue));
            when(existingRevenue.getCreatedByUserId()).thenReturn(OWNER_ID);

            assertThrows(NullPointerException.class, () -> service.getRevenue(OWNER_ID));

            verifyNoInteractions(sharedRevenueMapper);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(OWNER_ID))
                    .thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> service.getRevenue(OWNER_ID));

            verifyNoInteractions(authBackendClient, sharedRevenueMapper);
        }

        @Test
        void shouldThrowExceptionWhenAuthBackendClientFails() {
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(OWNER_ID)).thenReturn(List.of(existingRevenue));
            when(existingRevenue.getCreatedByUserId()).thenReturn(OWNER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenThrow(new IllegalStateException("auth down"));

            assertThrows(IllegalStateException.class, () -> service.getRevenue(OWNER_ID));

            verifyNoInteractions(sharedRevenueMapper);
        }

        @Test
        void shouldThrowExceptionWhenMapperFails() {
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(OWNER_ID)).thenReturn(List.of(existingRevenue));
            when(existingRevenue.getCreatedByUserId()).thenReturn(OWNER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
            when(sharedRevenueMapper.mapToDto(existingRevenue, USERNAME)).thenThrow(new IllegalArgumentException("mapping failed"));

            assertThrows(IllegalArgumentException.class, () -> service.getRevenue(OWNER_ID));
        }
    }

    @Nested
    class DeleteRevenue {

        private void stubSuccessfulDelete() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, OWNER_ID))
                    .thenReturn(Optional.of(existingRevenue));
            when(existingRevenue.getAmount()).thenReturn(OLD_AMOUNT);
            when(existingRevenue.getId()).thenReturn(REVENUE_ID);
            stubParticipants(OWNER_ID);
        }

        @Test
        void shouldDeleteRevenueWhenRevenueExists() {
            stubSuccessfulDelete();

            service.deleteRevenue(REVENUE_ID, OWNER_ID);

            verify(sharedRevenueRepository).delete(existingRevenue);
        }

        @Test
        void shouldRemoveRevenueAmountFromWalletWhenRevenueIsDeleted() {
            stubSuccessfulDelete();

            service.deleteRevenue(REVENUE_ID, OWNER_ID);

            verify(sharedWalletService).removeBalanceFromWallet(OWNER_ID, OLD_AMOUNT);
        }

        @Test
        void shouldSaveOutboxEventWhenRevenueIsDeleted() {
            stubSuccessfulDelete();

            service.deleteRevenue(REVENUE_ID, OWNER_ID);

            verifyOutboxEvent(OWNER_ID, SharedAccountActivityLogType.REVENUE_DELETED);
        }

        @Test
        void shouldUpdateWalletBeforeDeletingAndSavingEventWhenRevenueIsDeleted() {
            stubSuccessfulDelete();

            service.deleteRevenue(REVENUE_ID, OWNER_ID);

            InOrder inOrder = inOrder(sharedWalletService, sharedRevenueRepository, outboxService);
            inOrder.verify(sharedWalletService).removeBalanceFromWallet(OWNER_ID, OLD_AMOUNT);
            inOrder.verify(sharedRevenueRepository).delete(existingRevenue);
            inOrder.verify(outboxService).save(anyString(), anyString(), anyString(), any());
        }

        @Test
        void shouldThrowExceptionWhenRevenueDoesNotExist() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, OWNER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.deleteRevenue(REVENUE_ID, OWNER_ID));

            verifyNoInteractions(sharedWalletService, sharedAccountParticipantsService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryLookupFails() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, OWNER_ID))
                    .thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> service.deleteRevenue(REVENUE_ID, OWNER_ID));

            verifyNoInteractions(sharedWalletService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenWalletUpdateFails() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, OWNER_ID))
                    .thenReturn(Optional.of(existingRevenue));
            when(existingRevenue.getAmount()).thenReturn(OLD_AMOUNT);
            doThrow(new IllegalStateException("wallet failed")).when(sharedWalletService)
                    .removeBalanceFromWallet(OWNER_ID, OLD_AMOUNT);

            assertThrows(IllegalStateException.class, () -> service.deleteRevenue(REVENUE_ID, OWNER_ID));

            verify(sharedRevenueRepository, never()).delete(any(SharedRevenue.class));
            verifyNoInteractions(sharedAccountParticipantsService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenParticipantsServiceFails() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, OWNER_ID))
                    .thenReturn(Optional.of(existingRevenue));
            when(existingRevenue.getAmount()).thenReturn(OLD_AMOUNT);
            when(sharedAccountParticipantsService.getParticipants(OWNER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("participants not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.deleteRevenue(REVENUE_ID, OWNER_ID));

            verify(sharedRevenueRepository, never()).delete(any(SharedRevenue.class));
            verifyNoInteractions(outboxService);
        }



        @Test
        void shouldThrowExceptionWhenOutboxServiceFails() {
            stubSuccessfulDelete();
            doThrow(new IllegalStateException("outbox failed")).when(outboxService)
                    .save(anyString(), anyString(), anyString(), any());

            assertThrows(IllegalStateException.class, () -> service.deleteRevenue(REVENUE_ID, OWNER_ID));
        }
    }
}