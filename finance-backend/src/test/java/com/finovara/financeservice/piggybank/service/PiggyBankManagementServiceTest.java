package com.finovara.authbackend.piggybank.service;

import com.finovara.contracts.event.activity.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.authbackend.piggybank.dto.PiggyBankDto;
import com.finovara.authbackend.piggybank.mapper.PiggyBankMapper;
import com.finovara.authbackend.piggybank.model.PiggyBank;
import com.finovara.contracts.model.transaction.PiggyBankGoalType;
import com.finovara.authbackend.piggybank.repository.PiggyBankRepository;
import com.finovara.authbackend.settings.factory.SettingsFactory;
import com.finovara.authbackend.settings.finances.recurring.model.RecurringSettings;
import com.finovara.authbackend.settings.finances.recurring.repository.RecurringSettingsRepository;
import com.finovara.authbackend.settings.piggybank.model.PiggyBankSettings;
import com.finovara.authbackend.settings.piggybank.repository.PiggyBankSettingsRepository;
import com.finovara.authbackend.util.piggybank.manager.PiggyBankManagerService;
import org.springframework.kafka.core.KafkaTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PiggyBankManagementServiceTest {

    @InjectMocks
    private PiggyBankManagementService piggyBankManagementService;

    @Mock
    private PiggyBankRepository piggyBankRepository;
    @Mock
    private PiggyBankManagerService piggyBankManagerService;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private PiggyBankSettingsRepository piggyBankSettingsRepository;
    @Mock
    private SettingsFactory settingsFactory;
    @Mock
    private PiggyBankMapper piggyBankMapper;
    @Mock
    private RecurringSettingsRepository recurringSettingsRepository;

    private Long userId;
    private Long piggyBankId;
    private PiggyBankDto defaultDto;

    @BeforeEach
    void setUp() {
        userId = 1L;
        piggyBankId = 1L;

        defaultDto = new PiggyBankDto(
                null, null, "Piggy",
                BigDecimal.valueOf(100), null,
                PiggyBankGoalType.GIFTS,
                BigDecimal.valueOf(230), null, null
        );
    }

    @Nested
    class AddPiggyBankTests {

        @Test
        void shouldAddPiggyBankSuccessfully() {

            PiggyBank saved = new PiggyBank();
            saved.setId(piggyBankId);
            saved.setUserId(userId);

            when(piggyBankRepository.countPiggyBanksByUserId(userId)).thenReturn(0L);
            when(piggyBankRepository.existsByNameIgnoreCase(userId, defaultDto.name())).thenReturn(false);
            when(piggyBankRepository.save(any())).thenReturn(saved);
            when(settingsFactory.createDefaultPiggyBankSettings(any())).thenReturn(new PiggyBankSettings());

            Long result = piggyBankManagementService.addPiggyBank(defaultDto, userId);

            assertEquals(piggyBankId, result);

            verify(piggyBankRepository).save(any());
            ArgumentCaptor<PiggyBankActivityEvent> eventCaptor = ArgumentCaptor.forClass(PiggyBankActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.piggybank"), eventCaptor.capture());
            assertEquals(PiggyBankActivityType.ADDED_PIGGY_BANK, eventCaptor.getValue().type());
            verify(piggyBankSettingsRepository).save(any());
        }

        @Test
        void shouldThrowExceptionWhenMaxReached() {

            when(piggyBankRepository.countPiggyBanksByUserId(userId)).thenReturn(5L);

            assertThrows(InvalidInputException.class, () -> piggyBankManagementService.addPiggyBank(defaultDto, userId));

            verify(piggyBankRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenNameExists() {

            when(piggyBankRepository.countPiggyBanksByUserId(userId)).thenReturn(0L);
            when(piggyBankRepository.existsByNameIgnoreCase(eq(userId), any())).thenReturn(true);

            assertThrows(EntityAlreadyExistsException.class, () -> piggyBankManagementService.addPiggyBank(defaultDto, userId));
        }
    }

    @Nested
    class EditPiggyBankTests {

        @Test
        void shouldEditSuccessfully() {

            PiggyBank piggyBank = new PiggyBank();
            piggyBank.setId(piggyBankId);
            piggyBank.setName("Old");
            piggyBank.setGoalAmount(BigDecimal.TEN);
            piggyBank.setGoalType(PiggyBankGoalType.GIFTS);

            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            when(piggyBankRepository.existsByNameIgnoreCase(userId, defaultDto.name())).thenReturn(false);
            when(piggyBankRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Long result = piggyBankManagementService.editPiggyBank(userId, defaultDto, piggyBankId);

            assertEquals(piggyBankId, result);
            assertEquals("Piggy", defaultDto.name());

            ArgumentCaptor<PiggyBankActivityEvent> eventCaptor = ArgumentCaptor.forClass(PiggyBankActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.piggybank"), eventCaptor.capture());
            assertEquals(PiggyBankActivityType.EDITED_PIGGY_BANK, eventCaptor.getValue().type());
        }

        @Test
        void shouldThrowExceptionWhenNameExists() {

            PiggyBank piggyBank = new PiggyBank();
            piggyBank.setName("Old");

            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            when(piggyBankRepository.existsByNameIgnoreCase(userId, defaultDto.name())).thenReturn(true);

            assertThrows(EntityAlreadyExistsException.class, () -> piggyBankManagementService.editPiggyBank(userId, defaultDto, piggyBankId));
        }
    }

    @Nested
    class GetAllTests {

        @Test
        void shouldReturnList() {

            PiggyBank piggyBank = new PiggyBank();

            when(piggyBankRepository.findAllByUserId(userId)).thenReturn(List.of(piggyBank));
            when(piggyBankMapper.mapToPiggyBankDto(any(), any(), anyBoolean()))
                    .thenReturn(defaultDto);

            List<PiggyBankDto> result = piggyBankManagementService.getAllPiggyBanks(userId);

            assertEquals(1, result.size());
        }

        @Test
        void shouldReturnEmpty() {

            when(piggyBankRepository.findAllByUserId(userId)).thenReturn(List.of());

            List<PiggyBankDto> result = piggyBankManagementService.getAllPiggyBanks(userId);

            assertTrue(result.isEmpty());
            verifyNoInteractions(piggyBankMapper);
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void shouldDelete() {

            PiggyBank piggyBank = new PiggyBank();
            piggyBank.setAmount(BigDecimal.ZERO);

            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            when(recurringSettingsRepository.findByUserIdAndPiggyBankId(userId, piggyBankId)).thenReturn(Optional.empty());

            piggyBankManagementService.deletePiggyBank(userId, piggyBankId);

            verify(piggyBankRepository).delete(piggyBank);
            ArgumentCaptor<PiggyBankActivityEvent> eventCaptor = ArgumentCaptor.forClass(PiggyBankActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.piggybank"), eventCaptor.capture());
            assertEquals(PiggyBankActivityType.DELETED_PIGGY_BANK, eventCaptor.getValue().type());
        }

        @Test
        void shouldThrowExceptionWhenBalanceNotZero() {

            PiggyBank piggyBank = new PiggyBank();
            piggyBank.setAmount(BigDecimal.TEN);

            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            assertThrows(InvalidInputException.class, () -> piggyBankManagementService.deletePiggyBank(userId, piggyBankId));
        }

        @Test
        void shouldDisableRecurring() {

            PiggyBank piggyBank = new PiggyBank();
            piggyBank.setAmount(BigDecimal.ZERO);

            RecurringSettings settings = new RecurringSettings();
            settings.setEnable(true);
            settings.setNextExecutionDate(LocalDate.now());

            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            when(recurringSettingsRepository.findByUserIdAndPiggyBankId(userId, piggyBankId)).thenReturn(Optional.of(settings));

            piggyBankManagementService.deletePiggyBank(userId, piggyBankId);

            assertFalse(settings.isEnable());
            assertNull(settings.getNextExecutionDate());
            assertNull(settings.getPiggyBankId());
        }
    }
}
