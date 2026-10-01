package com.finovara.financeservice.sharedaccount.revenue.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.finance.event.sharedaccount.finance.SharedAccountRevenueActivityEvent;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.outbox.OutboxService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedRevenueServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long REVENUE_ID = 100L;
    private static final Long OWNER_ID = 10L;
    private static final Long MEMBER_ID = 20L;
    private static final String USERNAME = "john";
    private static final String OTHER_USERNAME = "anna";
    private static final String DESCRIPTION = "Salary";
    private static final BigDecimal AMOUNT = new BigDecimal("500.00");
    private static final BigDecimal OLD_AMOUNT = new BigDecimal("300.00");

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
    private SharedRevenueDto sharedRevenueDto;

    @Mock
    private SharedRevenueDto firstMappedDto;

    @Mock
    private SharedRevenueDto secondMappedDto;

    @Mock
    private SharedAccountParticipantsResponse participants;

    @Mock
    private SharedRevenue existingRevenue;

    @Mock
    private SharedRevenue savedRevenue;

    @Mock
    private SharedRevenue firstRevenue;

    @Mock
    private SharedRevenue secondRevenue;

    @Captor
    private ArgumentCaptor<SharedRevenue> revenueCaptor;

    @InjectMocks
    private SharedRevenueService sharedRevenueService;

    private RevenueCategory category;

    @BeforeEach
    void setUp() {
        category = RevenueCategory.values()[0];
    }

    @Nested
    class AddSharedRevenue {

        @Nested
        class Success {

            @BeforeEach
            void setUp() {
                when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
                when(participants.ownerId()).thenReturn(OWNER_ID);
                when(participants.memberId()).thenReturn(MEMBER_ID);
                when(authBackendClient.getUsername(USER_ID)).thenReturn(USERNAME);
                when(sharedRevenueDto.amount()).thenReturn(AMOUNT);
                when(sharedRevenueDto.category()).thenReturn(category);
                when(sharedRevenueDto.description()).thenReturn(DESCRIPTION);
                when(sharedRevenueRepository.save(any(SharedRevenue.class))).thenReturn(savedRevenue);
                when(savedRevenue.getId()).thenReturn(REVENUE_ID);
                when(savedRevenue.getAmount()).thenReturn(AMOUNT);
            }

            @Test
            void shouldReturnResponseWithCreatorWhenRevenueIsAdded() {
                SharedRevenueResponse result = sharedRevenueService.addSharedRevenue(sharedRevenueDto, USER_ID);

                assertEquals(new SharedRevenueResponse(null, USER_ID, USERNAME), result);
            }

            @Test
            void shouldSaveRevenueWithDtoDataWhenRevenueIsAdded() {
                sharedRevenueService.addSharedRevenue(sharedRevenueDto, USER_ID);

                verify(sharedRevenueRepository).save(revenueCaptor.capture());
                SharedRevenue captured = revenueCaptor.getValue();
                assertEquals(AMOUNT, captured.getAmount());
                assertEquals(category, captured.getCategory());
                assertEquals(DESCRIPTION, captured.getDescription());
                assertEquals(OWNER_ID, captured.getOwnerId());
                assertEquals(MEMBER_ID, captured.getMemberId());
                assertEquals(USER_ID, captured.getCreatedByUserId());
            }

            @Test
            void shouldAddAmountToWalletWhenRevenueIsAdded() {
                sharedRevenueService.addSharedRevenue(sharedRevenueDto, USER_ID);

                verify(sharedWalletService).addBalanceToWallet(USER_ID, AMOUNT);
            }

            @Test
            void shouldAddToWalletBeforeSavingWhenRevenueIsAdded() {
                InOrder inOrder = inOrder(sharedWalletService, sharedRevenueRepository);

                sharedRevenueService.addSharedRevenue(sharedRevenueDto, USER_ID);

                inOrder.verify(sharedWalletService).addBalanceToWallet(USER_ID, AMOUNT);
                inOrder.verify(sharedRevenueRepository).save(any(SharedRevenue.class));
            }

            @Test
            void shouldSaveRevenueEventToOutboxWhenRevenueIsAdded() {
                sharedRevenueService.addSharedRevenue(sharedRevenueDto, USER_ID);

                verify(outboxService).save(eq("SharedAccountRevenue"), eq(REVENUE_ID.toString()), eq("shared-account.revenue.created"), any(SharedAccountRevenueActivityEvent.class));
            }

            @Test
            void shouldLookUpParticipantsAndUsernameOfCreatorWhenRevenueIsAdded() {
                sharedRevenueService.addSharedRevenue(sharedRevenueDto, USER_ID);

                verify(sharedAccountParticipantsService).getParticipants(USER_ID);
                verify(authBackendClient).getUsername(USER_ID);
            }

            @Test
            void shouldSetCreationDateToTodayWhenRevenueIsAdded() {
                sharedRevenueService.addSharedRevenue(sharedRevenueDto, USER_ID);

                verify(sharedRevenueRepository).save(revenueCaptor.capture());
                assertEquals(LocalDate.now(), revenueCaptor.getValue().getCreatedAt());
            }
        }

        @Nested
        class EarlyFailures {

            @Test
            void shouldThrowExceptionWhenParticipantsLookupFails() {
                when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenThrow(new IllegalStateException());

                assertThrows(IllegalStateException.class, () -> sharedRevenueService.addSharedRevenue(sharedRevenueDto, USER_ID));
                verifyNoInteractions(authBackendClient, sharedWalletService, sharedRevenueRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenUsernameLookupFails() {
                when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
                when(authBackendClient.getUsername(USER_ID)).thenThrow(new IllegalStateException());

                assertThrows(IllegalStateException.class, () -> sharedRevenueService.addSharedRevenue(sharedRevenueDto, USER_ID));
                verifyNoInteractions(sharedWalletService, sharedRevenueRepository, outboxService);
            }
        }

        @Nested
        class LateFailures {

            @BeforeEach
            void setUp() {
                when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
                when(participants.ownerId()).thenReturn(OWNER_ID);
                when(participants.memberId()).thenReturn(MEMBER_ID);
                when(authBackendClient.getUsername(USER_ID)).thenReturn(USERNAME);
                when(sharedRevenueDto.amount()).thenReturn(AMOUNT);
                when(sharedRevenueDto.category()).thenReturn(category);
                when(sharedRevenueDto.description()).thenReturn(DESCRIPTION);
            }

            @Test
            void shouldThrowExceptionWhenWalletDepositFails() {
                doThrow(new IllegalStateException()).when(sharedWalletService).addBalanceToWallet(USER_ID, AMOUNT);

                assertThrows(IllegalStateException.class, () -> sharedRevenueService.addSharedRevenue(sharedRevenueDto, USER_ID));
                verifyNoInteractions(sharedRevenueRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenSavingRevenueFails() {
                when(sharedRevenueRepository.save(any(SharedRevenue.class))).thenThrow(new IllegalStateException());

                assertThrows(IllegalStateException.class, () -> sharedRevenueService.addSharedRevenue(sharedRevenueDto, USER_ID));
                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldThrowExceptionWhenOutboxSaveFails() {
                when(sharedRevenueRepository.save(any(SharedRevenue.class))).thenReturn(savedRevenue);
                when(savedRevenue.getId()).thenReturn(REVENUE_ID);
                when(savedRevenue.getAmount()).thenReturn(AMOUNT);
                doThrow(new IllegalStateException()).when(outboxService).save(anyString(), anyString(), anyString(), any());

                assertThrows(IllegalStateException.class, () -> sharedRevenueService.addSharedRevenue(sharedRevenueDto, USER_ID));
            }
        }
    }

    @Nested
    class EditRevenue {

        @Nested
        class AsOwner {

            @BeforeEach
            void setUp() {
                when(sharedRevenueManagerService.getSharedRevenueOrThrow(REVENUE_ID)).thenReturn(existingRevenue);
                when(existingRevenue.getOwnerId()).thenReturn(USER_ID);
                when(existingRevenue.getAmount()).thenReturn(OLD_AMOUNT);
                when(sharedRevenueDto.amount()).thenReturn(AMOUNT);
                when(sharedRevenueDto.category()).thenReturn(category);
                when(sharedRevenueDto.description()).thenReturn(DESCRIPTION);
            }

            @Test
            void shouldReturnRevenueIdWhenRevenueIsEdited() {
                Long result = sharedRevenueService.editRevenue(sharedRevenueDto, REVENUE_ID, USER_ID);

                assertEquals(REVENUE_ID, result);
            }

            @Test
            void shouldUpdateRevenueFieldsWhenRevenueIsEdited() {
                sharedRevenueService.editRevenue(sharedRevenueDto, REVENUE_ID, USER_ID);

                verify(existingRevenue).setAmount(AMOUNT);
                verify(existingRevenue).setCategory(category);
                verify(existingRevenue).setDescription(DESCRIPTION);
            }

            @Test
            void shouldSaveExistingRevenueWhenRevenueIsEdited() {
                sharedRevenueService.editRevenue(sharedRevenueDto, REVENUE_ID, USER_ID);

                verify(sharedRevenueRepository).save(existingRevenue);
            }

            @Test
            void shouldAddNewAmountAndRemoveOldAmountFromWalletWhenRevenueIsEdited() {
                InOrder inOrder = inOrder(sharedWalletService);

                sharedRevenueService.editRevenue(sharedRevenueDto, REVENUE_ID, USER_ID);

                inOrder.verify(sharedWalletService).addBalanceToWallet(USER_ID, AMOUNT);
                inOrder.verify(sharedWalletService).removeBalanceFromWallet(USER_ID, OLD_AMOUNT);
            }

            @Test
            void shouldNotUseOutboxOrParticipantsWhenRevenueIsEdited() {
                sharedRevenueService.editRevenue(sharedRevenueDto, REVENUE_ID, USER_ID);

                verifyNoInteractions(outboxService, sharedAccountParticipantsService, authBackendClient);
            }

            @Test
            void shouldEditRevenueWhenUserIsMember() {
                when(existingRevenue.getOwnerId()).thenReturn(OTHER_USER_ID);
                when(existingRevenue.getMemberId()).thenReturn(USER_ID);

                Long result = sharedRevenueService.editRevenue(sharedRevenueDto, REVENUE_ID, USER_ID);

                assertEquals(REVENUE_ID, result);
                verify(sharedRevenueRepository).save(existingRevenue);
            }

            @Test
            void shouldThrowExceptionWhenSavingEditedRevenueFails() {
                when(sharedRevenueRepository.save(existingRevenue)).thenThrow(new IllegalStateException());

                assertThrows(IllegalStateException.class, () -> sharedRevenueService.editRevenue(sharedRevenueDto, REVENUE_ID, USER_ID));
            }
        }

        @Nested
        class Failures {

            @Test
            void shouldThrowExceptionWhenRevenueDoesNotExist() {
                when(sharedRevenueManagerService.getSharedRevenueOrThrow(REVENUE_ID)).thenThrow(new RequestedEntityNotFoundException("Revenue not found"));

                assertThrows(RequestedEntityNotFoundException.class, () -> sharedRevenueService.editRevenue(sharedRevenueDto, REVENUE_ID, USER_ID));
                verifyNoInteractions(sharedWalletService, sharedRevenueRepository);
            }

            @Test
            void shouldThrowExceptionWhenUserIsNeitherOwnerNorMember() {
                when(sharedRevenueManagerService.getSharedRevenueOrThrow(REVENUE_ID)).thenReturn(existingRevenue);
                when(existingRevenue.getOwnerId()).thenReturn(OTHER_USER_ID);
                when(existingRevenue.getMemberId()).thenReturn(OTHER_USER_ID);

                RequestedEntityNotFoundException exception = assertThrows(RequestedEntityNotFoundException.class, () -> sharedRevenueService.editRevenue(sharedRevenueDto, REVENUE_ID, USER_ID));

                assertEquals("Revenue not found for this user", exception.getMessage());
            }

            @Test
            void shouldNotChangeAnythingWhenUserIsNeitherOwnerNorMember() {
                when(sharedRevenueManagerService.getSharedRevenueOrThrow(REVENUE_ID)).thenReturn(existingRevenue);
                when(existingRevenue.getOwnerId()).thenReturn(OTHER_USER_ID);
                when(existingRevenue.getMemberId()).thenReturn(OTHER_USER_ID);

                assertThrows(RequestedEntityNotFoundException.class, () -> sharedRevenueService.editRevenue(sharedRevenueDto, REVENUE_ID, USER_ID));

                verifyNoInteractions(sharedWalletService, sharedRevenueRepository);
                verify(existingRevenue, never()).setAmount(any(BigDecimal.class));
            }

            @Test
            void shouldThrowExceptionWhenWalletDepositFailsOnEdit() {
                when(sharedRevenueManagerService.getSharedRevenueOrThrow(REVENUE_ID)).thenReturn(existingRevenue);
                when(existingRevenue.getOwnerId()).thenReturn(USER_ID);
                when(sharedRevenueDto.amount()).thenReturn(AMOUNT);
                doThrow(new IllegalStateException()).when(sharedWalletService).addBalanceToWallet(USER_ID, AMOUNT);

                assertThrows(IllegalStateException.class, () -> sharedRevenueService.editRevenue(sharedRevenueDto, REVENUE_ID, USER_ID));
                verifyNoInteractions(sharedRevenueRepository);
            }

            @Test
            void shouldThrowExceptionWhenWalletWithdrawalFailsOnEdit() {
                when(sharedRevenueManagerService.getSharedRevenueOrThrow(REVENUE_ID)).thenReturn(existingRevenue);
                when(existingRevenue.getOwnerId()).thenReturn(USER_ID);
                when(existingRevenue.getAmount()).thenReturn(OLD_AMOUNT);
                when(sharedRevenueDto.amount()).thenReturn(AMOUNT);
                doThrow(new IllegalStateException()).when(sharedWalletService).removeBalanceFromWallet(USER_ID, OLD_AMOUNT);

                assertThrows(IllegalStateException.class, () -> sharedRevenueService.editRevenue(sharedRevenueDto, REVENUE_ID, USER_ID));
                verifyNoInteractions(sharedRevenueRepository);
            }
        }
    }

    @Nested
    class GetRevenue {

        @Test
        void shouldReturnEmptyListWhenNoRevenuesExist() {
            List<SharedRevenueDto> result = sharedRevenueService.getRevenue(USER_ID);

            assertTrue(result.isEmpty());
            verifyNoInteractions(authBackendClient, sharedRevenueMapper);
        }

        @Test
        void shouldReturnMappedRevenueWhenSingleRevenueExists() {
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(USER_ID)).thenReturn(List.of(firstRevenue));
            when(firstRevenue.getCreatedByUserId()).thenReturn(USER_ID);
            when(authBackendClient.getUsername(USER_ID)).thenReturn(USERNAME);
            when(sharedRevenueMapper.mapToDto(firstRevenue, USERNAME)).thenReturn(firstMappedDto);

            List<SharedRevenueDto> result = sharedRevenueService.getRevenue(USER_ID);

            assertEquals(List.of(firstMappedDto), result);
        }

        @Test
        void shouldMapEachRevenueWithItsCreatorUsernameWhenCreatorsDiffer() {
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(USER_ID)).thenReturn(List.of(firstRevenue, secondRevenue));
            when(firstRevenue.getCreatedByUserId()).thenReturn(USER_ID);
            when(secondRevenue.getCreatedByUserId()).thenReturn(OTHER_USER_ID);
            when(authBackendClient.getUsername(USER_ID)).thenReturn(USERNAME);
            when(authBackendClient.getUsername(OTHER_USER_ID)).thenReturn(OTHER_USERNAME);
            when(sharedRevenueMapper.mapToDto(firstRevenue, USERNAME)).thenReturn(firstMappedDto);
            when(sharedRevenueMapper.mapToDto(secondRevenue, OTHER_USERNAME)).thenReturn(secondMappedDto);

            List<SharedRevenueDto> result = sharedRevenueService.getRevenue(USER_ID);

            assertEquals(List.of(firstMappedDto, secondMappedDto), result);
        }

        @Test
        void shouldFetchUsernameOnceWhenRevenuesShareCreator() {
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(USER_ID)).thenReturn(List.of(firstRevenue, secondRevenue));
            when(firstRevenue.getCreatedByUserId()).thenReturn(USER_ID);
            when(secondRevenue.getCreatedByUserId()).thenReturn(USER_ID);
            when(authBackendClient.getUsername(USER_ID)).thenReturn(USERNAME);
            when(sharedRevenueMapper.mapToDto(firstRevenue, USERNAME)).thenReturn(firstMappedDto);
            when(sharedRevenueMapper.mapToDto(secondRevenue, USERNAME)).thenReturn(secondMappedDto);

            List<SharedRevenueDto> result = sharedRevenueService.getRevenue(USER_ID);

            verify(authBackendClient, times(1)).getUsername(USER_ID);
            assertEquals(List.of(firstMappedDto, secondMappedDto), result);
        }

        @Test
        void shouldThrowExceptionWhenCreatorUsernameIsNull() {
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(USER_ID)).thenReturn(List.of(firstRevenue));
            when(firstRevenue.getCreatedByUserId()).thenReturn(USER_ID);

            assertThrows(NullPointerException.class, () -> sharedRevenueService.getRevenue(USER_ID));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(USER_ID)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> sharedRevenueService.getRevenue(USER_ID));
        }

        @Test
        void shouldThrowExceptionWhenUsernameLookupFails() {
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(USER_ID)).thenReturn(List.of(firstRevenue));
            when(firstRevenue.getCreatedByUserId()).thenReturn(USER_ID);
            when(authBackendClient.getUsername(USER_ID)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> sharedRevenueService.getRevenue(USER_ID));
            verifyNoInteractions(sharedRevenueMapper);
        }

        @Test
        void shouldThrowExceptionWhenMapperFails() {
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(USER_ID)).thenReturn(List.of(firstRevenue));
            when(firstRevenue.getCreatedByUserId()).thenReturn(USER_ID);
            when(authBackendClient.getUsername(USER_ID)).thenReturn(USERNAME);
            when(sharedRevenueMapper.mapToDto(firstRevenue, USERNAME)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> sharedRevenueService.getRevenue(USER_ID));
        }
    }

    @Nested
    class DeleteRevenue {

        @Test
        void shouldRemoveAmountFromWalletWhenRevenueExists() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, USER_ID)).thenReturn(Optional.of(existingRevenue));
            when(existingRevenue.getAmount()).thenReturn(AMOUNT);

            sharedRevenueService.deleteRevenue(REVENUE_ID, USER_ID);

            verify(sharedWalletService).removeBalanceFromWallet(USER_ID, AMOUNT);
        }

        @Test
        void shouldDeleteRevenueWhenRevenueExists() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, USER_ID)).thenReturn(Optional.of(existingRevenue));
            when(existingRevenue.getAmount()).thenReturn(AMOUNT);

            sharedRevenueService.deleteRevenue(REVENUE_ID, USER_ID);

            verify(sharedRevenueRepository).delete(existingRevenue);
        }

        @Test
        void shouldRemoveFromWalletBeforeDeletingWhenRevenueExists() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, USER_ID)).thenReturn(Optional.of(existingRevenue));
            when(existingRevenue.getAmount()).thenReturn(AMOUNT);
            InOrder inOrder = inOrder(sharedWalletService, sharedRevenueRepository);

            sharedRevenueService.deleteRevenue(REVENUE_ID, USER_ID);

            inOrder.verify(sharedWalletService).removeBalanceFromWallet(USER_ID, AMOUNT);
            inOrder.verify(sharedRevenueRepository).delete(existingRevenue);
        }

        @Test
        void shouldThrowExceptionWhenRevenueDoesNotExist() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, USER_ID)).thenReturn(Optional.empty());

            RequestedEntityNotFoundException exception = assertThrows(RequestedEntityNotFoundException.class, () -> sharedRevenueService.deleteRevenue(REVENUE_ID, USER_ID));

            assertEquals("Revenue not found", exception.getMessage());
        }

        @Test
        void shouldNotTouchWalletOrDeleteWhenRevenueDoesNotExist() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> sharedRevenueService.deleteRevenue(REVENUE_ID, USER_ID));

            verifyNoInteractions(sharedWalletService);
            verify(sharedRevenueRepository, never()).delete(any(SharedRevenue.class));
        }

        @Test
        void shouldNotDeleteRevenueWhenWalletWithdrawalFails() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, USER_ID)).thenReturn(Optional.of(existingRevenue));
            when(existingRevenue.getAmount()).thenReturn(AMOUNT);
            doThrow(new IllegalStateException()).when(sharedWalletService).removeBalanceFromWallet(USER_ID, AMOUNT);

            assertThrows(IllegalStateException.class, () -> sharedRevenueService.deleteRevenue(REVENUE_ID, USER_ID));

            verify(sharedRevenueRepository, never()).delete(any(SharedRevenue.class));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryDeleteFails() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, USER_ID)).thenReturn(Optional.of(existingRevenue));
            when(existingRevenue.getAmount()).thenReturn(AMOUNT);
            doThrow(new IllegalStateException()).when(sharedRevenueRepository).delete(existingRevenue);

            assertThrows(IllegalStateException.class, () -> sharedRevenueService.deleteRevenue(REVENUE_ID, USER_ID));
        }
    }
}