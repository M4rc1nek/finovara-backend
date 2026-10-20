package com.finovara.financeservice.sharedaccount.revenue.service;

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
import com.finovara.financeservice.sharedaccount.revenue.repository.SharedRevenueRepository;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    private AuthBackendClient authBackendClient;

    @Mock
    private OutboxService outboxService;

    @Mock
    private SharedRevenueDto revenueDto;

    @Mock
    private SharedRevenueDto firstMappedDto;

    @Mock
    private SharedRevenueDto secondMappedDto;

    @Mock
    private SharedAccountParticipantsResponse participantsResponse;

    @Mock
    private SharedRevenue savedRevenue;

    @Mock
    private SharedRevenue existingRevenue;

    @Mock
    private SharedRevenue anotherRevenue;

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
            when(sharedAccountParticipantsService.getParticipants(OWNER_ID)).thenThrow(new RequestedEntityNotFoundException("participants not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.addSharedRevenue(revenueDto, OWNER_ID));

            verifyNoInteractions(authBackendClient, sharedWalletService, sharedRevenueRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenAuthBackendClientFails() {
            when(sharedAccountParticipantsService.getParticipants(OWNER_ID)).thenReturn(participantsResponse);
            when(authBackendClient.getUsername(OWNER_ID)).thenThrow(new IllegalStateException("auth down"));

            assertThrows(IllegalStateException.class, () -> service.addSharedRevenue(revenueDto, OWNER_ID));

            verifyNoInteractions(sharedWalletService, sharedRevenueRepository, outboxService);
        }

        @Test
        void shouldAddRevenueWithMemberAsCreatorWhenMemberAddsRevenue() {
            stubParticipants(MEMBER_ID);
            when(authBackendClient.getUsername(MEMBER_ID)).thenReturn(OTHER_USERNAME);
            when(revenueDto.amount()).thenReturn(AMOUNT);
            when(revenueDto.description()).thenReturn(DESCRIPTION);
            when(sharedRevenueRepository.save(any(SharedRevenue.class))).thenReturn(savedRevenue);
            when(savedRevenue.getId()).thenReturn(REVENUE_ID);

            SharedRevenueResponse result = service.addSharedRevenue(revenueDto, MEMBER_ID);

            verify(sharedRevenueRepository).save(revenueCaptor.capture());
            assertEquals(MEMBER_ID, revenueCaptor.getValue().getCreatedByUserId());
            assertEquals(new SharedRevenueResponse(revenueCaptor.getValue().getId(), MEMBER_ID, OTHER_USERNAME), result);
            verify(sharedWalletService).addBalanceToWallet(MEMBER_ID, AMOUNT);
            verifyOutboxEvent(MEMBER_ID, SharedAccountActivityLogType.REVENUE_CREATED);
        }

        @Nested
        class WithValidInput {

            @BeforeEach
            void setUp() {
                stubParticipants(OWNER_ID);
                when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
                when(revenueDto.amount()).thenReturn(AMOUNT);
                when(revenueDto.description()).thenReturn(DESCRIPTION);
            }

            @Test
            void shouldThrowExceptionWhenWalletUpdateFails() {
                doThrow(new IllegalStateException("wallet failed")).when(sharedWalletService).addBalanceToWallet(OWNER_ID, AMOUNT);

                assertThrows(IllegalStateException.class, () -> service.addSharedRevenue(revenueDto, OWNER_ID));

                verifyNoInteractions(sharedRevenueRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenRepositorySaveFails() {
                when(sharedRevenueRepository.save(any(SharedRevenue.class))).thenThrow(new IllegalStateException("db failed"));

                assertThrows(IllegalStateException.class, () -> service.addSharedRevenue(revenueDto, OWNER_ID));

                verifyNoInteractions(outboxService);
            }

            @Nested
            class WhenRevenueIsSaved {

                @BeforeEach
                void setUp() {
                    when(sharedRevenueRepository.save(any(SharedRevenue.class))).thenReturn(savedRevenue);
                    when(savedRevenue.getId()).thenReturn(REVENUE_ID);
                }

                @Test
                void shouldReturnResponseWithCreatorDataWhenRevenueIsAdded() {
                    SharedRevenueResponse result = service.addSharedRevenue(revenueDto, OWNER_ID);

                    verify(sharedRevenueRepository).save(revenueCaptor.capture());
                    assertEquals(new SharedRevenueResponse(revenueCaptor.getValue().getId(), OWNER_ID, USERNAME), result);
                }

                @Test
                void shouldSaveRevenueWithDtoAndParticipantDataWhenRevenueIsAdded() {
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
                void shouldAddAmountToWalletWhenRevenueIsAdded() {
                    service.addSharedRevenue(revenueDto, OWNER_ID);

                    verify(sharedWalletService).addBalanceToWallet(OWNER_ID, AMOUNT);
                }

                @Test
                void shouldSaveOutboxEventWhenRevenueIsAdded() {
                    service.addSharedRevenue(revenueDto, OWNER_ID);

                    verifyOutboxEvent(OWNER_ID, SharedAccountActivityLogType.REVENUE_CREATED);
                }

                @Test
                void shouldUpdateWalletBeforeSavingRevenueAndOutboxEventWhenRevenueIsAdded() {
                    service.addSharedRevenue(revenueDto, OWNER_ID);

                    InOrder order = inOrder(sharedWalletService, sharedRevenueRepository, outboxService);

                    order.verify(sharedWalletService).addBalanceToWallet(OWNER_ID, AMOUNT);
                    order.verify(sharedRevenueRepository).save(any(SharedRevenue.class));
                    order.verify(outboxService).save(anyString(), anyString(), anyString(), any());
                }

                @Test
                void shouldThrowExceptionWhenOutboxServiceFails() {
                    doThrow(new IllegalStateException("outbox failed")).when(outboxService).save(anyString(), anyString(), anyString(), any());

                    assertThrows(IllegalStateException.class, () -> service.addSharedRevenue(revenueDto, OWNER_ID));
                }
            }
        }
    }

    @Nested
    class EditRevenue {

        @Test
        void shouldThrowExceptionWhenRevenueDoesNotExist() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, OWNER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.editRevenue(revenueDto, REVENUE_ID, OWNER_ID));

            verifyNoInteractions(sharedWalletService, sharedAccountParticipantsService, outboxService);
            verify(sharedRevenueRepository, never()).save(any(SharedRevenue.class));
        }

        @Test
        void shouldThrowExceptionWhenUserIsNeitherOwnerNorMember() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, OUTSIDER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.editRevenue(revenueDto, REVENUE_ID, OUTSIDER_ID));

            verifyNoInteractions(sharedWalletService, sharedAccountParticipantsService, outboxService);
            verify(sharedRevenueRepository, never()).save(any(SharedRevenue.class));
        }

        @Test
        void shouldThrowExceptionWhenDtoIsNull() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, OWNER_ID)).thenReturn(Optional.of(existingRevenue));

            assertThrows(NullPointerException.class, () -> service.editRevenue(null, REVENUE_ID, OWNER_ID));

            verifyNoInteractions(sharedWalletService, sharedAccountParticipantsService, outboxService);
            verify(sharedRevenueRepository, never()).save(any(SharedRevenue.class));
        }

        @Test
        void shouldEditRevenueWhenMemberEditsRevenue() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, MEMBER_ID)).thenReturn(Optional.of(existingRevenue));
            when(revenueDto.amount()).thenReturn(AMOUNT);
            when(existingRevenue.getAmount()).thenReturn(OLD_AMOUNT);
            when(sharedRevenueRepository.save(existingRevenue)).thenReturn(existingRevenue);
            when(existingRevenue.getId()).thenReturn(REVENUE_ID);
            stubParticipants(MEMBER_ID);

            Long result = service.editRevenue(revenueDto, REVENUE_ID, MEMBER_ID);

            assertEquals(REVENUE_ID, result);
            verify(sharedWalletService).addBalanceToWallet(MEMBER_ID, AMOUNT);
            verify(sharedWalletService).removeBalanceFromWallet(MEMBER_ID, OLD_AMOUNT);
            verifyOutboxEvent(MEMBER_ID, SharedAccountActivityLogType.REVENUE_EDITED);
        }

        @Nested
        class WhenRevenueExists {

            @BeforeEach
            void setUp() {
                when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, OWNER_ID)).thenReturn(Optional.of(existingRevenue));
                when(revenueDto.amount()).thenReturn(AMOUNT);
            }

            @Test
            void shouldThrowExceptionWhenAddingBalanceToWalletFails() {
                doThrow(new IllegalStateException("wallet failed")).when(sharedWalletService).addBalanceToWallet(OWNER_ID, AMOUNT);

                assertThrows(IllegalStateException.class, () -> service.editRevenue(revenueDto, REVENUE_ID, OWNER_ID));

                verify(sharedWalletService, never()).removeBalanceFromWallet(any(Long.class), any(BigDecimal.class));
                verify(sharedRevenueRepository, never()).save(any(SharedRevenue.class));
                verifyNoInteractions(sharedAccountParticipantsService, outboxService);
            }

            @Nested
            class WhenOldAmountIsAvailable {

                @BeforeEach
                void setUp() {
                    when(existingRevenue.getAmount()).thenReturn(OLD_AMOUNT);
                }

                @Test
                void shouldThrowExceptionWhenRemovingBalanceFromWalletFails() {
                    doThrow(new IllegalStateException("wallet failed")).when(sharedWalletService).removeBalanceFromWallet(OWNER_ID, OLD_AMOUNT);

                    assertThrows(IllegalStateException.class, () -> service.editRevenue(revenueDto, REVENUE_ID, OWNER_ID));

                    verify(existingRevenue, never()).setAmount(any(BigDecimal.class));
                    verify(sharedRevenueRepository, never()).save(any(SharedRevenue.class));
                    verifyNoInteractions(sharedAccountParticipantsService, outboxService);
                }

                @Test
                void shouldThrowExceptionWhenRepositorySaveFails() {
                    when(sharedRevenueRepository.save(existingRevenue)).thenThrow(new IllegalStateException("db failed"));

                    assertThrows(IllegalStateException.class, () -> service.editRevenue(revenueDto, REVENUE_ID, OWNER_ID));

                    verifyNoInteractions(sharedAccountParticipantsService, outboxService);
                }

                @Nested
                class WhenRevenueIsSaved {

                    @BeforeEach
                    void setUp() {
                        when(sharedRevenueRepository.save(existingRevenue)).thenReturn(existingRevenue);
                        when(revenueDto.description()).thenReturn(DESCRIPTION);
                    }

                    @Test
                    void shouldThrowExceptionWhenParticipantsServiceFails() {
                        when(sharedAccountParticipantsService.getParticipants(OWNER_ID)).thenThrow(new RequestedEntityNotFoundException("participants not found"));

                        assertThrows(RequestedEntityNotFoundException.class, () -> service.editRevenue(revenueDto, REVENUE_ID, OWNER_ID));

                        verifyNoInteractions(outboxService);
                    }

                    @Nested
                    class WhenParticipantsAreAvailable {

                        @BeforeEach
                        void setUp() {
                            stubParticipants(OWNER_ID);
                            when(existingRevenue.getId()).thenReturn(REVENUE_ID);
                        }

                        @Test
                        void shouldReturnRevenueIdWhenOwnerEditsRevenue() {
                            Long result = service.editRevenue(revenueDto, REVENUE_ID, OWNER_ID);

                            assertEquals(REVENUE_ID, result);
                        }

                        @Test
                        void shouldUpdateRevenueFieldsWhenRevenueIsEdited() {
                            service.editRevenue(revenueDto, REVENUE_ID, OWNER_ID);

                            verify(existingRevenue).setAmount(AMOUNT);
                            verify(existingRevenue).setCategory(any());
                            verify(existingRevenue).setDescription(DESCRIPTION);
                            verify(sharedRevenueRepository).save(existingRevenue);
                        }

                        @Test
                        void shouldAddNewAmountAndRemoveOldAmountFromWalletWhenRevenueIsEdited() {
                            service.editRevenue(revenueDto, REVENUE_ID, OWNER_ID);

                            InOrder order = inOrder(sharedWalletService);

                            order.verify(sharedWalletService).addBalanceToWallet(OWNER_ID, AMOUNT);
                            order.verify(sharedWalletService).removeBalanceFromWallet(OWNER_ID, OLD_AMOUNT);
                        }

                        @Test
                        void shouldSaveOutboxEventWhenRevenueIsEdited() {
                            service.editRevenue(revenueDto, REVENUE_ID, OWNER_ID);

                            verifyOutboxEvent(OWNER_ID, SharedAccountActivityLogType.REVENUE_EDITED);
                        }

                        @Test
                        void shouldUpdateWalletBeforeSavingRevenueAndOutboxEventWhenRevenueIsEdited() {
                            service.editRevenue(revenueDto, REVENUE_ID, OWNER_ID);

                            InOrder order = inOrder(sharedWalletService, sharedRevenueRepository, outboxService);

                            order.verify(sharedWalletService).removeBalanceFromWallet(OWNER_ID, OLD_AMOUNT);
                            order.verify(sharedRevenueRepository).save(existingRevenue);
                            order.verify(outboxService).save(anyString(), anyString(), anyString(), any());
                        }

                        @Test
                        void shouldThrowExceptionWhenOutboxServiceFails() {
                            doThrow(new IllegalStateException("outbox failed")).when(outboxService).save(anyString(), anyString(), anyString(), any());

                            assertThrows(IllegalStateException.class, () -> service.editRevenue(revenueDto, REVENUE_ID, OWNER_ID));
                        }
                    }
                }
            }
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
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(OWNER_ID)).thenReturn(List.of(existingRevenue));
            when(existingRevenue.getCreatedByUserId()).thenReturn(OWNER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
            when(sharedRevenueMapper.mapToDto(existingRevenue, USERNAME)).thenReturn(firstMappedDto);

            List<SharedRevenueDto> result = service.getRevenue(OWNER_ID);

            assertEquals(List.of(firstMappedDto), result);
        }

        @Test
        void shouldMapEachRevenueWithItsCreatorUsernameWhenCreatorsDiffer() {
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(OWNER_ID)).thenReturn(List.of(existingRevenue, anotherRevenue));
            when(existingRevenue.getCreatedByUserId()).thenReturn(OWNER_ID);
            when(anotherRevenue.getCreatedByUserId()).thenReturn(MEMBER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
            when(authBackendClient.getUsername(MEMBER_ID)).thenReturn(OTHER_USERNAME);
            when(sharedRevenueMapper.mapToDto(existingRevenue, USERNAME)).thenReturn(firstMappedDto);
            when(sharedRevenueMapper.mapToDto(anotherRevenue, OTHER_USERNAME)).thenReturn(secondMappedDto);

            List<SharedRevenueDto> result = service.getRevenue(OWNER_ID);

            assertEquals(List.of(firstMappedDto, secondMappedDto), result);
        }

        @Test
        void shouldFetchUsernameOnceWhenRevenuesShareSameCreator() {
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(OWNER_ID)).thenReturn(List.of(existingRevenue, anotherRevenue));
            when(existingRevenue.getCreatedByUserId()).thenReturn(OWNER_ID);
            when(anotherRevenue.getCreatedByUserId()).thenReturn(OWNER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
            when(sharedRevenueMapper.mapToDto(existingRevenue, USERNAME)).thenReturn(firstMappedDto);
            when(sharedRevenueMapper.mapToDto(anotherRevenue, USERNAME)).thenReturn(secondMappedDto);

            List<SharedRevenueDto> result = service.getRevenue(OWNER_ID);

            assertEquals(List.of(firstMappedDto, secondMappedDto), result);
            verify(authBackendClient).getUsername(OWNER_ID);
        }

        @Test
        void shouldThrowExceptionWhenUsernameIsNull() {
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(OWNER_ID)).thenReturn(List.of(existingRevenue));
            when(existingRevenue.getCreatedByUserId()).thenReturn(OWNER_ID);

            assertThrows(NullPointerException.class, () -> service.getRevenue(OWNER_ID));

            verify(authBackendClient).getUsername(OWNER_ID);
            verifyNoInteractions(sharedRevenueMapper);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(OWNER_ID)).thenThrow(new IllegalStateException("db failed"));

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

        @Test
        void shouldThrowExceptionWhenRevenueDoesNotExist() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, OWNER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.deleteRevenue(REVENUE_ID, OWNER_ID));

            verify(sharedRevenueRepository, never()).delete(any(SharedRevenue.class));
            verifyNoInteractions(sharedWalletService, sharedAccountParticipantsService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenUserIsNeitherOwnerNorMember() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, OUTSIDER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.deleteRevenue(REVENUE_ID, OUTSIDER_ID));

            verify(sharedRevenueRepository, never()).delete(any(SharedRevenue.class));
            verifyNoInteractions(sharedWalletService, sharedAccountParticipantsService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryLookupFails() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, OWNER_ID)).thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> service.deleteRevenue(REVENUE_ID, OWNER_ID));

            verifyNoInteractions(sharedWalletService, sharedAccountParticipantsService, outboxService);
        }

        @Nested
        class WhenRevenueExists {

            @BeforeEach
            void setUp() {
                when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, OWNER_ID)).thenReturn(Optional.of(existingRevenue));
                when(existingRevenue.getAmount()).thenReturn(OLD_AMOUNT);
            }

            @Test
            void shouldThrowExceptionWhenWalletUpdateFails() {
                doThrow(new IllegalStateException("wallet failed")).when(sharedWalletService).removeBalanceFromWallet(OWNER_ID, OLD_AMOUNT);

                assertThrows(IllegalStateException.class, () -> service.deleteRevenue(REVENUE_ID, OWNER_ID));

                verify(sharedRevenueRepository, never()).delete(any(SharedRevenue.class));
                verifyNoInteractions(sharedAccountParticipantsService, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenParticipantsServiceFails() {
                when(sharedAccountParticipantsService.getParticipants(OWNER_ID)).thenThrow(new RequestedEntityNotFoundException("participants not found"));

                assertThrows(RequestedEntityNotFoundException.class, () -> service.deleteRevenue(REVENUE_ID, OWNER_ID));

                verify(sharedRevenueRepository, never()).delete(any(SharedRevenue.class));
                verifyNoInteractions(outboxService);
            }

            @Nested
            class WhenParticipantsAreAvailable {

                @BeforeEach
                void setUp() {
                    stubParticipants(OWNER_ID);
                }


                @Nested
                class WhenRevenueIsDeleted {

                    @BeforeEach
                    void setUp() {
                        when(existingRevenue.getId()).thenReturn(REVENUE_ID);
                    }

                    @Test
                    void shouldDeleteRevenueWhenRevenueExists() {
                        service.deleteRevenue(REVENUE_ID, OWNER_ID);

                        verify(sharedRevenueRepository).delete(existingRevenue);
                    }

                    @Test
                    void shouldRemoveRevenueAmountFromWalletWhenRevenueIsDeleted() {
                        service.deleteRevenue(REVENUE_ID, OWNER_ID);

                        verify(sharedWalletService).removeBalanceFromWallet(OWNER_ID, OLD_AMOUNT);
                    }

                    @Test
                    void shouldSaveOutboxEventWhenRevenueIsDeleted() {
                        service.deleteRevenue(REVENUE_ID, OWNER_ID);

                        verifyOutboxEvent(OWNER_ID, SharedAccountActivityLogType.REVENUE_DELETED);
                    }

                    @Test
                    void shouldUpdateWalletBeforeDeletingAndSavingEventWhenRevenueIsDeleted() {
                        service.deleteRevenue(REVENUE_ID, OWNER_ID);

                        InOrder order = inOrder(sharedWalletService, sharedRevenueRepository, outboxService);

                        order.verify(sharedWalletService).removeBalanceFromWallet(OWNER_ID, OLD_AMOUNT);
                        order.verify(sharedRevenueRepository).delete(existingRevenue);
                        order.verify(outboxService).save(anyString(), anyString(), anyString(), any());
                    }

                    @Test
                    void shouldThrowExceptionWhenOutboxServiceFails() {
                        doThrow(new IllegalStateException("outbox failed")).when(outboxService).save(anyString(), anyString(), anyString(), any());

                        assertThrows(IllegalStateException.class, () -> service.deleteRevenue(REVENUE_ID, OWNER_ID));
                    }
                }
            }
        }
    }

    @Nested
    class GetSharedRevenueByUserIdOrThrow {

        @Test
        void shouldReturnRevenueWhenRevenueExists() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, OWNER_ID)).thenReturn(Optional.of(existingRevenue));

            SharedRevenue result = service.getSharedRevenueByUserIdOrThrow(OWNER_ID, REVENUE_ID);

            assertEquals(existingRevenue, result);
        }

        @Test
        void shouldThrowExceptionWhenRevenueDoesNotExist() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, OUTSIDER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.getSharedRevenueByUserIdOrThrow(OUTSIDER_ID, REVENUE_ID));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(REVENUE_ID, OWNER_ID)).thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> service.getSharedRevenueByUserIdOrThrow(OWNER_ID, REVENUE_ID));
        }
    }
}