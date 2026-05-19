package com.finovara.finovarabackend.piggybank.service;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.exception.conflict.NameAlreadyExistsException;
import com.finovara.finovarabackend.piggybank.dto.PiggyBankDto;
import com.finovara.finovarabackend.piggybank.mapper.PiggyBankMapper;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.model.PiggyBankGoalType;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.factory.SettingsFactory;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.finovarabackend.usersetting.finances.recurring.repository.RecurringSettingsRepository;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.usersetting.piggybank.repository.PiggyBankSettingsRepository;
import com.finovara.finovarabackend.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    private UserManagerService userManagerService;
    @Mock
    private PiggyBankRepository piggyBankRepository;
    @Mock
    private PiggyBankManagerService piggyBankManagerService;
    @Mock
    private PiggyBankActivityService piggyBankActivityService;
    @Mock
    private PiggyBankSettingsRepository piggyBankSettingsRepository;
    @Mock
    private SettingsFactory settingsFactory;
    @Mock
    private PiggyBankMapper piggyBankMapper;
    @Mock
    private RecurringSettingsRepository recurringSettingsRepository;

    private User user;
    private Long userId;
    private Long piggyBankId;
    private PiggyBankDto defaultDto;

    @BeforeEach
    void setUp() {
        userId = 1L;
        piggyBankId = 1L;

        user = new User();
        user.setId(userId);

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
            saved.setUserAssigned(user);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(piggyBankRepository.countPiggyBanksByUserId(userId)).thenReturn(0L);
            when(piggyBankRepository.existsByNameIgnoreCase(userId, defaultDto.name())).thenReturn(false);
            when(piggyBankRepository.save(any())).thenReturn(saved);
            when(settingsFactory.createDefaultPiggyBankSettings(any())).thenReturn(new PiggyBankSettings());

            Long result = piggyBankManagementService.addPiggyBank(defaultDto, userId);

            assertEquals(piggyBankId, result);

            verify(piggyBankRepository).save(any());
            verify(piggyBankActivityService)
                    .createSimplePiggyBankActivity(eq(userId), any(), eq(PiggyBankActivityType.ADDED_PIGGY_BANK));
            verify(piggyBankSettingsRepository).save(any());
        }

        @Test
        void shouldThrowExceptionWhenMaxReached() {

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(piggyBankRepository.countPiggyBanksByUserId(userId)).thenReturn(5L);

            assertThrows(InvalidInputException.class, () -> piggyBankManagementService.addPiggyBank(defaultDto, userId));

            verify(piggyBankRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenNameExists() {

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(piggyBankRepository.countPiggyBanksByUserId(userId)).thenReturn(0L);
            when(piggyBankRepository.existsByNameIgnoreCase(eq(userId), any())).thenReturn(true);

            assertThrows(NameAlreadyExistsException.class, () -> piggyBankManagementService.addPiggyBank(defaultDto, userId));
        }

        @Test
        void shouldThrowExceptionWhenUserNotFound() {

            when(userManagerService.getUserByIdOrThrow(userId)).thenThrow(new UserNotFoundException("x"));

            assertThrows(UserNotFoundException.class, () -> piggyBankManagementService.addPiggyBank(defaultDto, userId));
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

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            when(piggyBankRepository.existsByNameIgnoreCase(userId, defaultDto.name())).thenReturn(false);
            when(piggyBankRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Long result = piggyBankManagementService.editPiggyBank(userId, defaultDto, piggyBankId);

            assertEquals(piggyBankId, result);
            assertEquals("Piggy", defaultDto.name());

            verify(piggyBankActivityService)
                    .createEditPiggyBankActivity(eq(userId), eq(piggyBank),
                            eq(PiggyBankActivityType.EDITED_PIGGY_BANK),
                            any(), any(), eq("Old"));
        }

        @Test
        void shouldThrowExceptionWhenNameExists() {

            PiggyBank piggyBank = new PiggyBank();
            piggyBank.setName("Old");

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            when(piggyBankRepository.existsByNameIgnoreCase(userId, defaultDto.name())).thenReturn(true);

            assertThrows(NameAlreadyExistsException.class, () -> piggyBankManagementService.editPiggyBank(userId, defaultDto, piggyBankId));
        }
    }

    @Nested
    class GetAllTests {

        @Test
        void shouldReturnList() {

            PiggyBank piggyBank = new PiggyBank();

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(piggyBankRepository.findAllByUserAssignedId(userId)).thenReturn(List.of(piggyBank));
            when(piggyBankMapper.mapToPiggyBankDto(any(), any(), anyDouble(), anyBoolean()))
                    .thenReturn(defaultDto);

            List<PiggyBankDto> result = piggyBankManagementService.getAllPiggyBanks(userId);

            assertEquals(1, result.size());
        }

        @Test
        void shouldReturnEmpty() {

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(piggyBankRepository.findAllByUserAssignedId(userId)).thenReturn(List.of());

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
            when(recurringSettingsRepository.findByUserAssignedIdAndPiggyBankId(userId, piggyBankId)).thenReturn(Optional.empty());

            piggyBankManagementService.deletePiggyBank(userId, piggyBankId);

            verify(piggyBankRepository).delete(piggyBank);
            verify(piggyBankActivityService).createSimplePiggyBankActivity(userId, piggyBank, PiggyBankActivityType.DELETED_PIGGY_BANK);
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
            when(recurringSettingsRepository.findByUserAssignedIdAndPiggyBankId(userId, piggyBankId)).thenReturn(Optional.of(settings));

            piggyBankManagementService.deletePiggyBank(userId, piggyBankId);

            assertFalse(settings.isEnable());
            assertNull(settings.getNextExecutionDate());
            assertNull(settings.getPiggyBankId());
        }
    }
}