package com.finovara.financeservice.sharedaccount.piggybank.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.model.transaction.PiggyBankGoalType;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedPiggyBankManagementServiceTest {

    @Mock
    private SharedPiggyBankRepository sharedPiggyBankRepository;

    @Mock
    private SharedPiggyBankManager sharedPiggyBankManager;

    @Mock
    private SharedPiggyBankMapper sharedPiggyBankMapper;

    @Mock
    private SharedAccountParticipantsService sharedAccountParticipantsService;

    @InjectMocks
    private SharedPiggyBankManagementService sharedPiggyBankManagementService;

    private Long userId;
    private Long piggyBankId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        piggyBankId = 4L;
    }

    @Nested
    class AddPiggyBank {

        private SharedPiggyBankDto dto;
        private SharedAccountParticipantsResponse participants;

        @BeforeEach
        void setUp() {
            dto = mock(SharedPiggyBankDto.class);
            participants = mock(SharedAccountParticipantsResponse.class);
        }

        @Test
        void shouldReturnGeneratedIdWhenPiggyBankIsCreated() {
            when(dto.name()).thenReturn("Car");
            when(dto.goalAmount()).thenReturn(BigDecimal.valueOf(500));
            when(dto.goalType()).thenReturn(PiggyBankGoalType.VACATION);
            when(sharedAccountParticipantsService.getParticipants(userId)).thenReturn(participants);
            when(participants.ownerId()).thenReturn(10L);
            when(participants.memberId()).thenReturn(20L);
            when(sharedPiggyBankRepository.countPiggyBanksByUserId(userId)).thenReturn(0L);
            when(sharedPiggyBankRepository.existsByNameIgnoreCase(userId, "Car")).thenReturn(false);
            when(sharedPiggyBankRepository.save(any(SharedPiggyBank.class)))
                    .thenReturn(SharedPiggyBank.builder().id(piggyBankId).name("Car").build());

            Long result = sharedPiggyBankManagementService.addPiggyBank(dto, userId);

            assertEquals(piggyBankId, result);
        }

        @Test
        void shouldSavePiggyBankWithZeroAmountWhenCreated() {
            when(dto.name()).thenReturn("Car");
            when(dto.goalAmount()).thenReturn(BigDecimal.valueOf(500));
            when(dto.goalType()).thenReturn(PiggyBankGoalType.VACATION);
            when(sharedAccountParticipantsService.getParticipants(userId)).thenReturn(participants);
            when(participants.ownerId()).thenReturn(10L);
            when(participants.memberId()).thenReturn(20L);
            when(sharedPiggyBankRepository.countPiggyBanksByUserId(userId)).thenReturn(0L);
            when(sharedPiggyBankRepository.existsByNameIgnoreCase(userId, "Car")).thenReturn(false);
            when(sharedPiggyBankRepository.save(any(SharedPiggyBank.class)))
                    .thenReturn(SharedPiggyBank.builder().id(piggyBankId).build());

            sharedPiggyBankManagementService.addPiggyBank(dto, userId);

            verify(sharedPiggyBankRepository).save(argThat(bank ->
                    bank.getAmount().compareTo(BigDecimal.ZERO) == 0
                            && bank.getName().equals("Car")
                            && bank.getGoalAmount().equals(BigDecimal.valueOf(500))
                            && bank.getGoalType() == PiggyBankGoalType.VACATION
                            && bank.getOwnerId().equals(10L)
                            && bank.getMemberId().equals(20L)
            ));
        }

        @Test
        void shouldThrowExceptionWhenMaxPiggyBanksReached() {
            when(sharedAccountParticipantsService.getParticipants(userId)).thenReturn(participants);
            when(sharedPiggyBankRepository.countPiggyBanksByUserId(userId)).thenReturn(5L);

            assertThrows(InvalidInputException.class,
                    () -> sharedPiggyBankManagementService.addPiggyBank(dto, userId));
        }

        @Test
        void shouldNotSavePiggyBankWhenMaxPiggyBanksReached() {
            when(sharedAccountParticipantsService.getParticipants(userId)).thenReturn(participants);
            when(sharedPiggyBankRepository.countPiggyBanksByUserId(userId)).thenReturn(5L);

            assertThrows(InvalidInputException.class,
                    () -> sharedPiggyBankManagementService.addPiggyBank(dto, userId));

            verify(sharedPiggyBankRepository, never()).save(any(SharedPiggyBank.class));
        }

        @Test
        void shouldThrowExceptionWhenNameAlreadyExists() {
            when(dto.name()).thenReturn("Car");
            when(sharedAccountParticipantsService.getParticipants(userId)).thenReturn(participants);
            when(sharedPiggyBankRepository.countPiggyBanksByUserId(userId)).thenReturn(2L);
            when(sharedPiggyBankRepository.existsByNameIgnoreCase(userId, "Car")).thenReturn(true);

            assertThrows(EntityAlreadyExistsException.class,
                    () -> sharedPiggyBankManagementService.addPiggyBank(dto, userId));
        }

        @Test
        void shouldNotSavePiggyBankWhenNameAlreadyExists() {
            when(dto.name()).thenReturn("Car");
            when(sharedAccountParticipantsService.getParticipants(userId)).thenReturn(participants);
            when(sharedPiggyBankRepository.countPiggyBanksByUserId(userId)).thenReturn(2L);
            when(sharedPiggyBankRepository.existsByNameIgnoreCase(userId, "Car")).thenReturn(true);

            assertThrows(EntityAlreadyExistsException.class,
                    () -> sharedPiggyBankManagementService.addPiggyBank(dto, userId));

            verify(sharedPiggyBankRepository, never()).save(any(SharedPiggyBank.class));
        }

        @Test
        void shouldNotSavePiggyBankAndThrowExceptionWhenGoalAmountIsNegative() {
            when(dto.goalAmount()).thenReturn(BigDecimal.valueOf(-100));

            assertThrows(InvalidInputException.class,
                    () -> sharedPiggyBankManagementService.addPiggyBank(dto, userId));

            verify(sharedPiggyBankRepository, never()).save(any(SharedPiggyBank.class));
        }

        @Test
        void shouldCreatePiggyBankWhenGoalAmountIsNull() {
            when(dto.name()).thenReturn("Bez celu");
            when(dto.goalAmount()).thenReturn(null);
            when(dto.goalType()).thenReturn(PiggyBankGoalType.OTHER);
            when(sharedAccountParticipantsService.getParticipants(userId)).thenReturn(participants);
            when(participants.ownerId()).thenReturn(10L);
            when(participants.memberId()).thenReturn(20L);
            when(sharedPiggyBankRepository.countPiggyBanksByUserId(userId)).thenReturn(0L);
            when(sharedPiggyBankRepository.existsByNameIgnoreCase(userId, "Bez celu")).thenReturn(false);
            when(sharedPiggyBankRepository.save(any(SharedPiggyBank.class)))
                    .thenReturn(SharedPiggyBank.builder().id(piggyBankId).build());

            Long result = sharedPiggyBankManagementService.addPiggyBank(dto, userId);

            assertEquals(piggyBankId, result);
        }
    }

    @Nested
    class EditPiggyBank {

        private SharedPiggyBankDto dto;
        private SharedPiggyBank existingPiggyBank;

        @BeforeEach
        void setUp() {
            dto = mock(SharedPiggyBankDto.class);
            existingPiggyBank = SharedPiggyBank.builder()
                    .id(piggyBankId)
                    .name("Old  name")
                    .amount(BigDecimal.ZERO)
                    .build();
        }

        @Test
        void shouldReturnPiggyBankIdWhenEditIsValid() {
            when(dto.name()).thenReturn("New name");
            when(dto.goalAmount()).thenReturn(BigDecimal.valueOf(700));
            when(dto.goalType()).thenReturn(PiggyBankGoalType.CAR);
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(existingPiggyBank);
            when(sharedPiggyBankRepository.existsByNameIgnoreCase(userId, "New name")).thenReturn(false);
            when(sharedPiggyBankRepository.save(existingPiggyBank)).thenReturn(existingPiggyBank);

            Long result = sharedPiggyBankManagementService.editPiggyBank(userId, dto, piggyBankId);

            assertEquals(piggyBankId, result);
        }

        @Test
        void shouldUpdatePiggyBankFieldsWhenEditIsValid() {
            when(dto.name()).thenReturn("New name");
            when(dto.goalAmount()).thenReturn(BigDecimal.valueOf(700));
            when(dto.goalType()).thenReturn(PiggyBankGoalType.CAR);
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(existingPiggyBank);
            when(sharedPiggyBankRepository.existsByNameIgnoreCase(userId, "New name")).thenReturn(false);
            when(sharedPiggyBankRepository.save(existingPiggyBank)).thenReturn(existingPiggyBank);

            sharedPiggyBankManagementService.editPiggyBank(userId, dto, piggyBankId);

            assertEquals("New name", existingPiggyBank.getName());
            assertEquals(BigDecimal.valueOf(700), existingPiggyBank.getGoalAmount());
            assertEquals(PiggyBankGoalType.CAR, existingPiggyBank.getGoalType());
        }

        @Test
        void shouldThrowExceptionWhenNewNameAlreadyExists() {
            when(dto.name()).thenReturn("Name taken");
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(existingPiggyBank);
            when(sharedPiggyBankRepository.existsByNameIgnoreCase(userId, "Name taken")).thenReturn(true);

            assertThrows(EntityAlreadyExistsException.class,
                    () -> sharedPiggyBankManagementService.editPiggyBank(userId, dto, piggyBankId));
        }

        @Test
        void shouldNotSaveWhenNewNameAlreadyExists() {
            when(dto.name()).thenReturn("Name taken");
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(existingPiggyBank);
            when(sharedPiggyBankRepository.existsByNameIgnoreCase(userId, "Name taken")).thenReturn(true);

            assertThrows(EntityAlreadyExistsException.class,
                    () -> sharedPiggyBankManagementService.editPiggyBank(userId, dto, piggyBankId));

            verify(sharedPiggyBankRepository, never()).save(any(SharedPiggyBank.class));
        }

        @Test
        void shouldThrowExceptionWhenGoalAmountIsNegativeOnEdit() {
            when(dto.name()).thenReturn("New name");
            when(dto.goalAmount()).thenReturn(BigDecimal.valueOf(-50));
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(existingPiggyBank);
            when(sharedPiggyBankRepository.existsByNameIgnoreCase(userId, "New name")).thenReturn(false);

            assertThrows(InvalidInputException.class,
                    () -> sharedPiggyBankManagementService.editPiggyBank(userId, dto, piggyBankId));
        }

        @Test
        void shouldNotSaveWhenGoalAmountIsNegativeOnEdit() {
            when(dto.name()).thenReturn("New name");
            when(dto.goalAmount()).thenReturn(BigDecimal.valueOf(-50));
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(existingPiggyBank);
            when(sharedPiggyBankRepository.existsByNameIgnoreCase(userId, "New name")).thenReturn(false);

            assertThrows(InvalidInputException.class,
                    () -> sharedPiggyBankManagementService.editPiggyBank(userId, dto, piggyBankId));

            verify(sharedPiggyBankRepository, never()).save(any(SharedPiggyBank.class));
        }
    }

    @Nested
    class GetAllPiggyBanks {

        @Test
        void shouldReturnEmptyListWhenNoPiggyBanksExist() {
            when(sharedPiggyBankRepository.findAllByUserId(userId)).thenReturn(List.of());

            List<SharedPiggyBankDto> result = sharedPiggyBankManagementService.getAllPiggyBanks(userId);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldReturnMappedPiggyBankWhenSinglePiggyBankExists() {
            SharedPiggyBank piggyBank = SharedPiggyBank.builder()
                    .id(piggyBankId)
                    .amount(BigDecimal.valueOf(25))
                    .goalAmount(BigDecimal.valueOf(100))
                    .build();
            SharedPiggyBankDto expectedDto = mock(SharedPiggyBankDto.class);

            when(sharedPiggyBankRepository.findAllByUserId(userId)).thenReturn(List.of(piggyBank));
            when(sharedPiggyBankMapper.mapToPiggyBankDto(eq(piggyBank), any(Double.class))).thenReturn(expectedDto);

            List<SharedPiggyBankDto> result = sharedPiggyBankManagementService.getAllPiggyBanks(userId);

            assertEquals(1, result.size());
            assertEquals(expectedDto, result.get(0));
        }

        @Test
        void shouldCallMapperForEveryPiggyBankWhenMultiplePiggyBanksExist() {
            SharedPiggyBank firstPiggyBank = SharedPiggyBank.builder().id(1L).amount(BigDecimal.ZERO).build();
            SharedPiggyBank secondPiggyBank = SharedPiggyBank.builder().id(2L).amount(BigDecimal.ZERO).build();

            when(sharedPiggyBankRepository.findAllByUserId(userId)).thenReturn(List.of(firstPiggyBank, secondPiggyBank));
            when(sharedPiggyBankMapper.mapToPiggyBankDto(any(SharedPiggyBank.class), any(Double.class)))
                    .thenReturn(mock(SharedPiggyBankDto.class));

            sharedPiggyBankManagementService.getAllPiggyBanks(userId);

            verify(sharedPiggyBankMapper, times(2)).mapToPiggyBankDto(any(SharedPiggyBank.class), any(Double.class));
        }
    }

    @Nested
    class DeletePiggyBank {

        @Test
        void shouldDeletePiggyBankWhenAmountIsZero() {
            SharedPiggyBank piggyBank = SharedPiggyBank.builder().id(piggyBankId).amount(BigDecimal.ZERO).build();
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            sharedPiggyBankManagementService.deletePiggyBank(userId, piggyBankId);

            verify(sharedPiggyBankRepository).delete(piggyBank);
        }

        @Test
        void shouldThrowExceptionWhenAmountIsGreaterThanZero() {
            SharedPiggyBank piggyBank = SharedPiggyBank.builder().id(piggyBankId).amount(BigDecimal.TEN).build();
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            assertThrows(InvalidInputException.class,
                    () -> sharedPiggyBankManagementService.deletePiggyBank(userId, piggyBankId));
        }

        @Test
        void shouldNotDeletePiggyBankWhenAmountIsGreaterThanZero() {
            SharedPiggyBank piggyBank = SharedPiggyBank.builder().id(piggyBankId).amount(BigDecimal.TEN).build();
            when(sharedPiggyBankManager.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            assertThrows(InvalidInputException.class,
                    () -> sharedPiggyBankManagementService.deletePiggyBank(userId, piggyBankId));

            verify(sharedPiggyBankRepository, never()).delete(any(SharedPiggyBank.class));
        }
    }
}