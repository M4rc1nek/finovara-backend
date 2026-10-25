package com.finovara.financeservice.sharedaccount.revenue.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.revenue.dto.SharedRevenueDto;
import com.finovara.financeservice.sharedaccount.revenue.dto.SharedRevenueResponse;
import com.finovara.financeservice.sharedaccount.revenue.mapper.SharedRevenueMapper;
import com.finovara.financeservice.sharedaccount.revenue.model.SharedRevenue;
import com.finovara.financeservice.sharedaccount.revenue.repository.SharedRevenueRepository;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import com.finovara.financeservice.util.sharedaccount.SharedUsernameResolver;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class SharedRevenueServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long REVENUE_ID = 30L;
    private static final String USERNAME = "alice";
    private static final String OTHER_USERNAME = "bob";
    private static final String DESCRIPTION = "Salary";
    private static final String OUTBOX_AGGREGATE = "SharedAccountRevenue";
    private static final String OUTBOX_EVENT_TYPE = "shared-account.activity";
    private static final BigDecimal EXISTING_AMOUNT = new BigDecimal("100.00");
    private static final int PAGE_SIZE = 10;

    @Mock
    private SharedRevenueMapper sharedRevenueMapper;
    @Mock
    private SharedRevenueRepository sharedRevenueRepository;
    @Mock
    private SharedAccountParticipantsService sharedAccountParticipantsService;
    @Mock
    private SharedWalletService sharedWalletService;
    @Mock
    private SharedUsernameResolver usernameResolver;
    @Mock
    private OutboxService outboxService;
    @Mock
    private SharedRevenueDto dto;
    @Mock
    private SharedRevenueDto otherDto;
    @Mock
    private SharedAccountParticipantsResponse participants;

    @InjectMocks
    private SharedRevenueService service;

    private SharedRevenue existing;

    @BeforeEach
    void setUp() {
        setField(service, "pageSize", PAGE_SIZE);
        existing = SharedRevenue.builder()
                .id(REVENUE_ID)
                .amount(EXISTING_AMOUNT)
                .ownerId(OWNER_ID)
                .memberId(MEMBER_ID)
                .createdByUserId(USER_ID)
                .build();
    }

    @Nested
    class AddSharedRevenue {

        @Nested
        class WhenAmountIsValid {

            @BeforeEach
            void setUp() {
                when(dto.amount()).thenReturn(EXISTING_AMOUNT);
                when(dto.description()).thenReturn(DESCRIPTION);
                when(participants.ownerId()).thenReturn(OWNER_ID);
                when(participants.memberId()).thenReturn(MEMBER_ID);
                when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
                when(usernameResolver.resolve(List.of(USER_ID))).thenReturn(Map.of(USER_ID, USERNAME));
                when(sharedRevenueRepository.save(any(SharedRevenue.class))).thenReturn(existing);
            }

            @Test
            void shouldReturnResponseWithSavedIdAndUsernameWhenAmountIsValid() {
                SharedRevenueResponse result = service.addSharedRevenue(dto, USER_ID);

                assertEquals(new SharedRevenueResponse(REVENUE_ID, USER_ID, USERNAME), result);
            }

            @Test
            void shouldSaveRevenueWithParticipantsAndCreatorWhenAmountIsValid() {
                ArgumentCaptor<SharedRevenue> captor = ArgumentCaptor.forClass(SharedRevenue.class);

                service.addSharedRevenue(dto, USER_ID);

                verify(sharedRevenueRepository).save(captor.capture());
                SharedRevenue captured = captor.getValue();
                assertEquals(EXISTING_AMOUNT, captured.getAmount());
                assertEquals(DESCRIPTION, captured.getDescription());
                assertEquals(OWNER_ID, captured.getOwnerId());
                assertEquals(MEMBER_ID, captured.getMemberId());
                assertEquals(USER_ID, captured.getCreatedByUserId());
                assertNotNull(captured.getCreatedAt());
            }

            @Test
            void shouldAddBalanceToSharedWalletWhenAmountIsValid() {
                service.addSharedRevenue(dto, USER_ID);

                verify(sharedWalletService).addBalanceToWallet(USER_ID, EXISTING_AMOUNT);
            }

            @Test
            void shouldLogActivityWhenAmountIsValid() {
                service.addSharedRevenue(dto, USER_ID);

                verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(REVENUE_ID.toString()), eq(OUTBOX_EVENT_TYPE),
                        any(SharedAccountActivityLogEvent.class));
            }
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNull() {
            assertThrows(InvalidInputException.class, () -> service.addSharedRevenue(dto, USER_ID));

            verifyNoInteractions(sharedAccountParticipantsService, sharedWalletService, sharedRevenueRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenAmountIsZero() {
            when(dto.amount()).thenReturn(BigDecimal.ZERO);

            assertThrows(InvalidInputException.class, () -> service.addSharedRevenue(dto, USER_ID));

            verifyNoInteractions(sharedAccountParticipantsService, sharedWalletService, sharedRevenueRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNegative() {
            when(dto.amount()).thenReturn(new BigDecimal("-0.01"));

            assertThrows(InvalidInputException.class, () -> service.addSharedRevenue(dto, USER_ID));

            verifyNoInteractions(sharedAccountParticipantsService, sharedWalletService, sharedRevenueRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenParticipantsCannotBeResolved() {
            when(dto.amount()).thenReturn(EXISTING_AMOUNT);
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenThrow(new RequestedEntityNotFoundException("Shared account not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.addSharedRevenue(dto, USER_ID));

            verifyNoInteractions(sharedWalletService, sharedRevenueRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenSharedWalletRejectsDeposit() {
            when(dto.amount()).thenReturn(EXISTING_AMOUNT);
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
            when(usernameResolver.resolve(List.of(USER_ID))).thenReturn(Map.of(USER_ID, USERNAME));
            doThrow(new InvalidInputException("Wallet error")).when(sharedWalletService).addBalanceToWallet(USER_ID, EXISTING_AMOUNT);

            assertThrows(InvalidInputException.class, () -> service.addSharedRevenue(dto, USER_ID));

            verifyNoInteractions(sharedRevenueRepository, outboxService);
        }
    }

    @Nested
    class EditRevenue {

        @Nested
        class WhenRevenueExists {

            @BeforeEach
            void setUp() {
                when(dto.description()).thenReturn(DESCRIPTION);
                when(participants.ownerId()).thenReturn(OWNER_ID);
                when(participants.memberId()).thenReturn(MEMBER_ID);
                when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, USER_ID)).thenReturn(Optional.of(existing));
                when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
            }

            @Test
            void shouldReturnRevenueIdWhenRevenueIsEdited() {
                when(dto.amount()).thenReturn(EXISTING_AMOUNT);

                Long result = service.editRevenue(dto, REVENUE_ID, USER_ID);

                assertEquals(REVENUE_ID, result);
            }

            @Test
            void shouldLockWalletWhenRevenueIsEdited() {
                when(dto.amount()).thenReturn(EXISTING_AMOUNT);

                service.editRevenue(dto, REVENUE_ID, USER_ID);

                verify(sharedWalletService).lockWalletForUpdate(USER_ID);
            }

            @Test
            void shouldUpdateRevenueFieldsWhenRevenueIsEdited() {
                BigDecimal newAmount = new BigDecimal("150.00");
                when(dto.amount()).thenReturn(newAmount);

                service.editRevenue(dto, REVENUE_ID, USER_ID);

                assertEquals(newAmount, existing.getAmount());
                assertEquals(DESCRIPTION, existing.getDescription());
            }

            @Test
            void shouldAddDeltaToWalletWhenNewAmountIsHigher() {
                when(dto.amount()).thenReturn(new BigDecimal("150.00"));

                service.editRevenue(dto, REVENUE_ID, USER_ID);

                verify(sharedWalletService).addBalanceToWallet(USER_ID, new BigDecimal("50.00"));
                verify(sharedWalletService, never()).removeBalanceFromWallet(any(Long.class), any(BigDecimal.class));
            }

            @Test
            void shouldRemoveDeltaFromWalletWhenNewAmountIsLower() {
                when(dto.amount()).thenReturn(new BigDecimal("60.00"));

                service.editRevenue(dto, REVENUE_ID, USER_ID);

                verify(sharedWalletService).removeBalanceFromWallet(USER_ID, new BigDecimal("40.00"));
                verify(sharedWalletService, never()).addBalanceToWallet(any(Long.class), any(BigDecimal.class));
            }

            @Test
            void shouldNotChangeWalletBalanceWhenAmountIsUnchanged() {
                when(dto.amount()).thenReturn(new BigDecimal("100"));

                service.editRevenue(dto, REVENUE_ID, USER_ID);

                verify(sharedWalletService, never()).addBalanceToWallet(any(Long.class), any(BigDecimal.class));
                verify(sharedWalletService, never()).removeBalanceFromWallet(any(Long.class), any(BigDecimal.class));
            }

            @Test
            void shouldLogActivityWhenRevenueIsEdited() {
                when(dto.amount()).thenReturn(EXISTING_AMOUNT);

                service.editRevenue(dto, REVENUE_ID, USER_ID);

                verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(REVENUE_ID.toString()), eq(OUTBOX_EVENT_TYPE),
                        any(SharedAccountActivityLogEvent.class));
            }
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNull() {
            assertThrows(InvalidInputException.class, () -> service.editRevenue(dto, REVENUE_ID, USER_ID));

            verifyNoInteractions(sharedWalletService, sharedRevenueRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenAmountIsZero() {
            when(dto.amount()).thenReturn(BigDecimal.ZERO);

            assertThrows(InvalidInputException.class, () -> service.editRevenue(dto, REVENUE_ID, USER_ID));

            verifyNoInteractions(sharedWalletService, sharedRevenueRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNegative() {
            when(dto.amount()).thenReturn(new BigDecimal("-10.00"));

            assertThrows(InvalidInputException.class, () -> service.editRevenue(dto, REVENUE_ID, USER_ID));

            verifyNoInteractions(sharedWalletService, sharedRevenueRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenRevenueDoesNotExist() {
            when(dto.amount()).thenReturn(EXISTING_AMOUNT);
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.editRevenue(dto, REVENUE_ID, USER_ID));

            verify(sharedWalletService).lockWalletForUpdate(USER_ID);
            verify(sharedWalletService, never()).addBalanceToWallet(any(Long.class), any(BigDecimal.class));
            verify(sharedWalletService, never()).removeBalanceFromWallet(any(Long.class), any(BigDecimal.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenWalletCannotCoverDecrease() {
            when(dto.amount()).thenReturn(new BigDecimal("60.00"));
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, USER_ID)).thenReturn(Optional.of(existing));
            doThrow(new InvalidInputException("Insufficient funds")).when(sharedWalletService)
                    .removeBalanceFromWallet(USER_ID, new BigDecimal("40.00"));

            assertThrows(InvalidInputException.class, () -> service.editRevenue(dto, REVENUE_ID, USER_ID));

            verifyNoInteractions(outboxService);
        }
    }

    @Nested
    class GetRevenue {

        private SharedRevenue otherRevenue;

        @BeforeEach
        void setUp() {
            otherRevenue = SharedRevenue.builder()
                    .id(31L)
                    .amount(new BigDecimal("20.00"))
                    .createdByUserId(OTHER_USER_ID)
                    .build();
        }

        @Test
        void shouldReturnMappedDtosWhenRevenuesExist() {
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(USER_ID, PageRequest.of(0, PAGE_SIZE)))
                    .thenReturn(List.of(existing, otherRevenue));
            when(usernameResolver.resolve(Set.of(USER_ID, OTHER_USER_ID))).thenReturn(Map.of(USER_ID, USERNAME, OTHER_USER_ID, OTHER_USERNAME));
            when(sharedRevenueMapper.mapToDto(existing, USERNAME)).thenReturn(dto);
            when(sharedRevenueMapper.mapToDto(otherRevenue, OTHER_USERNAME)).thenReturn(otherDto);

            List<SharedRevenueDto> result = service.getRevenue(USER_ID, 0);

            assertEquals(List.of(dto, otherDto), result);
        }

        @Test
        void shouldUseRequestedPageAndConfiguredPageSizeWhenFetchingRevenues() {
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(USER_ID, PageRequest.of(3, PAGE_SIZE))).thenReturn(List.of());

            service.getRevenue(USER_ID, 3);

            verify(sharedRevenueRepository).findAllByOwnerIdOrMemberId(USER_ID, PageRequest.of(3, PAGE_SIZE));
        }

        @Test
        void shouldReturnEmptyListWhenNoRevenuesExist() {
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(USER_ID, PageRequest.of(0, PAGE_SIZE))).thenReturn(List.of());

            List<SharedRevenueDto> result = service.getRevenue(USER_ID, 0);

            assertEquals(List.of(), result);
            verifyNoInteractions(sharedRevenueMapper);
        }

        @Test
        void shouldMapWithNullUsernameWhenUsernameIsNotResolved() {
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(USER_ID, PageRequest.of(0, PAGE_SIZE))).thenReturn(List.of(existing));
            when(usernameResolver.resolve(Set.of(USER_ID))).thenReturn(Map.of());
            when(sharedRevenueMapper.mapToDto(existing, null)).thenReturn(dto);

            List<SharedRevenueDto> result = service.getRevenue(USER_ID, 0);

            assertSame(dto, result.get(0));
        }

        @Test
        void shouldResolveUsernamesOnlyOncePerCreatorWhenCreatorHasManyRevenues() {
            SharedRevenue secondByUser = SharedRevenue.builder().id(32L).createdByUserId(USER_ID).build();
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(USER_ID, PageRequest.of(0, PAGE_SIZE))).thenReturn(List.of(existing, secondByUser));
            when(usernameResolver.resolve(Set.of(USER_ID))).thenReturn(Map.of(USER_ID, USERNAME));
            when(sharedRevenueMapper.mapToDto(existing, USERNAME)).thenReturn(dto);
            when(sharedRevenueMapper.mapToDto(secondByUser, USERNAME)).thenReturn(otherDto);

            List<SharedRevenueDto> result = service.getRevenue(USER_ID, 0);

            assertEquals(List.of(dto, otherDto), result);
            verify(usernameResolver).resolve(Set.of(USER_ID));
        }
    }

    @Nested
    class DeleteRevenue {

        @Test
        void shouldDeleteRevenueWhenRevenueExists() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, USER_ID)).thenReturn(Optional.of(existing));
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
            when(participants.ownerId()).thenReturn(OWNER_ID);
            when(participants.memberId()).thenReturn(MEMBER_ID);

            service.deleteRevenue(REVENUE_ID, USER_ID);

            verify(sharedRevenueRepository).delete(existing);
        }

        @Test
        void shouldLockWalletAndRemoveRevenueAmountWhenRevenueExists() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, USER_ID)).thenReturn(Optional.of(existing));
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
            when(participants.ownerId()).thenReturn(OWNER_ID);
            when(participants.memberId()).thenReturn(MEMBER_ID);

            service.deleteRevenue(REVENUE_ID, USER_ID);

            verify(sharedWalletService).lockWalletForUpdate(USER_ID);
            verify(sharedWalletService).removeBalanceFromWallet(USER_ID, EXISTING_AMOUNT);
        }

        @Test
        void shouldLogActivityWhenRevenueIsDeleted() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, USER_ID)).thenReturn(Optional.of(existing));
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
            when(participants.ownerId()).thenReturn(OWNER_ID);
            when(participants.memberId()).thenReturn(MEMBER_ID);

            service.deleteRevenue(REVENUE_ID, USER_ID);

            verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(REVENUE_ID.toString()), eq(OUTBOX_EVENT_TYPE),
                    any(SharedAccountActivityLogEvent.class));
        }

        @Test
        void shouldThrowExceptionWhenRevenueDoesNotExist() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.deleteRevenue(REVENUE_ID, USER_ID));

            verify(sharedWalletService).lockWalletForUpdate(USER_ID);
            verify(sharedWalletService, never()).removeBalanceFromWallet(any(Long.class), any(BigDecimal.class));
            verify(sharedRevenueRepository, never()).delete(any(SharedRevenue.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenWalletCannotCoverRevenueRemoval() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, USER_ID)).thenReturn(Optional.of(existing));
            doThrow(new InvalidInputException("Insufficient funds")).when(sharedWalletService).removeBalanceFromWallet(USER_ID, EXISTING_AMOUNT);

            assertThrows(InvalidInputException.class, () -> service.deleteRevenue(REVENUE_ID, USER_ID));

            verify(sharedRevenueRepository, never()).delete(any(SharedRevenue.class));
            verifyNoInteractions(outboxService);
        }
    }

    @Nested
    class GetSharedRevenueByUserIdOrThrow {

        @Test
        void shouldReturnRevenueWhenRevenueExists() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, USER_ID)).thenReturn(Optional.of(existing));

            SharedRevenue result = service.getSharedRevenueByUserIdOrThrow(USER_ID, REVENUE_ID);

            assertSame(existing, result);
        }

        @Test
        void shouldThrowExceptionWhenRevenueDoesNotExist() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.getSharedRevenueByUserIdOrThrow(USER_ID, REVENUE_ID));
        }
    }
}