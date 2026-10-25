package com.finovara.financeservice.sharedaccount.piggybank.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.piggybank.dto.SharedPiggyBankDto;
import com.finovara.financeservice.sharedaccount.piggybank.mapper.SharedPiggyBankMapper;
import com.finovara.financeservice.sharedaccount.piggybank.model.SharedPiggyBank;
import com.finovara.financeservice.sharedaccount.piggybank.repository.SharedPiggyBankRepository;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import com.finovara.financeservice.util.sharedaccount.piggybank.SharedPiggyBankManager;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedPiggyBankManagementServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PIGGY_BANK_ID = 10L;
    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final int MAX_PIGGY_BANKS = 3;
    private static final String NAME = "Vacation";
    private static final String OUTBOX_AGGREGATE = "SharedAccountPiggyBank";
    private static final String OUTBOX_TOPIC = "shared-account.activity";
    private static final BigDecimal GOAL_AMOUNT = BigDecimal.valueOf(1000);

    @Mock
    private SharedPiggyBankRepository sharedPiggyBankRepository;

    @Mock
    private SharedPiggyBankManager sharedPiggyBankManager;

    @Mock
    private SharedPiggyBankMapper sharedPiggyBankMapper;

    @Mock
    private SharedAccountParticipantsService sharedAccountParticipantsService;

    @Mock
    private SharedWalletService sharedWalletService;

    @Mock
    private OutboxService outboxService;

    @Mock
    private SharedAccountParticipantsResponse participants;

    @Mock
    private SharedPiggyBankDto dto;

    @Mock
    private SharedPiggyBankDto mappedDto;

    @Mock
    private SharedPiggyBankDto otherMappedDto;

    @Captor
    private ArgumentCaptor<SharedPiggyBank> piggyBankCaptor;

    @InjectMocks
    private SharedPiggyBankManagementService service;

    private SharedPiggyBank existingPiggyBank;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maxPiggyBanks", MAX_PIGGY_BANKS);
        existingPiggyBank = SharedPiggyBank.builder()
                .id(PIGGY_BANK_ID)
                .name("Old name")
                .amount(BigDecimal.ZERO)
                .goalAmount(BigDecimal.valueOf(500))
                .ownerId(OWNER_ID)
                .memberId(MEMBER_ID)
                .build();
    }

    @Nested
    class AddPiggyBank {

        @Nested
        class Persisted {

            private SharedPiggyBank savedPiggyBank;

            @BeforeEach
            void setUp() {
                when(dto.name()).thenReturn(NAME);
                when(dto.goalAmount()).thenReturn(GOAL_AMOUNT);
                when(participants.ownerId()).thenReturn(OWNER_ID);
                when(participants.memberId()).thenReturn(MEMBER_ID);
                when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
                savedPiggyBank = SharedPiggyBank.builder().id(PIGGY_BANK_ID).name(NAME).amount(BigDecimal.ZERO).build();
                when(sharedPiggyBankRepository.save(any(SharedPiggyBank.class))).thenReturn(savedPiggyBank);
            }

            @Test
            void shouldReturnSavedIdWhenPiggyBankAdded() {
                when(sharedPiggyBankRepository.findLowerCaseNamesByUserId(USER_ID)).thenReturn(List.of());

                Long result = service.addPiggyBank(dto, USER_ID);

                assertEquals(PIGGY_BANK_ID, result);
            }

            @Test
            void shouldSavePiggyBankWithZeroBalanceAndParticipantsWhenPiggyBankAdded() {
                when(sharedPiggyBankRepository.findLowerCaseNamesByUserId(USER_ID)).thenReturn(List.of());

                service.addPiggyBank(dto, USER_ID);

                verify(sharedPiggyBankRepository).save(piggyBankCaptor.capture());
                assertEquals(NAME, piggyBankCaptor.getValue().getName());
                assertEquals(BigDecimal.ZERO, piggyBankCaptor.getValue().getAmount());
                assertEquals(GOAL_AMOUNT, piggyBankCaptor.getValue().getGoalAmount());
                assertEquals(OWNER_ID, piggyBankCaptor.getValue().getOwnerId());
                assertEquals(MEMBER_ID, piggyBankCaptor.getValue().getMemberId());
                assertEquals(LocalDate.now(), piggyBankCaptor.getValue().getCreatedAt());
            }

            @Test
            void shouldLockWalletAndLogActivityWhenPiggyBankAdded() {
                when(sharedPiggyBankRepository.findLowerCaseNamesByUserId(USER_ID)).thenReturn(List.of());

                service.addPiggyBank(dto, USER_ID);

                InOrder inOrder = inOrder(sharedWalletService, sharedPiggyBankRepository, outboxService);
                inOrder.verify(sharedWalletService).lockWalletForUpdate(USER_ID);
                inOrder.verify(sharedPiggyBankRepository).save(any(SharedPiggyBank.class));
                inOrder.verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(PIGGY_BANK_ID.toString()), eq(OUTBOX_TOPIC), any(SharedAccountActivityLogEvent.class));
            }

            @Test
            void shouldAddPiggyBankWhenCountIsOneBelowMaximum() {
                when(sharedPiggyBankRepository.findLowerCaseNamesByUserId(USER_ID)).thenReturn(List.of("first", "second"));

                Long result = service.addPiggyBank(dto, USER_ID);

                assertEquals(PIGGY_BANK_ID, result);
            }

            @Test
            void shouldAddPiggyBankWhenOtherNamesDifferFromNewName() {
                when(sharedPiggyBankRepository.findLowerCaseNamesByUserId(USER_ID)).thenReturn(List.of("holiday"));

                Long result = service.addPiggyBank(dto, USER_ID);

                assertEquals(PIGGY_BANK_ID, result);
                verify(sharedPiggyBankRepository).save(any(SharedPiggyBank.class));
            }
        }

        @Test
        void shouldThrowExceptionWhenMaximumNumberOfPiggyBanksReached() {
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
            when(sharedPiggyBankRepository.findLowerCaseNamesByUserId(USER_ID)).thenReturn(List.of("a", "b", "c"));

            assertThrows(InvalidInputException.class, () -> service.addPiggyBank(dto, USER_ID));
            verify(sharedPiggyBankRepository, never()).save(any(SharedPiggyBank.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenNameAlreadyExistsIgnoringCase() {
            when(dto.name()).thenReturn("VACATION");
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
            when(sharedPiggyBankRepository.findLowerCaseNamesByUserId(USER_ID)).thenReturn(List.of("vacation"));

            assertThrows(EntityAlreadyExistsException.class, () -> service.addPiggyBank(dto, USER_ID));
            verify(sharedPiggyBankRepository, never()).save(any(SharedPiggyBank.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenGoalAmountIsInvalid() {
            when(dto.name()).thenReturn(NAME);
            when(dto.goalAmount()).thenReturn(BigDecimal.valueOf(-1));
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
            when(sharedPiggyBankRepository.findLowerCaseNamesByUserId(USER_ID)).thenReturn(List.of());

            assertThrows(InvalidInputException.class, () -> service.addPiggyBank(dto, USER_ID));
            verify(sharedPiggyBankRepository, never()).save(any(SharedPiggyBank.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenParticipantsNotFound() {
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> service.addPiggyBank(dto, USER_ID));
            verifyNoInteractions(sharedWalletService, sharedPiggyBankRepository, outboxService);
        }
    }

    @Nested
    class EditPiggyBank {

        @Test
        void shouldUpdatePiggyBankAndReturnIdWhenDataIsValid() {
            when(dto.name()).thenReturn(NAME);
            when(dto.goalAmount()).thenReturn(GOAL_AMOUNT);
            when(participants.ownerId()).thenReturn(OWNER_ID);
            when(participants.memberId()).thenReturn(MEMBER_ID);
            when(sharedPiggyBankManager.getSharedPiggyBankForUpdateByIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(existingPiggyBank);
            when(sharedPiggyBankRepository.existsByNameIgnoreCaseExcludingId(USER_ID, NAME, PIGGY_BANK_ID)).thenReturn(false);
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);

            Long result = service.editPiggyBank(USER_ID, dto, PIGGY_BANK_ID);

            assertEquals(PIGGY_BANK_ID, result);
            assertEquals(NAME, existingPiggyBank.getName());
            assertEquals(GOAL_AMOUNT, existingPiggyBank.getGoalAmount());
            verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(PIGGY_BANK_ID.toString()), eq(OUTBOX_TOPIC), any(SharedAccountActivityLogEvent.class));
        }

        @Test
        void shouldThrowExceptionWhenNameAlreadyUsedByAnotherPiggyBank() {
            when(dto.name()).thenReturn(NAME);
            when(sharedPiggyBankManager.getSharedPiggyBankForUpdateByIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(existingPiggyBank);
            when(sharedPiggyBankRepository.existsByNameIgnoreCaseExcludingId(USER_ID, NAME, PIGGY_BANK_ID)).thenReturn(true);

            assertThrows(EntityAlreadyExistsException.class, () -> service.editPiggyBank(USER_ID, dto, PIGGY_BANK_ID));
            assertEquals("Old name", existingPiggyBank.getName());
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenGoalAmountIsInvalid() {
            when(dto.name()).thenReturn(NAME);
            when(dto.goalAmount()).thenReturn(BigDecimal.valueOf(-1));
            when(sharedPiggyBankManager.getSharedPiggyBankForUpdateByIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(existingPiggyBank);
            when(sharedPiggyBankRepository.existsByNameIgnoreCaseExcludingId(USER_ID, NAME, PIGGY_BANK_ID)).thenReturn(false);

            assertThrows(InvalidInputException.class, () -> service.editPiggyBank(USER_ID, dto, PIGGY_BANK_ID));
            assertEquals("Old name", existingPiggyBank.getName());
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankNotFound() {
            when(sharedPiggyBankManager.getSharedPiggyBankForUpdateByIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenThrow(new RequestedEntityNotFoundException("Piggy bank not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.editPiggyBank(USER_ID, dto, PIGGY_BANK_ID));
            verifyNoInteractions(sharedPiggyBankRepository, sharedAccountParticipantsService, outboxService);
        }
    }

    @Nested
    class GetAllPiggyBanks {

        @Test
        void shouldReturnMappedDtosWhenUserHasPiggyBanks() {
            SharedPiggyBank otherPiggyBank = SharedPiggyBank.builder().id(11L).name("Car").amount(BigDecimal.valueOf(50)).goalAmount(BigDecimal.valueOf(100)).build();
            existingPiggyBank.setAmount(BigDecimal.valueOf(20));
            when(sharedPiggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of(existingPiggyBank, otherPiggyBank));
            when(sharedPiggyBankMapper.mapToPiggyBankDto(eq(existingPiggyBank), anyDouble())).thenReturn(mappedDto);
            when(sharedPiggyBankMapper.mapToPiggyBankDto(eq(otherPiggyBank), anyDouble())).thenReturn(otherMappedDto);

            List<SharedPiggyBankDto> result = service.getAllPiggyBanks(USER_ID);

            assertEquals(2, result.size());
            assertSame(mappedDto, result.get(0));
            assertSame(otherMappedDto, result.get(1));
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoPiggyBanks() {
            when(sharedPiggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

            List<SharedPiggyBankDto> result = service.getAllPiggyBanks(USER_ID);

            assertTrue(result.isEmpty());
            verifyNoInteractions(sharedPiggyBankMapper);
        }
    }

    @Nested
    class DeletePiggyBank {

        @Test
        void shouldDeletePiggyBankAndLogActivityWhenBalanceIsZero() {
            when(participants.ownerId()).thenReturn(OWNER_ID);
            when(participants.memberId()).thenReturn(MEMBER_ID);
            when(sharedPiggyBankManager.getSharedPiggyBankForUpdateByIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(existingPiggyBank);
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);

            service.deletePiggyBank(USER_ID, PIGGY_BANK_ID);

            verify(sharedPiggyBankRepository).delete(existingPiggyBank);
            verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(PIGGY_BANK_ID.toString()), eq(OUTBOX_TOPIC), any(SharedAccountActivityLogEvent.class));
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankHasBalance() {
            existingPiggyBank.setAmount(BigDecimal.valueOf(0.01));
            when(sharedPiggyBankManager.getSharedPiggyBankForUpdateByIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(existingPiggyBank);

            assertThrows(InvalidInputException.class, () -> service.deletePiggyBank(USER_ID, PIGGY_BANK_ID));
            verify(sharedPiggyBankRepository, never()).delete(any(SharedPiggyBank.class));
            verifyNoInteractions(outboxService, sharedAccountParticipantsService);
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankNotFound() {
            when(sharedPiggyBankManager.getSharedPiggyBankForUpdateByIdAndUserId(PIGGY_BANK_ID, USER_ID)).thenThrow(new RequestedEntityNotFoundException("Piggy bank not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.deletePiggyBank(USER_ID, PIGGY_BANK_ID));
            verifyNoInteractions(sharedPiggyBankRepository, outboxService, sharedAccountParticipantsService);
        }
    }
}