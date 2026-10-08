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
import com.finovara.financeservice.util.transaction.piggybank.manager.SharedPiggyBankManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedPiggyBankManagementServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PIGGY_BANK_ID = 10L;
    private static final Long OWNER_ID = 100L;
    private static final Long MEMBER_ID = 200L;
    private static final String PIGGY_BANK_NAME = "Vacation Fund";

    @Mock
    private SharedPiggyBankRepository sharedPiggyBankRepository;

    @Mock
    private SharedPiggyBankManager sharedPiggyBankManager;

    @Mock
    private SharedPiggyBankMapper sharedPiggyBankMapper;

    @Mock
    private SharedAccountParticipantsService sharedAccountParticipantsService;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private SharedPiggyBankManagementService sharedPiggyBankManagementService;

    private SharedPiggyBankDto sharedPiggyBankDto;
    private SharedAccountParticipantsResponse participantsResponse;

    @BeforeEach
    void setUp() {
        sharedPiggyBankDto = mock(SharedPiggyBankDto.class);
        participantsResponse = mock(SharedAccountParticipantsResponse.class);
    }

    @Nested
    class AddPiggyBank {

        @Test
        void shouldAddPiggyBankWhenInputValidAndUnderLimit() {
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);
            when(sharedPiggyBankRepository.countPiggyBanksByUserId(USER_ID)).thenReturn(1L);
            when(sharedPiggyBankRepository.existsByNameIgnoreCase(USER_ID, PIGGY_BANK_NAME)).thenReturn(false);
            when(sharedPiggyBankDto.name()).thenReturn(PIGGY_BANK_NAME);
            when(sharedPiggyBankDto.goalAmount()).thenReturn(new BigDecimal("1000.00"));
            when(sharedPiggyBankRepository.save(any(SharedPiggyBank.class))).thenReturn(SharedPiggyBank.builder().id(PIGGY_BANK_ID).build());

            Long result = sharedPiggyBankManagementService.addPiggyBank(sharedPiggyBankDto, USER_ID);

            assertThat(result).isEqualTo(PIGGY_BANK_ID);
        }

        @Test
        void shouldThrowExceptionWhenMaxPiggyBanksReached() {
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);
            when(sharedPiggyBankRepository.countPiggyBanksByUserId(USER_ID)).thenReturn(5L);

            assertThrows(InvalidInputException.class, () -> sharedPiggyBankManagementService.addPiggyBank(sharedPiggyBankDto, USER_ID));

            verify(sharedPiggyBankRepository, never()).save(any(SharedPiggyBank.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenNameAlreadyExists() {
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);
            when(sharedPiggyBankRepository.countPiggyBanksByUserId(USER_ID)).thenReturn(1L);
            when(sharedPiggyBankRepository.existsByNameIgnoreCase(USER_ID, PIGGY_BANK_NAME)).thenReturn(true);
            when(sharedPiggyBankDto.name()).thenReturn(PIGGY_BANK_NAME);

            assertThrows(EntityAlreadyExistsException.class, () -> sharedPiggyBankManagementService.addPiggyBank(sharedPiggyBankDto, USER_ID));

            verify(sharedPiggyBankRepository, never()).save(any(SharedPiggyBank.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldSavePiggyBankWithZeroInitialAmountAndOwnerMemberIds() {
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);
            when(participantsResponse.ownerId()).thenReturn(OWNER_ID);
            when(participantsResponse.memberId()).thenReturn(MEMBER_ID);
            when(sharedPiggyBankRepository.countPiggyBanksByUserId(USER_ID)).thenReturn(1L);
            when(sharedPiggyBankRepository.existsByNameIgnoreCase(USER_ID, PIGGY_BANK_NAME)).thenReturn(false);
            when(sharedPiggyBankDto.name()).thenReturn(PIGGY_BANK_NAME);
            when(sharedPiggyBankDto.goalAmount()).thenReturn(new BigDecimal("1000.00"));
            when(sharedPiggyBankRepository.save(any(SharedPiggyBank.class))).thenReturn(SharedPiggyBank.builder().id(PIGGY_BANK_ID).build());

            sharedPiggyBankManagementService.addPiggyBank(sharedPiggyBankDto, USER_ID);

            ArgumentCaptor<SharedPiggyBank> captor = ArgumentCaptor.forClass(SharedPiggyBank.class);
            verify(sharedPiggyBankRepository).save(captor.capture());
            assertThat(captor.getValue().getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(captor.getValue().getOwnerId()).isEqualTo(OWNER_ID);
            assertThat(captor.getValue().getMemberId()).isEqualTo(MEMBER_ID);
        }

        @Test
        void shouldPublishOutboxEventWhenPiggyBankCreated() {
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);
            when(sharedPiggyBankRepository.countPiggyBanksByUserId(USER_ID)).thenReturn(1L);
            when(sharedPiggyBankRepository.existsByNameIgnoreCase(USER_ID, PIGGY_BANK_NAME)).thenReturn(false);
            when(sharedPiggyBankDto.name()).thenReturn(PIGGY_BANK_NAME);
            when(sharedPiggyBankDto.goalAmount()).thenReturn(new BigDecimal("1000.00"));
            when(sharedPiggyBankRepository.save(any(SharedPiggyBank.class))).thenReturn(SharedPiggyBank.builder().id(PIGGY_BANK_ID).build());

            sharedPiggyBankManagementService.addPiggyBank(sharedPiggyBankDto, USER_ID);

            verify(outboxService).save(eq("SharedAccountPiggyBank"), eq(PIGGY_BANK_ID.toString()), eq("shared-account.activity"), any(SharedAccountActivityLogEvent.class));
        }

        @Test
        void shouldCheckNameExistenceBeforeSavingPiggyBank() {
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);
            when(sharedPiggyBankRepository.countPiggyBanksByUserId(USER_ID)).thenReturn(1L);
            when(sharedPiggyBankRepository.existsByNameIgnoreCase(USER_ID, PIGGY_BANK_NAME)).thenReturn(false);
            when(sharedPiggyBankDto.name()).thenReturn(PIGGY_BANK_NAME);
            when(sharedPiggyBankDto.goalAmount()).thenReturn(new BigDecimal("1000.00"));
            when(sharedPiggyBankRepository.save(any(SharedPiggyBank.class))).thenReturn(SharedPiggyBank.builder().id(PIGGY_BANK_ID).build());

            sharedPiggyBankManagementService.addPiggyBank(sharedPiggyBankDto, USER_ID);

            verify(sharedPiggyBankRepository).existsByNameIgnoreCase(USER_ID, PIGGY_BANK_NAME);
        }

        @Test
        void shouldThrowExceptionWhenRepositorySaveFails() {
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);
            when(sharedPiggyBankRepository.countPiggyBanksByUserId(USER_ID)).thenReturn(1L);
            when(sharedPiggyBankRepository.existsByNameIgnoreCase(USER_ID, PIGGY_BANK_NAME)).thenReturn(false);
            when(sharedPiggyBankDto.name()).thenReturn(PIGGY_BANK_NAME);
            when(sharedPiggyBankDto.goalAmount()).thenReturn(new BigDecimal("1000.00"));
            when(sharedPiggyBankRepository.save(any(SharedPiggyBank.class))).thenThrow(new IllegalStateException("save failed"));

            assertThrows(IllegalStateException.class, () -> sharedPiggyBankManagementService.addPiggyBank(sharedPiggyBankDto, USER_ID));

            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenParticipantsServiceFails() {
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenThrow(new IllegalStateException("participants failed"));

            assertThrows(IllegalStateException.class, () -> sharedPiggyBankManagementService.addPiggyBank(sharedPiggyBankDto, USER_ID));

            verify(sharedPiggyBankRepository, never()).save(any(SharedPiggyBank.class));
        }
    }

    @Nested
    class EditPiggyBank {

        @Test
        void shouldEditPiggyBankWhenNewNameIsUnique() {
            SharedPiggyBank existingPiggyBank = SharedPiggyBank.builder().id(PIGGY_BANK_ID).name(PIGGY_BANK_NAME).build();
            when(sharedPiggyBankManager.getPiggyBankByUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(existingPiggyBank);
            when(sharedPiggyBankDto.name()).thenReturn("New Name");
            when(sharedPiggyBankDto.goalAmount()).thenReturn(new BigDecimal("2000.00"));
            when(sharedPiggyBankRepository.existsByNameIgnoreCase(USER_ID, "New Name")).thenReturn(false);
            when(sharedPiggyBankRepository.save(existingPiggyBank)).thenReturn(existingPiggyBank);
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);

            Long result = sharedPiggyBankManagementService.editPiggyBank(USER_ID, sharedPiggyBankDto, PIGGY_BANK_ID);

            assertThat(result).isEqualTo(PIGGY_BANK_ID);
        }

        @Test
        void shouldEditPiggyBankWhenNameUnchanged() {
            SharedPiggyBank existingPiggyBank = SharedPiggyBank.builder().id(PIGGY_BANK_ID).name(PIGGY_BANK_NAME).build();
            when(sharedPiggyBankManager.getPiggyBankByUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(existingPiggyBank);
            when(sharedPiggyBankDto.name()).thenReturn(PIGGY_BANK_NAME.toUpperCase());
            when(sharedPiggyBankDto.goalAmount()).thenReturn(new BigDecimal("2000.00"));
            when(sharedPiggyBankRepository.existsByNameIgnoreCase(USER_ID, PIGGY_BANK_NAME.toUpperCase())).thenReturn(true);
            when(sharedPiggyBankRepository.save(existingPiggyBank)).thenReturn(existingPiggyBank);
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);

            Long result = sharedPiggyBankManagementService.editPiggyBank(USER_ID, sharedPiggyBankDto, PIGGY_BANK_ID);

            assertThat(result).isEqualTo(PIGGY_BANK_ID);
        }

        @Test
        void shouldThrowExceptionWhenNewNameAlreadyExistsForDifferentPiggyBank() {
            SharedPiggyBank existingPiggyBank = SharedPiggyBank.builder().id(PIGGY_BANK_ID).name(PIGGY_BANK_NAME).build();
            when(sharedPiggyBankManager.getPiggyBankByUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(existingPiggyBank);
            when(sharedPiggyBankDto.name()).thenReturn("Taken Name");
            when(sharedPiggyBankRepository.existsByNameIgnoreCase(USER_ID, "Taken Name")).thenReturn(true);

            assertThrows(EntityAlreadyExistsException.class, () -> sharedPiggyBankManagementService.editPiggyBank(USER_ID, sharedPiggyBankDto, PIGGY_BANK_ID));

            verify(sharedPiggyBankRepository, never()).save(any(SharedPiggyBank.class));
        }

        @Test
        void shouldUpdatePiggyBankFieldsFromDto() {
            SharedPiggyBank existingPiggyBank = SharedPiggyBank.builder().id(PIGGY_BANK_ID).name(PIGGY_BANK_NAME).build();
            when(sharedPiggyBankManager.getPiggyBankByUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(existingPiggyBank);
            when(sharedPiggyBankDto.name()).thenReturn("Updated Name");
            when(sharedPiggyBankDto.goalAmount()).thenReturn(new BigDecimal("3000.00"));
            when(sharedPiggyBankRepository.existsByNameIgnoreCase(USER_ID, "Updated Name")).thenReturn(false);
            when(sharedPiggyBankRepository.save(existingPiggyBank)).thenReturn(existingPiggyBank);
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);

            sharedPiggyBankManagementService.editPiggyBank(USER_ID, sharedPiggyBankDto, PIGGY_BANK_ID);

            assertThat(existingPiggyBank.getName()).isEqualTo("Updated Name");
            assertThat(existingPiggyBank.getGoalAmount()).isEqualByComparingTo(new BigDecimal("3000.00"));
        }

        @Test
        void shouldPublishOutboxEventWhenPiggyBankEdited() {
            SharedPiggyBank existingPiggyBank = SharedPiggyBank.builder().id(PIGGY_BANK_ID).name(PIGGY_BANK_NAME).build();
            when(sharedPiggyBankManager.getPiggyBankByUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(existingPiggyBank);
            when(sharedPiggyBankDto.name()).thenReturn(PIGGY_BANK_NAME);
            when(sharedPiggyBankDto.goalAmount()).thenReturn(new BigDecimal("2000.00"));
            when(sharedPiggyBankRepository.existsByNameIgnoreCase(USER_ID, PIGGY_BANK_NAME)).thenReturn(false);
            when(sharedPiggyBankRepository.save(existingPiggyBank)).thenReturn(existingPiggyBank);
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);

            sharedPiggyBankManagementService.editPiggyBank(USER_ID, sharedPiggyBankDto, PIGGY_BANK_ID);

            verify(outboxService).save(eq("SharedAccountPiggyBank"), eq(PIGGY_BANK_ID.toString()), eq("shared-account.activity"), any(SharedAccountActivityLogEvent.class));
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankNotFoundForEdit() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(PIGGY_BANK_ID, USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("Piggy bank not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> sharedPiggyBankManagementService.editPiggyBank(USER_ID, sharedPiggyBankDto, PIGGY_BANK_ID));

            verify(sharedPiggyBankRepository, never()).save(any(SharedPiggyBank.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenRepositorySaveFailsDuringEdit() {
            SharedPiggyBank existingPiggyBank = SharedPiggyBank.builder().id(PIGGY_BANK_ID).name(PIGGY_BANK_NAME).build();
            when(sharedPiggyBankManager.getPiggyBankByUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(existingPiggyBank);
            when(sharedPiggyBankDto.name()).thenReturn(PIGGY_BANK_NAME);
            when(sharedPiggyBankDto.goalAmount()).thenReturn(new BigDecimal("2000.00"));
            when(sharedPiggyBankRepository.existsByNameIgnoreCase(USER_ID, PIGGY_BANK_NAME)).thenReturn(false);
            when(sharedPiggyBankRepository.save(existingPiggyBank)).thenThrow(new IllegalStateException("save failed"));

            assertThrows(IllegalStateException.class, () -> sharedPiggyBankManagementService.editPiggyBank(USER_ID, sharedPiggyBankDto, PIGGY_BANK_ID));

            verifyNoInteractions(outboxService);
        }
    }

    @Nested
    class GetAllPiggyBanks {

        @Test
        void shouldReturnMappedPiggyBanksWhenUserHasPiggyBanks() {
            SharedPiggyBank bank = SharedPiggyBank.builder()
                    .id(PIGGY_BANK_ID)
                    .name(PIGGY_BANK_NAME)
                    .amount(new BigDecimal("500.00"))
                    .goalAmount(new BigDecimal("1000.00"))
                    .build();
            SharedPiggyBankDto mappedDto = mock(SharedPiggyBankDto.class);
            when(sharedPiggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of(bank));
            when(sharedPiggyBankMapper.mapToPiggyBankDto(eq(bank), any())).thenReturn(mappedDto);

            List<SharedPiggyBankDto> result = sharedPiggyBankManagementService.getAllPiggyBanks(USER_ID);

            assertThat(result).containsExactly(mappedDto);
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoPiggyBanks() {
            when(sharedPiggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

            List<SharedPiggyBankDto> result = sharedPiggyBankManagementService.getAllPiggyBanks(USER_ID);

            assertThat(result).isEmpty();
            verifyNoInteractions(sharedPiggyBankMapper);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(sharedPiggyBankRepository.findAllByUserId(USER_ID)).thenThrow(new IllegalStateException("query failed"));

            assertThrows(IllegalStateException.class, () -> sharedPiggyBankManagementService.getAllPiggyBanks(USER_ID));
        }

        @Test
        void shouldThrowExceptionWhenMapperFails() {
            SharedPiggyBank bank = SharedPiggyBank.builder()
                    .id(PIGGY_BANK_ID)
                    .amount(new BigDecimal("500.00"))
                    .goalAmount(new BigDecimal("1000.00"))
                    .build();
            when(sharedPiggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of(bank));
            when(sharedPiggyBankMapper.mapToPiggyBankDto(eq(bank), any())).thenThrow(new IllegalStateException("mapping failed"));

            assertThrows(IllegalStateException.class, () -> sharedPiggyBankManagementService.getAllPiggyBanks(USER_ID));
        }
    }

    @Nested
    class DeletePiggyBank {

        @Test
        void shouldDeletePiggyBankWhenBalanceIsZero() {
            SharedPiggyBank bank = SharedPiggyBank.builder().id(PIGGY_BANK_ID).amount(BigDecimal.ZERO).build();
            when(sharedPiggyBankManager.getPiggyBankByUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(bank);
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);

            sharedPiggyBankManagementService.deletePiggyBank(USER_ID, PIGGY_BANK_ID);

            verify(sharedPiggyBankRepository).delete(bank);
        }

        @Test
        void shouldThrowExceptionWhenBalanceIsPositive() {
            SharedPiggyBank bank = SharedPiggyBank.builder().id(PIGGY_BANK_ID).amount(new BigDecimal("50.00")).build();
            when(sharedPiggyBankManager.getPiggyBankByUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(bank);

            assertThrows(InvalidInputException.class, () -> sharedPiggyBankManagementService.deletePiggyBank(USER_ID, PIGGY_BANK_ID));

            verify(sharedPiggyBankRepository, never()).delete(any(SharedPiggyBank.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldPublishOutboxEventWhenPiggyBankDeleted() {
            SharedPiggyBank bank = SharedPiggyBank.builder().id(PIGGY_BANK_ID).amount(BigDecimal.ZERO).build();
            when(sharedPiggyBankManager.getPiggyBankByUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(bank);
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);

            sharedPiggyBankManagementService.deletePiggyBank(USER_ID, PIGGY_BANK_ID);

            verify(outboxService).save(eq("SharedAccountPiggyBank"), eq(PIGGY_BANK_ID.toString()), eq("shared-account.activity"), any(SharedAccountActivityLogEvent.class));
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankNotFoundForDeletion() {
            when(sharedPiggyBankManager.getPiggyBankByUserId(PIGGY_BANK_ID, USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("Piggy bank not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> sharedPiggyBankManagementService.deletePiggyBank(USER_ID, PIGGY_BANK_ID));

            verify(sharedPiggyBankRepository, never()).delete(any(SharedPiggyBank.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryDeleteFails() {
            SharedPiggyBank bank = SharedPiggyBank.builder().id(PIGGY_BANK_ID).amount(BigDecimal.ZERO).build();
            when(sharedPiggyBankManager.getPiggyBankByUserId(PIGGY_BANK_ID, USER_ID)).thenReturn(bank);
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);
            doThrow(new IllegalStateException("delete failed")).when(sharedPiggyBankRepository).delete(bank);

            assertThrows(IllegalStateException.class, () -> sharedPiggyBankManagementService.deletePiggyBank(USER_ID, PIGGY_BANK_ID));
        }
    }
}